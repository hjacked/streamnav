package com.hjed.ottnavigator

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.util.Log

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    channel: Channel,
    onPlaybackError: (PlaybackException) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mediaSourceFactory = remember(channel.url, channel.drmLicenseKey, channel.headers) {
        val userAgentFromHeaders = channel.headers["User-Agent"]
        val effectiveUserAgent = userAgentFromHeaders
            ?: "Mozilla/5.0 (Linux; Android 13; Build/TP1A.220624.014) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.7103.61 Mobile Safari/537.36"

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent(effectiveUserAgent)
            .apply {
                if (channel.headers.isNotEmpty()) {
                    setDefaultRequestProperties(channel.headers)
                }
            }

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val customDrmSessionManager = if (!channel.drmLicenseKey.isNullOrBlank()) {
            try {
                val clearKeyJson = buildClearKeyJson(channel.drmLicenseKey)
                if (clearKeyJson != null) {
                    val localDrmCallback = androidx.media3.exoplayer.drm.LocalMediaDrmCallback(clearKeyJson.toByteArray())

                    androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(
                            C.CLEARKEY_UUID,
                            androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER
                        )
                        .setMultiSession(false)
                        .setPlayClearSamplesWithoutKeys(true)
                        .build(localDrmCallback)
                } else null
            } catch (e: Exception) {
                Log.e("StreamNav", "DRM init failed: ${e.message}")
                null
            }
        } else null

        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(httpDataSourceFactory)
            .apply {
                if (customDrmSessionManager != null) {
                    setDrmSessionManagerProvider { customDrmSessionManager }
                } else {
                    setDrmSessionManagerProvider { androidx.media3.exoplayer.drm.DrmSessionManager.DRM_UNSUPPORTED }
                }
            }
    }

    val exoPlayer = remember(channel.url, channel.drmLicenseKey, channel.headers) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError(error)
            }
        }

        exoPlayer.addListener(errorListener)

        onDispose {
            exoPlayer.removeListener(errorListener)
        }
    }

    LaunchedEffect(exoPlayer, channel.url, channel.drmLicenseKey) {
        val mediaItemBuilder = MediaItem.Builder().setUri(channel.url)

        val mimeType = inferStreamMimeType(channel.url, channel.manifestType)
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }

        if (!channel.drmLicenseKey.isNullOrBlank()) {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .build()
            )
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                keepScreenOn = true
                isFocusable = false
                isFocusableInTouchMode = false
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun base64UrlEncode(hexString: String): String {
    val bArray = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return android.util.Base64.encodeToString(bArray, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
}

private fun buildClearKeyJson(licenseKey: String): String? {
    val trimmed = licenseKey.trim()

    if (trimmed.startsWith("{")) {
        return normalizeBase64UrlInJson(trimmed)
    }

    val parts = trimmed.split(":")
    if (parts.size == 2) {
        val keyId = parts[0]
        val key = parts[1]
        return """{"keys":[{"kty":"oct","kid":"${base64UrlEncode(keyId)}","k":"${base64UrlEncode(key)}"}],"type":"temporary"}"""
    }

    return null
}

private fun inferStreamMimeType(url: String, manifestType: String?): String? {
    if (!manifestType.isNullOrBlank()) {
        return when (manifestType.lowercase()) {
            "hls" -> MimeTypes.APPLICATION_M3U8
            "dash", "mpd" -> MimeTypes.APPLICATION_MPD
            "ss", "smoothstreaming" -> MimeTypes.APPLICATION_SS
            else -> null
        }
    }

    val path = url.substringBefore("?").substringBefore("#").lowercase()
    return when {
        path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
        path.endsWith(".ism") || path.endsWith(".ism/manifest") -> MimeTypes.APPLICATION_SS
        else -> null
    }
}

/**
 * ClearKey CDM requires base64url encoding (RFC 7515 / JWK) for "k" and "kid" values:
 *   '+' → '-', '/' → '_', no trailing '='
 *
 * Some M3U playlists supply standard base64 instead.  This function finds every "k" and
 * "kid" value in the JSON and converts them from standard base64 to base64url.
 */
private fun normalizeBase64UrlInJson(json: String): String {
    val regex = Regex(""""(k(?:id)?)"(\s*:\s*)"([A-Za-z0-9+/=_-]+)"""")
    return regex.replace(json) { match ->
        val fieldName = match.groupValues[1]
        val separator = match.groupValues[2]
        val raw = match.groupValues[3]
        val b64url = raw.replace('+', '-').replace('/', '_').trimEnd('=')
        """"$fieldName"$separator"$b64url""""
    }
}
