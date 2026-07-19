# StreamNav Licensing

Free 15-day trial, then a mandatory activation key. This document covers how the
licensing works, how to generate/distribute keys, how to run the activation server,
and how to tune the trial length.

## How it works

- On first launch the app records a trial start time (`LicenseManager.ensureTrialStarted`).
- For 15 days the app is fully usable with **no notifications**. When the trial ends and
  no valid key is stored, a **mandatory, non-dismissable** `ActivationDialog` appears over
  a dimmed screen. The user cannot use the app until a valid key is entered.
- A valid key is HMAC-SHA256 signed with a shared secret, so it cannot be forged without
  the secret. The app verifies the format locally and confirms with the server.

## Key format

`STN` + 17 random alphanumerics + 4-char base36 HMAC signature = 24 characters.
Example: `STNWDHYKTNQH0SBCA63FQISR`

Defined in `app/src/main/java/com/hjed/ottnavigator/ActivationKeyGenerator.kt`
and verified in `app/src/main/java/com/hjed/ottnavigator/LicenseManager.kt`.

## Generating activation keys (vendor side)

Keys are minted offline with `mint_activation_keys.ps1`. Keep this script and the
secret private — never ship it inside the app.

```powershell
$env:LICENSE_SECRET = "your-strong-secret"
pwsh -File mint_activation_keys.ps1 -Count 5 -Secret $env:LICENSE_SECRET
```

Give one key per paying customer. The key is signed with `LICENSE_SECRET`, so only keys
you mint are accepted by the server.

> The app's `BuildConfig.LICENSE_SECRET` is left empty on purpose. The app only checks
> key *format*; the authoritative check is server-side, so the real secret stays off-device.

## Activation server (`api/activate.js`)

Deploy this Vercel serverless function alongside your existing `update.json` endpoint
(same project/domain, e.g. `kinsfolktv.vercel.app`).

### Endpoints

- `POST /api/activate`
  Body: `{ "key": "STN...", "deviceId": "optional-android-id" }`
  Response: `{ "valid": true }` or `{ "valid": false, "reason": "..." }`
  Reasons: `invalid-key`, `revoked`, `device-limit`, `bad-json`, `server-misconfig`.
- `POST /api/activate?action=revoke` (admin)
  Header: `x-revoke-token: <REVOKE_TOKEN>`
  Body: `{ "key": "STN..." }`
  Response: `{ "revoked": true }`

### Environment variables (Vercel)

| Variable               | Required | Default | Purpose                                   |
|------------------------|----------|---------|-------------------------------------------|
| `LICENSE_SECRET`       | yes      | —       | HMAC secret; must match the mint script.  |
| `MAX_DEVICES_PER_KEY`  | no       | `3`     | Max activated devices per key.            |
| `REVOKE_TOKEN`         | no*      | —       | Required to call the `revoke` action.     |
| `KV_REST_API_URL`      | no       | —       | Vercel KV REST URL (enables persistence). |
| `KV_REST_API_TOKEN`    | no       | —       | Vercel KV REST token.                     |

\* Set `REVOKE_TOKEN` if you want to revoke keys at runtime.

### Persistence (Vercel KV)

Without KV, the server uses an in-memory store that is **not durable** across instances.
For production, add Vercel KV and set `KV_REST_API_URL` / `KV_REST_API_TOKEN`. The server
then persists:

- `act:<KEY>` → list of activated device ids (enforces the device limit)
- `rev:<KEY>` → `true` when a key has been revoked

### Revoking a key

```powershell
$body = @{ key = "STNWDHYKTNQH0SBCA63FQISR" } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri "https://kinsfolktv.vercel.app/api/activate?action=revoke" `
  -Headers @{ "x-revoke-token" = $env:REVOKE_TOKEN } `
  -ContentType "application/json" `
  -Body $body
```

Revoked keys fail validation immediately on the next activation check.

## Changing the trial duration

The trial length is a single constant in
`app/src/main/java/com/hjed/ottnavigator/LicenseManager.kt`:

```kotlin
const val TRIAL_DAYS = 15L   // change this number (days)
```

Change `15` to any number of days (e.g. `30` for a 30-day trial), then bump the app
version in `app/build.gradle.kts` (`versionCode` + `versionName`) and rebuild. No other
code changes are needed — the duration flows through `TRIAL_DURATION_MS` automatically.

> Note: changing `TRIAL_DAYS` affects new installs (and users who have not yet activated).
> A user already past their original trial window stays in the activation-required state.

## Files

- `app/src/main/java/com/hjed/ottnavigator/LicenseManager.kt` — trial + activation state, server validation.
- `app/src/main/java/com/hjed/ottnavigator/ActivationKeyGenerator.kt` — HMAC key generator/verifier (in-app format check).
- `app/src/main/java/com/hjed/ottnavigator/ActivationDialog.kt` — mandatory activation UI (dimmed, non-dismissable).
- `mint_activation_keys.ps1` — vendor key minting tool (private).
- `api/activate.js` — Vercel activation + revocation server.
