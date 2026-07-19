// Vercel serverless function: POST /api/activate
// Verifies an HMAC-signed StreamNav activation key, enforces an optional per-key
// device limit, and supports a server-side revocation list.
//
// Endpoints:
//   POST /api/activate            -> validate + (optionally) register a device
//     Body: { "key": "STN...", "deviceId": "optional-unique-device-id" }
//     Response: { "valid": true } | { "valid": false, "reason": "..." }
//
//   POST /api/activate?action=revoke   -> revoke a key (admin only)
//     Header: x-revoke-token: <REVOKE_TOKEN>
//     Body: { "key": "STN..." }
//     Response: { "revoked": true }
//
// Environment variables (set in Vercel):
//   LICENSE_SECRET        - HMAC secret. MUST match mint_activation_keys.ps1.
//   MAX_DEVICES_PER_KEY   - optional, default 3.
//   REVOKE_TOKEN          - optional, required to call the revoke action.
//   KV_REST_API_URL       - optional, Vercel KV REST URL (enables persistence).
//   KV_REST_API_TOKEN     - optional, Vercel KV REST token.
// If KV vars are absent, an in-memory store is used (per instance; not durable).

const ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const PREFIX = "STN";
const BODY_LEN = 17;
const SIGN_LEN = 4;
const TOTAL_LEN = PREFIX.length + BODY_LEN + SIGN_LEN;
const BASE = ALPHABET.length;
const MAX_DEVICES_PER_KEY = Number(process.env.MAX_DEVICES_PER_KEY || 3);

let memoryStore = { devices: {}, revoked: {} };

function base36(n, len) {
  let s = "";
  let v = n;
  for (let i = 0; i < len; i++) { s = ALPHABET[v % BASE] + s; v = Math.floor(v / BASE); }
  return s.padStart(len, ALPHABET[0]);
}

function hmacSign(core, secret) {
  const crypto = require("crypto");
  const raw = crypto.createHmac("sha256", secret).update(core).digest();
  let value = 0;
  for (let i = 0; i < SIGN_LEN; i++) value = (value * 256 + raw[i]) >>> 0;
  const mod = value % Math.pow(BASE, SIGN_LEN);
  return base36(mod, SIGN_LEN);
}

function verifyKey(key, secret) {
  const k = (key || "").trim().toUpperCase();
  if (k.length !== TOTAL_LEN) return false;
  if (![...k].every(c => ALPHABET.includes(c))) return false;
  if (!k.startsWith(PREFIX)) return false;
  const core = k.slice(0, TOTAL_LEN - SIGN_LEN);
  return k.slice(TOTAL_LEN - SIGN_LEN) === hmacSign(core, secret);
}

function kvEnabled() {
  return Boolean(process.env.KV_REST_API_URL && process.env.KV_REST_API_TOKEN);
}

async function kv() {
  const { createClient } = await import("@vercel/kv");
  return createClient({ url: process.env.KV_REST_API_URL, token: process.env.KV_REST_API_TOKEN });
}

async function registeredDevices(key) {
  if (kvEnabled()) return (await kv()).get("act:" + key) || [];
  return memoryStore.devices[key] || [];
}

async function registerDevice(key, deviceId) {
  if (kvEnabled()) {
    const client = await kv();
    const list = (await client.get("act:" + key)) || [];
    if (!list.includes(deviceId)) list.push(deviceId);
    await client.set("act:" + key, list);
    return list;
  }
  const list = memoryStore.devices[key] || [];
  if (deviceId && !list.includes(deviceId)) list.push(deviceId);
  memoryStore.devices[key] = list;
  return list;
}

async function isRevoked(key) {
  if (kvEnabled()) return Boolean(await (await kv()).get("rev:" + key));
  return Boolean(memoryStore.revoked[key]);
}

async function revokeKey(key) {
  if (kvEnabled()) { await (await kv()).set("rev:" + key, true); return; }
  memoryStore.revoked[key] = true;
}

async function handleActivate(req, res, secret) {
  let body;
  try { body = JSON.parse(req.body || "{}"); } catch { return res.status(400).json({ valid: false, reason: "bad-json" }); }
  const key = body.key;
  const deviceId = body.deviceId;

  if (!verifyKey(key, secret)) return res.status(200).json({ valid: false, reason: "invalid-key" });
  if (await isRevoked(key)) return res.status(200).json({ valid: false, reason: "revoked" });

  if (deviceId) {
    let devices = await registeredDevices(key);
    if (devices.includes(deviceId)) return res.status(200).json({ valid: true });
    if (devices.length >= MAX_DEVICES_PER_KEY) return res.status(200).json({ valid: false, reason: "device-limit" });
    devices = await registerDevice(key, deviceId);
    return res.status(200).json({ valid: true, devices: devices.length, maxDevices: MAX_DEVICES_PER_KEY });
  }

  return res.status(200).json({ valid: true });
}

async function handleRevoke(req, res, secret) {
  const token = req.headers["x-revoke-token"];
  if (!process.env.REVOKE_TOKEN || token !== process.env.REVOKE_TOKEN) {
    return res.status(401).json({ revoked: false, reason: "unauthorized" });
  }
  let body;
  try { body = JSON.parse(req.body || "{}"); } catch { return res.status(400).json({ revoked: false, reason: "bad-json" }); }
  const key = body.key;
  if (!verifyKey(key, secret)) return res.status(200).json({ revoked: false, reason: "invalid-key" });
  await revokeKey(key);
  return res.status(200).json({ revoked: true });
}

module.exports = async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ valid: false, reason: "method" });
  const secret = process.env.LICENSE_SECRET;
  if (!secret) return res.status(500).json({ valid: false, reason: "server-misconfig" });

  const action = (req.query && req.query.action) || "";
  if (action === "revoke") return handleRevoke(req, res, secret);
  return handleActivate(req, res, secret);
};
