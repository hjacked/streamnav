package com.hjed.ottnavigator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LeftDrawerUI(
    currentMenuMode: LeftMenuMode,
    onMenuModeChange: (LeftMenuMode) -> Unit,
    playlists: List<Playlist>,
    activePlaylistUrl: String,
    onAddPlaylist: (String, String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredChannels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelected: (Channel) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    settingsItems: List<RightMenuOption>,
    listState: LazyListState,
    groupListState: LazyListState,
    playlistsFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    channelsFocusRequester: FocusRequester,
    onUserInteraction: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val layout = rememberStreamNavLayout()
    val searchFocusRequester = remember { FocusRequester() }
    var focusedChannelIndex by remember { mutableIntStateOf(0) }
    var isChannelListFocused by remember { mutableStateOf(false) }
    val selectedChannelUrl = selectedChannel?.url

    LaunchedEffect(filteredChannels, selectedChannelUrl) {
        if (filteredChannels.isEmpty()) {
            focusedChannelIndex = 0
            return@LaunchedEffect
        }

        val selectedIndex = filteredChannels.indexOfFirst { it.url == selectedChannelUrl }
        focusedChannelIndex = if (selectedIndex >= 0) {
            selectedIndex
        } else {
            focusedChannelIndex.coerceIn(0, filteredChannels.lastIndex)
        }
    }

    LaunchedEffect(isChannelListFocused, focusedChannelIndex, filteredChannels) {
        if (isChannelListFocused && filteredChannels.isNotEmpty()) {
            focusedChannelIndex = focusedChannelIndex.coerceIn(0, filteredChannels.lastIndex)
            listState.scrollToKeepItemVisible(focusedChannelIndex)
        }
    }

    fun focusChannelAt(
        index: Int,
        scrollMode: ChannelFocusScrollMode = ChannelFocusScrollMode.JumpToItem
    ) {
        if (filteredChannels.isEmpty()) return

        onUserInteraction()
        val safeIndex = index.coerceIn(0, filteredChannels.lastIndex)
        focusedChannelIndex = safeIndex

        scope.launch {
            when (scrollMode) {
                ChannelFocusScrollMode.JumpToItem -> listState.scrollToItem(safeIndex)
                ChannelFocusScrollMode.KeepFocusMoving -> listState.scrollToKeepItemVisible(safeIndex)
            }
            delay(50)
            try {
                channelsFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun moveChannelFocus(offset: Int) {
        if (filteredChannels.isEmpty()) return

        onUserInteraction()
        val currentIndex = focusedChannelIndex.coerceIn(0, filteredChannels.lastIndex)
        val targetIndex = when {
            offset > 0 && currentIndex == filteredChannels.lastIndex -> 0
            offset < 0 && currentIndex == 0 -> filteredChannels.lastIndex
            else -> (currentIndex + offset).coerceIn(0, filteredChannels.lastIndex)
        }
        val isWrap = targetIndex == 0 && offset > 0 || targetIndex == filteredChannels.lastIndex && offset < 0

        focusChannelAt(
            index = targetIndex,
            scrollMode = if (isWrap) ChannelFocusScrollMode.JumpToItem else ChannelFocusScrollMode.KeepFocusMoving
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(layout.leftDrawerWidth)
            .background(MotifDrawerGlass.copy(alpha = 0.72f))
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    onUserInteraction()
                }
                false
            }
            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
    ) {
        // --- TOP TABS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabLabel(
                text = "Playlists",
                isActive = currentMenuMode == LeftMenuMode.PLAYLISTS,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onUserInteraction()
                        onMenuModeChange(LeftMenuMode.PLAYLISTS)
                    }
            )
            TabLabel(
                text = "Settings",
                isActive = currentMenuMode == LeftMenuMode.SETTINGS,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onUserInteraction()
                        onMenuModeChange(LeftMenuMode.SETTINGS)
                    }
            )
            TabLabel(
                text = "Channels",
                isActive = currentMenuMode == LeftMenuMode.CHANNELS,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onUserInteraction()
                        onMenuModeChange(LeftMenuMode.CHANNELS)
                    }
            )
        }

        // --- PLAYLISTS MODE ---
        if (currentMenuMode == LeftMenuMode.PLAYLISTS) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(playlistsFocusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight) {
                            onMenuModeChange(LeftMenuMode.SETTINGS)
                            true
                        } else false
                    }
            ) {
                PlaylistsMenuMode(
                    playlists = playlists,
                    activeUrl = activePlaylistUrl,
                    onAddPlaylist = onAddPlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onSelectPlaylist = onSelectPlaylist,
                    onUserInteraction = onUserInteraction,
                    onMoveToSettings = { onMenuModeChange(LeftMenuMode.SETTINGS) }
                )
            }
        }

        // --- CHANNELS MODE ---
        else if (currentMenuMode == LeftMenuMode.CHANNELS) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search ${filteredChannels.size} channels...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .focusRequester(searchFocusRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            onUserInteraction()
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    focusChannelAt(0)
                                    true
                                }
                                Key.DirectionUp -> {
                                    focusChannelAt(filteredChannels.lastIndex)
                                    true
                                }
                                Key.DirectionLeft -> {
                                    onMenuModeChange(LeftMenuMode.SETTINGS)
                                    true
                                }
                                Key.DirectionRight -> {
                                    onCloseDrawer()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MotifSurface,
                    unfocusedContainerColor = MotifInputSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MotifAccent,
                    focusedIndicatorColor = MotifAccent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(channelsFocusRequester)
                    .onFocusChanged { focusState -> isChannelListFocused = focusState.isFocused }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            onUserInteraction()
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    moveChannelFocus(1)
                                    true
                                }
                                Key.DirectionUp -> {
                                    moveChannelFocus(-1)
                                    true
                                }
                                Key.DirectionLeft -> {
                                    onMenuModeChange(LeftMenuMode.SETTINGS)
                                    true
                                }
                                Key.DirectionRight -> {
                                    onCloseDrawer()
                                    true
                                }
                                Key.DirectionCenter, Key.Enter -> {
                                    filteredChannels.getOrNull(focusedChannelIndex)?.let(onChannelSelected)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .focusable()
            ) {
                if (filteredChannels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No channels available" else "No matching channels",
                                color = MotifFocusNeon,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Choose another playlist or category."
                                } else {
                                    "Try a shorter search or clear the filter."
                                },
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = filteredChannels,
                    key = { index, channel -> "${channel.url}::$index" }
                ) { index, channel ->
                    val isCurrentlyPlaying = selectedChannel == channel
                    val isFocused = isChannelListFocused && index == focusedChannelIndex

                    val accentColor = if (isFocused) MotifFocusNeon else if (isCurrentlyPlaying) MotifAccent else Color.Transparent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .drawBehind {
                                if (accentColor != Color.Transparent) {
                                    drawLine(
                                        color = accentColor,
                                        start = Offset(0f, size.height * 0.15f),
                                        end = Offset(0f, size.height * 0.85f),
                                        strokeWidth = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            .background(
                                color = if (isFocused) MotifFocusNeon.copy(alpha = 0.06f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .pointerInput(channel.url) {
                                detectTapGestures {
                                    onUserInteraction()
                                    onChannelSelected(channel)
                                }
                            }
                            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(channel.logoUrl)
                                        .size(32, 32)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0x15FFFFFF), shape = RoundedCornerShape(4.dp))
                                        .padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0x22FFFFFF), shape = RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("TV", fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = channel.name,
                                fontSize = if (layout.isCompact) 14.sp else 15.sp,
                                maxLines = 1,
                                color = if (isFocused) MotifFocusNeon else if (isCurrentlyPlaying) Color.White else Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // --- SETTINGS MODE ---
        else if (currentMenuMode == LeftMenuMode.SETTINGS) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val playlistName = remember(activePlaylistUrl) {
                    activePlaylistUrl.substringAfterLast("/").ifBlank { "Live Stream" }
                }

                Text(
                    text = playlistName,
                    color = MotifFocusNeon,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = groupListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(settingsItems) { index, item ->
                        var isFocusedItem by remember { mutableStateOf(false) }
                        val isCategoryActive = selectedCategory == item.title

                        val accentColor = if (isFocusedItem) MotifFocusNeon else if (isCategoryActive) MotifAccent else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(settingsFocusRequester) else Modifier)
                                .onFocusChanged { isFocusedItem = it.isFocused }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        onUserInteraction()
                                        when (keyEvent.key) {
                                            Key.DirectionRight -> {
                                                onMenuModeChange(LeftMenuMode.CHANNELS)
                                                true
                                            }
                                            Key.DirectionLeft -> {
                                                onMenuModeChange(LeftMenuMode.PLAYLISTS)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .drawBehind {
                                    if (accentColor != Color.Transparent) {
                                        drawLine(
                                            color = accentColor,
                                            start = Offset(0f, size.height * 0.15f),
                                            end = Offset(0f, size.height * 0.85f),
                                            strokeWidth = 2.5.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                .background(
                                    color = if (isFocusedItem) MotifFocusNeon.copy(alpha = 0.06f) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    onUserInteraction()
                                    onCategorySelected(item.title)
                                }
                                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isFocusedItem) MotifFocusNeon else if (isCategoryActive) MotifAccent else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = item.title,
                                color = if (isFocusedItem) MotifFocusNeon else if (isCategoryActive) Color.White else Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                fontWeight = if (isFocusedItem || isCategoryActive) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                    item {
                        Text(
                            text = "Version: ${BuildConfig.VERSION_NAME} (Build: ${BuildConfig.VERSION_CODE})",
                            color = MotifFocusNeon,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class ChannelFocusScrollMode {
    JumpToItem,
    KeepFocusMoving
}

@Composable
private fun TabLabel(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (isActive) 16.dp else 0.dp)
                .height(1.5.dp)
                .background(
                    if (isActive) MotifFocusNeon else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

private suspend fun LazyListState.scrollToKeepItemVisible(index: Int) {
    val visibleItems = layoutInfo.visibleItemsInfo
    val targetItem = visibleItems.firstOrNull { it.index == index }

    if (targetItem == null) {
        val nearestItem = visibleItems.minByOrNull { kotlin.math.abs(it.index - index) }

        if (nearestItem == null) {
            scrollToItem(index)
            return
        }

        val itemDistance = index - nearestItem.index
        val scrollAmount = itemDistance * nearestItem.size.toFloat()
        scrollBy(scrollAmount)
        return
    }

    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val targetStart = targetItem.offset
    val targetEnd = targetItem.offset + targetItem.size

    when {
        targetStart < viewportStart -> scrollBy((targetStart - viewportStart).toFloat())
        targetEnd > viewportEnd -> scrollBy((targetEnd - viewportEnd).toFloat())
    }
}
