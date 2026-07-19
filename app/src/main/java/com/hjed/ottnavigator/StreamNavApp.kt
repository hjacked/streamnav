package com.hjed.ottnavigator

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val DRAWER_IDLE_TIMEOUT_MS = 15_000L

@Composable
fun StreamNavApp(
    playlistUrl: String,
    onExitApp: () -> Unit,
    onPlayerKeyHandlerChanged: (((Int) -> Boolean)?) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("streamnav_prefs", Context.MODE_PRIVATE)
    }

    var currentMenuMode by remember { mutableStateOf(LeftMenuMode.PLAYLISTS) }
    var playlists by remember { mutableStateOf(loadPlaylists(context)) }
    var activePlaylistUrl by remember {
        val savedActiveUrl = sharedPrefs.getString("active_playlist_url", null)
        val fallbackUrl = playlists.firstOrNull()?.url ?: playlistUrl
        val startingUrl = savedActiveUrl
            ?.takeIf { savedUrl -> playlists.any { it.url == savedUrl } }
            ?: fallbackUrl

        if (startingUrl != savedActiveUrl) {
            sharedPrefs.edit { putString("active_playlist_url", startingUrl) }
        }

        mutableStateOf(startingUrl)
    }

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showRightMenu by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var latestVersionUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All channels") }
    var showChannelOverlay by remember { mutableStateOf(false) }
    var showLogoOverlay by remember { mutableStateOf(false) }
    var showPlaybackControls by remember { mutableStateOf(false) }
    var lastChannelNavigationAt by remember { mutableLongStateOf(0L) }
    var drawerInteractionVersion by remember { mutableIntStateOf(0) }
    var favoriteChannelUrls by remember {
        mutableStateOf(
            sharedPrefs.getStringSet("favorite_channel_urls", emptySet())
                ?.toSet()
                ?: emptySet()
        )
    }
    var streamFailureMessage by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val settingsFocusRequester = remember { FocusRequester() }
    val playlistsFocusRequester = remember { FocusRequester() }
    val channelsFocusRequester = remember { FocusRequester() }
    val groupListState = rememberLazyListState()
    val listState = rememberLazyListState()
    val latestSelectedChannel by rememberUpdatedState(selectedChannel)

    val filteredChannels = remember(channels, searchQuery, selectedCategory, favoriteChannelUrls) {
        filterChannels(
            channels = channels,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            favoriteChannelUrls = favoriteChannelUrls
        )
    }

    LaunchedEffect(Unit) {
        LicenseManager.ensureTrialStarted(context)
        if (LicenseManager.isActivationRequired(context)) {
            showActivationDialog = true
        }

        val latest = UpdateManager.getLatestVersion()
        if (latest != null && latest.versionCode > BuildConfig.VERSION_CODE) {
            latestVersionUrl = latest.downloadUrl
            showUpdateDialog = true
        }
    }

    LaunchedEffect(selectedChannel) {
        if (selectedChannel != null) {
            showChannelOverlay = true
            showLogoOverlay = true
            delay(3500)
            showChannelOverlay = false
            showLogoOverlay = false
        }
    }

    LaunchedEffect(showPlaybackControls) {
        if (showPlaybackControls) {
            delay(4200)
            showPlaybackControls = false
        }
    }

    LaunchedEffect(streamFailureMessage) {
        if (!streamFailureMessage.isNullOrBlank()) {
            delay(4000)
            streamFailureMessage = null
        }
    }

    LaunchedEffect(drawerState.isOpen, currentMenuMode) {
        if (!drawerState.isOpen) return@LaunchedEffect

        delay(120)
        try {
            when (currentMenuMode) {
                LeftMenuMode.CHANNELS -> {
                    if (filteredChannels.isNotEmpty()) {
                        val currentOrFirstUrl = latestSelectedChannel?.url ?: filteredChannels.first().url
                        val targetIndex = filteredChannels
                            .indexOfFirst { it.url == currentOrFirstUrl }
                            .coerceAtLeast(0)

                        listState.scrollToItem(targetIndex)
                        delay(50)
                        channelsFocusRequester.requestFocus()
                    } else {
                        channelsFocusRequester.requestFocus()
                    }
                }
                LeftMenuMode.SETTINGS -> settingsFocusRequester.requestFocus()
                LeftMenuMode.PLAYLISTS -> playlistsFocusRequester.requestFocus()
            }
        } catch (_: IllegalStateException) {
        }
    }

    LaunchedEffect(drawerState.isOpen, showRightMenu, showExitDialog, showUpdateDialog, drawerInteractionVersion) {
        if (showExitDialog || showUpdateDialog || (!drawerState.isOpen && !showRightMenu)) {
            return@LaunchedEffect
        }

        delay(DRAWER_IDLE_TIMEOUT_MS)

        if (drawerState.isOpen) {
            drawerState.close()
        }
        showRightMenu = false
    }

    LaunchedEffect(activePlaylistUrl) {
        selectedChannel = null
        selectedCategory = "All channels"
        streamFailureMessage = null

        val loadedChannels = withContext(Dispatchers.IO) {
            try {
                M3uParser.parse(readTextWithTimeout(activePlaylistUrl, timeoutMs = 8_000))
            } catch (_: Exception) {
                emptyList()
            }
        }

        channels = loadedChannels

        val savedLastChannelUrl = sharedPrefs.getString("last_channel_url", null)
        val startupChannel = savedLastChannelUrl
            ?.let { lastUrl -> loadedChannels.find { it.url == lastUrl } }
            ?: loadedChannels.firstOrNull()

        selectedChannel = startupChannel
        startupChannel?.let { channel ->
            sharedPrefs.edit { putString("last_channel_url", channel.url) }
        }
    }

    fun saveLastChannel(channel: Channel) {
        sharedPrefs.edit { putString("last_channel_url", channel.url) }
    }

    fun markDrawerInteraction() {
        drawerInteractionVersion += 1
    }

    fun toggleFavorite(channel: Channel) {
        favoriteChannelUrls = if (channel.url in favoriteChannelUrls) {
            favoriteChannelUrls - channel.url
        } else {
            favoriteChannelUrls + channel.url
        }

        sharedPrefs.edit {
            putStringSet("favorite_channel_urls", favoriteChannelUrls)
        }
    }

    fun navigateToChannel(offset: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastChannelNavigationAt < 320L) return
        lastChannelNavigationAt = now

        val nextChannel = getNextChannel(
            channels = channels,
            currentChannel = selectedChannel,
            offset = offset
        ) ?: return

        streamFailureMessage = null
        selectedChannel = nextChannel
        saveLastChannel(nextChannel)
    }

    fun openChannelDrawer() {
        markDrawerInteraction()
        showRightMenu = false
        showChannelOverlay = false
        showLogoOverlay = false
        currentMenuMode = LeftMenuMode.CHANNELS
        scope.launch { drawerState.open() }
    }

    fun openRightDrawer() {
        markDrawerInteraction()
        scope.launch {
            if (drawerState.isOpen) {
                drawerState.close()
            }

            showChannelOverlay = false
            showLogoOverlay = false
            showRightMenu = true
        }
    }

    val playerKeyHandler by rememberUpdatedState { keyCode: Int ->
        if (showUpdateDialog || showExitDialog || drawerState.isOpen || showRightMenu) {
            false
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    navigateToChannel(-1)
                    true
                }

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    navigateToChannel(1)
                    true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    openChannelDrawer()
                    true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    openRightDrawer()
                    true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    selectedChannel?.let { channel ->
                        streamFailureMessage = null
                        selectedChannel = null
                        scope.launch {
                            delay(10)
                            selectedChannel = channel
                        }
                    }
                    true
                }

                else -> false
            }
        }
    }

    DisposableEffect(Unit) {
        onPlayerKeyHandlerChanged { keyCode -> playerKeyHandler(keyCode) }
        onDispose { onPlayerKeyHandlerChanged(null) }
    }

    BackHandler(enabled = true) {
        when {
            showUpdateDialog -> {
            }

            showActivationDialog -> {
            }

            showExitDialog -> {
            }

            drawerState.isOpen -> scope.launch { drawerState.close() }
            showRightMenu -> showRightMenu = false

            else -> {
                showExitDialog = true
            }
        }
    }

    if (showActivationDialog) {
        ActivationDialog(
            context = context,
            onActivated = {
                // ActivationDialog already persisted the key via LicenseManager.
                showActivationDialog = false
            }
        )
    }

    if (showUpdateDialog) {
        UpdateAvailableDialog(
            onDownload = {
                UpdateManager.downloadAndInstall(context, latestVersionUrl)
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showRightMenu,
        drawerContent = {
            LeftDrawerUI(
                currentMenuMode = currentMenuMode,
                onMenuModeChange = { currentMenuMode = it },
                playlists = playlists,
                activePlaylistUrl = activePlaylistUrl,
                onAddPlaylist = { name, url ->
                    playlists = playlists + Playlist(
                        java.util.UUID.randomUUID().toString(),
                        name,
                        url
                    )
                    savePlaylists(context, playlists)
                },
                onDeletePlaylist = { playlistToDelete ->
                    if (playlistToDelete.id != "default") {
                        playlists = playlists.filter { it.id != playlistToDelete.id }
                        savePlaylists(context, playlists)

                        if (activePlaylistUrl == playlistToDelete.url) {
                            val nextPlaylistUrl = playlists.firstOrNull()?.url ?: playlistUrl
                            activePlaylistUrl = nextPlaylistUrl
                            sharedPrefs.edit { putString("active_playlist_url", nextPlaylistUrl) }
                        }
                    }
                },
                onSelectPlaylist = { selected ->
                    activePlaylistUrl = selected.url
                    sharedPrefs.edit { putString("active_playlist_url", activePlaylistUrl) }
                    currentMenuMode = LeftMenuMode.CHANNELS
                },
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    markDrawerInteraction()
                    searchQuery = it
                },
                filteredChannels = filteredChannels,
                selectedChannel = selectedChannel,
                onChannelSelected = {
                    streamFailureMessage = null
                    selectedChannel = it
                    saveLastChannel(it)
                    scope.launch { drawerState.close() }
                },
                onUserInteraction = ::markDrawerInteraction,
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    searchQuery = ""
                    currentMenuMode = LeftMenuMode.CHANNELS
                },
                settingsItems = defaultSettingsCategories,
                listState = listState,
                groupListState = groupListState,
                playlistsFocusRequester = playlistsFocusRequester,
                settingsFocusRequester = settingsFocusRequester,
                channelsFocusRequester = channelsFocusRequester,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box {
            PlayerScreen(
                channels = channels,
                selectedChannel = selectedChannel,
                streamFailureMessage = streamFailureMessage,
                showChannelOverlay = showChannelOverlay,
                showLogoOverlay = showLogoOverlay,
                showRightMenu = showRightMenu,
                showExitDialog = showExitDialog,
                isFavorite = selectedChannel?.url in favoriteChannelUrls,
                showPlaybackControls = showPlaybackControls,
                isDrawerOpen = drawerState.isOpen,
                onNavigateChannel = ::navigateToChannel,
                onOpenChannelDrawer = ::openChannelDrawer,
                onRefreshChannel = { channel ->
                    scope.launch {
                        streamFailureMessage = null
                        selectedChannel = null
                        delay(10)
                        selectedChannel = channel
                    }
                },
                onStreamPlaybackFailed = {
                    streamFailureMessage = "Please choose another channel."
                },
                onOpenRightMenu = ::openRightDrawer,
                onToggleFavorite = ::toggleFavorite,
                onTogglePlaybackControls = {
                    showPlaybackControls = true
                },
                onDrawerInteraction = ::markDrawerInteraction,
                onDismissRightMenu = { showRightMenu = false },
                onConfirmExit = onExitApp,
                onDismissExitDialog = { showExitDialog = false }
            )
        }
    }
}

private fun readTextWithTimeout(url: String, timeoutMs: Int): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        instanceFollowRedirects = true
    }

    return try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun UpdateAvailableDialog(
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Mandatory update: do not dismiss. */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text("Update Required") },
        text = { Text("A new version is available. Please update to continue using StreamNav.") },
        confirmButton = {
            TextButton(
                onClick = onDownload,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MotifFocusNeon,
                    contentColor = Color.Black
                )
            ) {
                Text("Update Now")
            }
        }
    )
}
