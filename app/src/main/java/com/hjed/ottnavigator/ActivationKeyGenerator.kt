package com.hjed.ottnavigator

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Activation key generator + verifier (HMAC-signed).
 *
 * Scheme:
 *   - 24 characters total, uppercase alphanumeric.
 *   - Starts with "STN".
 *   - First 20 chars ("core") = "STN" + 17 random alphanumerics.
 *   - Last 4 chars = base36 of the first 4 bytes of HMAC-SHA256(secret, core).
 *
 * The secret is shared with the activation server. Because the signature is keyed,
 * keys cannot be forged without the secret, so validateKeyWithServer() can trust it.
 *
 * WARNING: do NOT embed the real production secret in the published app. The app
 * only needs to VERIFY format; the authoritative check is server-side. The secret
 * below is a placeholder for local/offline key generation tooling only.
 */

object ActivationKeyGenerator {

    private val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val PREFIX = "STN"
    private const val BODY_LEN = 17
    private const val SIGN_LEN = 4
    private const val TOTAL_LEN = PREFIX.length + BODY_LEN + SIGN_LEN // 24

    private val BASE = ALPHABET.length // 36

    // Placeholder secret for offline tooling. Override via BuildConfig / env in your mint script.
    private var secret: String = BuildConfig.LICENSE_SECRET.takeIf { it.isNotBlank() }
        ?: "dev-only-insecure-secret-change-me"

    /** Sets the signing secret (used by offline key-minting tooling). */
    fun setSecret(value: String) {
        secret = value
    }

    fun generate(): String {
        val body = (1..BODY_LEN)
            .map { ALPHABET[Random.nextInt(ALPHABET.length)] }
            .joinToString("")
        val core = PREFIX + body
        return core + sign(core)
    }

    fun generate(count: Int): List<String> {
        val seen = LinkedHashSet<String>()
        while (seen.size < count) seen.add(generate())
        return seen.toList()
    }

    /** Verifies format + HMAC signature using the currently configured secret. */
    fun verify(key: String): Boolean {
        val k = key.trim().uppercase()
        if (k.length != TOTAL_LEN) return false
        if (!k.all { it in ALPHABET }) return false
        if (!k.startsWith(PREFIX)) return false
        val core = k.take(TOTAL_LEN - SIGN_LEN)
        return k.takeLast(SIGN_LEN) == sign(core)
    }

    private fun sign(core: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal(core.toByteArray(Charsets.UTF_8))
        var value = 0L
        for (i in 0 until SIGN_LEN) {
            value = (value * 256 + (raw[i].toLong() and 0xFF)) and 0xFFFFFFFFL
        }
        val mod = value % BASE.toLong().pow(SIGN_LEN)
        val sb = StringBuilder()
        var m = mod
        repeat(SIGN_LEN) {
            sb.insert(0, ALPHABET[(m % BASE).toInt()])
            m /= BASE
        }
        return sb.toString().padStart(SIGN_LEN, ALPHABET[0])
    }

    private fun Long.pow(n: Int): Long {
        var r = 1L
        repeat(n) { r *= this }
        return r
    }
}
