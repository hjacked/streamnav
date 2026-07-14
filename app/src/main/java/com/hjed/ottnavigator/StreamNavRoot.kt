package com.hjed.ottnavigator

import androidx.compose.runtime.*

@Composable
fun StreamNavRoot(
    defaultPlaylistUrl: String,
    onExitApp: () -> Unit,
    onPlayerKeyHandlerChanged: (((Int) -> Boolean)?) -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen { showSplash = false }
    } else {
        StreamNavApp(
            playlistUrl = defaultPlaylistUrl,
            onExitApp = onExitApp,
            onPlayerKeyHandlerChanged = onPlayerKeyHandlerChanged
        )
    }
}
