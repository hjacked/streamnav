package com.hjed.ottnavigator

enum class LeftMenuMode {
    PLAYLISTS,
    SETTINGS,
    CHANNELS
}

data class Playlist(
    val id: String,
    val name: String,
    val url: String
)
