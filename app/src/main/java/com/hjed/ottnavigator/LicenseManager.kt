package com.hjed.ottnavigator

import android.content.Context
import androidx.core.content.edit
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages the 15-day free trial and full activation via an activation key.
 *
 * Validation strategy:
 * - First activation always requires approval from the activation server.
 * - A successful activation is stored locally so the same device stays unlocked offline.
 * - The production signing secret is never embedded in the APK.
 */
object LicenseManager {

    private const val PREFS_NAME = "streamnav_license"
    private const val KEY_TRIAL_START = "trial_start_epoch"
    private const val KEY_ACTIVATION = "activation_key"
    private const val KEY_ACTIVATED = "activated"

    private const val ACTIVATION_ENDPOINT = "https://streamnav.huetechonline.com/api/activate"

    const val TRIAL_DAYS = 0L
    private val TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(TRIAL_DAYS)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Records the trial start time on first launch only. */
    fun ensureTrialStarted(context: Context) {
        val p = prefs(context)
        if (!p.contains(KEY_TRIAL_START)) {
            p.edit { putLong(KEY_TRIAL_START, System.currentTimeMillis()) }
        }
    }

    fun getTrialStartEpoch(context: Context): Long =
        prefs(context).getLong(KEY_TRIAL_START, 0L)

    /** Milliseconds remaining in the trial, clamped at 0. */
    fun getTrialRemainingMs(context: Context): Long {
        val start = getTrialStartEpoch(context)
        if (start == 0L) return 0L
        return (start + TRIAL_DURATION_MS - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isTrialActive(context: Context): Boolean =
        getTrialRemainingMs(context) > 0L

    fun isActivated(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ACTIVATED, false)
    }

    /** True when the trial has ended and no valid activation key is stored. */
    fun isActivationRequired(context: Context): Boolean =
        !isTrialActive(context) && !isActivated(context)

    /** Checks presentation only. Authenticity is checked exclusively by the server. */
    fun isKeyFormatValid(key: String): Boolean {
        val normalized = normalizeKey(key)
        return normalized.length == 24 &&
            normalized.startsWith("STN") &&
            normalized.all { it.isDigit() || it in 'A'..'Z' }
    }

    fun normalizeKey(key: String): String =
        key.replace("-", "").trim().uppercase()

    data class ActivationResult(
        val success: Boolean,
        val reason: String? = null
    )

    /**
     * Server-side validation. Posts the key (and a stable device id) to the
     * activation endpoint and returns the server's specific result.
     */
    suspend fun validateKeyWithServer(key: String, deviceId: String? = null): ActivationResult {
        val normalized = normalizeKey(key)
        if (!isKeyFormatValid(normalized)) {
            return ActivationResult(false, "invalid-format")
        }

        val payload = JSONObject().apply {
            put("key", normalized)
            if (!deviceId.isNullOrBlank()) put("deviceId", deviceId)
        }.toString()

        return try {
            val connection = (URL(ACTIVATION_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val status = connection.responseCode
            val response = try {
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } finally {
                connection.disconnect()
            }
            val json = if (response.isNotBlank()) JSONObject(response) else JSONObject()
            ActivationResult(
                success = status in 200..299 && json.optBoolean("valid", false),
                reason = json.optString("reason").takeIf { it.isNotBlank() }
                    ?: if (status !in 200..299) "server-error" else null
            )
        } catch (_: Exception) {
            ActivationResult(false, "network-error")
        }
    }

    /** Stores the activation key after successful validation. */
    fun activate(context: Context, key: String) {
        prefs(context).edit {
            putString(KEY_ACTIVATION, normalizeKey(key))
            putBoolean(KEY_ACTIVATED, true)
        }
    }
}
