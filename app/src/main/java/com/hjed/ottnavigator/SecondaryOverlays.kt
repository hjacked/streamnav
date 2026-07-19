package com.hjed.ottnavigator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RightMenuOption(val title: String, val icon: ImageVector)

private data class RightDrawerAction(
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultFocusRequester = remember { FocusRequester() }
    val layout = rememberStreamNavLayout()

    LaunchedEffect(Unit) {
        defaultFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .onPreviewKeyEvent { keyEvent ->
                keyEvent.key == Key.Back
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(layout.dialogWidth)
                .background(MotifSurface, shape = RoundedCornerShape(10.dp))
                .border(0.5.dp, Color(0x15FFFFFF), shape = RoundedCornerShape(10.dp))
                .padding(if (layout.isCompact) 20.dp else 28.dp)
        ) {
            Text(
                text = "Exit StreamNav?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Are you sure you want to close the app?",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExitDialogActionButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(defaultFocusRequester)
                )

                ExitDialogActionButton(
                    text = "Exit App",
                    onClick = onConfirmExit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExitDialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isActive = isFocused || isHovered

    Box(
        modifier = modifier
            .height(46.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .hoverable(interactionSource)
            .focusable()
            .background(
                color = if (isActive) MotifFocusNeon else MotifBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Enter,
                        Key.NumPadEnter,
                        Key.DirectionCenter -> {
                            onClick()
                            true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }
            .border(
                width = if (isActive) 1.dp else 0.5.dp,
                color = if (isActive) MotifFocusNeon.copy(alpha = 0.5f) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RightSettingsDrawer(
    selectedChannel: Channel?,
    isFavorite: Boolean,
    showPlaybackControls: Boolean,
    drawerWidth: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    onToggleFavorite: (Channel) -> Unit,
    onTogglePlaybackControls: () -> Unit,
    onRefreshChannel: (Channel) -> Unit,
    onUserInteraction: () -> Unit,
    onDismiss: () -> Unit
) {
    val optionsFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val menuActions = remember(selectedChannel, isFavorite, showPlaybackControls) {
        listOf(
            RightDrawerAction(
                title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                enabled = selectedChannel != null,
                onClick = {
                    selectedChannel?.let(onToggleFavorite)
                    onDismiss()
                }
            ),
            RightDrawerAction(
                title = "Show playback controls",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    onTogglePlaybackControls()
                    onDismiss()
                }
            ),
            RightDrawerAction(
                title = "Refresh stream",
                icon = Icons.Default.Refresh,
                enabled = selectedChannel != null,
                onClick = {
                    selectedChannel?.let(onRefreshChannel)
                    onDismiss()
                }
            )
        )
    }
    val actionFocusRequesters = remember(menuActions.size) { List(menuActions.size) { FocusRequester() } }

    LaunchedEffect(menuActions) {
        delay(20)
        val firstEnabledIndex = menuActions.indexOfFirst { it.enabled }.coerceAtLeast(0)
        actionFocusRequesters.getOrNull(firstEnabledIndex)?.requestFocus() ?: optionsFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .background(MotifDrawerGlass)
            .drawBehind {
                drawLine(
                    color = MotifFocusNeon.copy(alpha = 0.1f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .padding(vertical = if (compact) 16.dp else 20.dp)
            .focusRequester(optionsFocusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    onUserInteraction()
                }

                if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.Back)) {
                    onDismiss()
                    true
                } else false
            }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MotifAccent.copy(alpha = 0.9f))
                .padding(horizontal = 22.dp, vertical = if (compact) 14.dp else 18.dp)
        ) {
            Column {
                Text(
                    text = selectedChannel?.name ?: "Playback",
                    color = Color.White,
                    fontSize = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedChannel?.groupTitle ?: "Current stream",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            itemsIndexed(menuActions) { index, action ->
                RightDrawerActionRow(
                    action = action,
                    index = index,
                    actions = menuActions,
                    focusRequesters = actionFocusRequesters,
                    listState = listState,
                    compact = compact,
                    onUserInteraction = onUserInteraction,
                    onDismiss = onDismiss
                )

                if (action.title == "Refresh stream") {
                    Spacer(modifier = Modifier.height(12.dp))
                    StreamDetails(
                        selectedChannel = selectedChannel,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RightDrawerActionRow(
    action: RightDrawerAction,
    index: Int,
    actions: List<RightDrawerAction>,
    focusRequesters: List<FocusRequester>,
    listState: LazyListState,
    compact: Boolean,
    onUserInteraction: () -> Unit,
    onDismiss: () -> Unit
) {
    var isItemFocused by remember { mutableStateOf(false) }
    val tintColor = when {
        !action.enabled -> Color.Gray
        isItemFocused -> MotifFocusNeon
        else -> Color.White
    }

    LaunchedEffect(isItemFocused) {
        if (isItemFocused) {
            listState.animateScrollToItem(index)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .focusRequester(focusRequesters[index])
            .onFocusChanged { isItemFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                onUserInteraction()
                when (keyEvent.key) {
                    Key.DirectionLeft, Key.Back -> {
                        onDismiss()
                        true
                    }
                    Key.DirectionUp -> {
                        focusPreviousEnabledAction(index, actions, focusRequesters)
                        true
                    }
                    Key.DirectionDown -> {
                        focusNextEnabledAction(index, actions, focusRequesters)
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (action.enabled) {
                            action.onClick()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
            .focusable(enabled = action.enabled)
            .clickable(enabled = action.enabled) {
                onUserInteraction()
                action.onClick()
            }
            .drawBehind {
                if (isItemFocused) {
                    drawLine(
                        color = MotifFocusNeon,
                        start = Offset(0f, size.height * 0.15f),
                        end = Offset(0f, size.height * 0.85f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            .background(
                color = if (isItemFocused) MotifFocusNeon.copy(alpha = 0.06f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 14.dp, vertical = if (compact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.title,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = action.title,
            color = if (action.enabled) tintColor else Color.Gray,
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = if (isItemFocused) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun StreamDetails(
    selectedChannel: Channel?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MotifAccentMuted.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        DetailText("Group", selectedChannel?.groupTitle ?: "Unknown")
        DetailText("Type", selectedChannel?.manifestType ?: "Auto")
        DetailText("Headers", (selectedChannel?.headers?.size ?: 0).toString())
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

private fun focusPreviousEnabledAction(
    index: Int,
    actions: List<RightDrawerAction>,
    focusRequesters: List<FocusRequester>
) {
    val previousIndex = (index - 1 downTo 0).firstOrNull { actions[it].enabled }
        ?: actions.indices.reversed().firstOrNull { actions[it].enabled }
        ?: return

    focusRequesters[previousIndex].requestFocus()
}

private fun focusNextEnabledAction(
    index: Int,
    actions: List<RightDrawerAction>,
    focusRequesters: List<FocusRequester>
) {
    val nextIndex = (index + 1 until actions.size).firstOrNull { actions[it].enabled }
        ?: actions.indices.firstOrNull { actions[it].enabled }
        ?: return

    focusRequesters[nextIndex].requestFocus()
}
