package com.hjed.ottnavigator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistsMenuMode(
    playlists: List<Playlist>,
    activeUrl: String,
    onAddPlaylist: (String, String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onUserInteraction: () -> Unit = {},
    onMoveToSettings: () -> Unit = {}
) {
    var newUrl by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val urlFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val addButtonFocusRequester = remember { FocusRequester() }
    val firstPlaylistFocusRequester = remember { FocusRequester() }

    LaunchedEffect(playlists.size) {
        if (playlists.isNotEmpty()) {
            try {
                firstPlaylistFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

    val pendingDelete = playlistToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete playlist?") },
            text = { Text("Do you want to delete ${pendingDelete.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pendingDelete.id != "default") {
                            onDeletePlaylist(pendingDelete)
                        }
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = MotifSurface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = newUrl,
            onValueChange = {
                onUserInteraction()
                newUrl = it
            },
            placeholder = { Text("Paste M3U URL...", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .focusRequester(urlFocusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionDown -> {
                                nameFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionRight -> {
                                onMoveToSettings()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
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

        TextField(
            value = newName,
            onValueChange = {
                onUserInteraction()
                newName = it
            },
            placeholder = { Text("Playlist Name (e.g. Sports)...", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .focusRequester(nameFocusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                urlFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionDown -> {
                                addButtonFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionRight -> {
                                onMoveToSettings()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
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

        Button(
            onClick = {
                onUserInteraction()
                if (newUrl.isNotBlank() && newName.isNotBlank()) {
                    onAddPlaylist(newName, newUrl)
                    newUrl = ""
                    newName = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(addButtonFocusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                nameFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionDown -> {
                                if (playlists.isNotEmpty()) {
                                    firstPlaylistFocusRequester.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                onMoveToSettings()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            colors = ButtonDefaults.buttonColors(containerColor = MotifFocusNeon)
        ) {
            Text("Add Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(playlists) { index, playlist ->
                val isActive = playlist.url == activeUrl
                var isFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstPlaylistFocusRequester) else Modifier)
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { keyEvent ->
                            when {
                                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight -> {
                                    onMoveToSettings()
                                    true
                                }

                                keyEvent.type == KeyEventType.KeyDown &&
                                    keyEvent.key == Key.DirectionUp &&
                                    index == 0 -> {
                                    addButtonFocusRequester.requestFocus()
                                    true
                                }

                                keyEvent.type == KeyEventType.KeyUp -> {
                                    when (keyEvent.key) {
                                        Key.Enter,
                                        Key.NumPadEnter,
                                        Key.DirectionCenter -> {
                                            onSelectPlaylist(playlist)
                                            true
                                        }
                                        else -> false
                                    }
                                }

                                else -> false
                            }
                        }
                        .focusable()
                        .background(
                            color = if (isFocused) MotifSurface else if (isActive) MotifHighlightPlaying else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MotifFocusNeon else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .combinedClickable(
                            onClick = {
                                onUserInteraction()
                                onSelectPlaylist(playlist)
                            },
                            onLongClick = {
                                onUserInteraction()
                                if (playlist.id != "default") {
                                    playlistToDelete = playlist
                                }
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playlist.name,
                        color = if (isFocused || isActive) MotifFocusNeon else Color.White,
                        fontWeight = if (isFocused || isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )

                    if (playlist.id != "default") {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Delete Playlist",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    onUserInteraction()
                                    playlistToDelete = playlist
                                }
                        )
                    }
                }
            }
        }
    }
}
