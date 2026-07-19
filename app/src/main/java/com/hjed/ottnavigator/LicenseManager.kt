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
 * - A successful activation is stored locally so the app stays unlocked even offline.
 * - [validateKeyWithServer] is the seam where real server-side validation should be
 *   plugged in (mirrors UpdateManager.getLatestVersion). Until a backend is wired,
 *   [isKeyFormatValid] provides a safe local-format check so the flow works standalone.
 */
object LicenseManager {

    private const val PREFS_NAME = "streamnav_license"
    private const val KEY_TRIAL_START = "trial_start_epoch"
    private const val KEY_ACTIVATION = "activation_key"

    private const val ACTIVATION_ENDPOINT = "https://kinsfolktv.vercel.app/api/activate"

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
        val stored = prefs(context).getString(KEY_ACTIVATION, null)
        return !stored.isNullOrBlank() && isKeyFormatValid(stored)
    }

    /** True when the trial has ended and no valid activation key is stored. */
    fun isActivationRequired(context: Context): Boolean =
        !isTrialActive(context) && !isActivated(context)

    /**
     * Local format validation. Replace/augment with [validateKeyWithServer]
     * for production to prevent key sharing.
     */
    fun isKeyFormatValid(key: String): Boolean {
        return ActivationKeyGenerator.verify(key)
    }

    /**
     * Server-side validation. Posts the key (and a stable device id) to the
     * activation endpoint. Returns true only if the server replies { valid: true }.
     * Falls back to local format check when offline so already-activated devices
     * stay unlocked (the local activation flag is the source of truth once set).
     */
    suspend fun validateKeyWithServer(key: String, deviceId: String? = null): Boolean {
        val payload = org.json.JSONObject().apply {
            put("key", key)
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
            val response = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
            val json = org.json.JSONObject(response)
            json.optBoolean("valid", false)
        } catch (e: Exception) {
            // Offline: trust the local format check; the stored activation flag rules afterwards.
            isKeyFormatValid(key)
        }
    }

    /** Stores the activation key after successful validation. */
    fun activate(context: Context, key: String) {
        prefs(context).edit { putString(KEY_ACTIVATION, key.trim()) }
    }
}
