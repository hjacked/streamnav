package com.hjed.ottnavigator

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    channels: List<Channel>,
    selectedChannel: Channel?,
    streamFailureMessage: String? = null,
    showChannelOverlay: Boolean,
    showLogoOverlay: Boolean,
    showRightMenu: Boolean,
    showExitDialog: Boolean,
    isFavorite: Boolean,
    showPlaybackControls: Boolean,
    isDrawerOpen: Boolean,
    onNavigateChannel: (Int) -> Unit,
    onOpenChannelDrawer: () -> Unit,
    onRefreshChannel: (Channel) -> Unit,
    onStreamPlaybackFailed: () -> Unit,
    onOpenRightMenu: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onTogglePlaybackControls: () -> Unit,
    onDrawerInteraction: () -> Unit,
    onDismissRightMenu: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit
) {
    val context = LocalContext.current
    val playerFocusRequester = remember { FocusRequester() }
    val isPlayerInputEnabled = !isDrawerOpen && !showRightMenu && !showExitDialog
    val layout = rememberStreamNavLayout()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(playerFocusRequester)
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        when {
                            totalDragY > 70f -> onNavigateChannel(-1)
                            totalDragY < -70f -> onNavigateChannel(1)
                        }
                    },
                    onDragCancel = { totalDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        when {
                            totalDragX < -70f -> onOpenRightMenu()
                            totalDragX > 70f -> onOpenChannelDrawer()
                        }
                    },
                    onDragCancel = { totalDragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    }
                )
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || !isPlayerInputEnabled) {
                    return@onPreviewKeyEvent false
                }

                when (keyEvent.key) {
                    Key.DirectionUp -> {
                        onNavigateChannel(-1)
                        true
                    }
                    Key.DirectionDown -> {
                        onNavigateChannel(1)
                        true
                    }
                    Key.DirectionLeft -> {
                        onOpenChannelDrawer()
                        true
                    }
                    Key.DirectionCenter, Key.Enter -> {
                        val channel = selectedChannel
                        if (channel == null) {
                            onOpenChannelDrawer()
                            true
                        } else {
                            onRefreshChannel(channel)
                            Toast.makeText(context, "Refreshing stream...", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                    Key.DirectionRight -> {
                        onOpenRightMenu()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        selectedChannel
            ?.takeIf { it.url.isNotBlank() }
            ?.let { channel ->
                key(channel.url, channel.drmLicenseKey) {
                    VideoPlayer(
                        channel = channel,
                        onPlaybackError = { onStreamPlaybackFailed() }
                    )
                }
            }

        LaunchedEffect(Unit, isPlayerInputEnabled) {
            if (isPlayerInputEnabled) {
                delay(50)
                playerFocusRequester.requestFocus()
            }
        }

        StreamNavLogoOverlay(
            visible = showLogoOverlay && isPlayerInputEnabled
        )

        ChannelInfoOverlay(
            visible = showChannelOverlay && isPlayerInputEnabled,
            channel = selectedChannel,
            displayIndex = selectedChannel
                ?.let { channels.indexOf(it) }
                ?.takeIf { it != -1 }
                ?.plus(1)
                ?: 1
        )

        PlaybackControlsOverlay(
            visible = showPlaybackControls && isPlayerInputEnabled
        )

        AnimatedVisibility(
            visible = !streamFailureMessage.isNullOrBlank(),
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(180)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(180)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (layout.isCompact) 12.dp else 24.dp)
        ) {
            StreamFailureNotification(
                message = streamFailureMessage.orEmpty()
            )
        }

        AnimatedVisibility(
            visible = showRightMenu,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(120)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(120)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            RightSettingsDrawer(
                selectedChannel = selectedChannel,
                isFavorite = isFavorite,
                showPlaybackControls = showPlaybackControls,
                drawerWidth = layout.rightDrawerWidth,
                compact = layout.isCompact,
                onToggleFavorite = onToggleFavorite,
                onTogglePlaybackControls = onTogglePlaybackControls,
                onRefreshChannel = onRefreshChannel,
                onUserInteraction = onDrawerInteraction,
                onDismiss = onDismissRightMenu
            )
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                onConfirmExit = onConfirmExit,
                onDismiss = onDismissExitDialog
            )
        }
    }
}

@Composable
private fun StreamFailureNotification(
    message: String
) {
    val layout = rememberStreamNavLayout()

    Row(
        modifier = Modifier
            .widthIn(max = if (layout.isCompact) 300.dp else 360.dp)
            .background(MotifSurface.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .border(1.dp, MotifFocusNeon.copy(alpha = 0.54f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (layout.isCompact) 14.dp else 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Stream failed",
            tint = MotifFocusNeon,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.widthIn(max = if (layout.isCompact) 240.dp else 300.dp)
        ) {
            Text(
                text = "Stream failed",
                color = Color.White,
                fontSize = if (layout.isCompact) 14.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = if (layout.isCompact) 12.sp else 13.sp,
                lineHeight = if (layout.isCompact) 16.sp else 17.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
