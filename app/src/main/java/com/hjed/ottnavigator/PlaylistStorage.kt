package com.hjed.ottnavigator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "streamnav_prefs"
private const val SAVED_PLAYLISTS_KEY = "saved_playlists"
private const val PLAYLIST_SEED_VERSION_KEY = "playlist_seed_version"

private const val CURRENT_PLAYLIST_SEED_VERSION = 1

private data class OptionalBuiltInPlaylist(
    val playlist: Playlist,
    val enabled: Boolean
)

private val protectedDefaultPlaylist = Playlist(
    id = "default",
    name = "StreamNav TV",
    url = "https://raw.githubusercontent.com/hjacked/iliong/refs/heads/main/channels.m3u"
)

private val optionalBuiltInPlaylists = listOf<OptionalBuiltInPlaylist>()

private val enabledBuiltInPlaylists: List<Playlist>
    get() = listOf(protectedDefaultPlaylist) + optionalBuiltInPlaylists
        .filter { it.enabled }
        .map { it.playlist }

private val disabledBuiltInPlaylistIds: Set<String>
    get() = optionalBuiltInPlaylists
        .filterNot { it.enabled }
        .map { it.playlist.id }
        .toSet()

fun savePlaylists(context: Context, playlists: List<Playlist>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(SAVED_PLAYLISTS_KEY, playlists.toJsonString())
        .apply()
}

fun loadPlaylists(context: Context): List<Playlist> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonString = prefs.getString(SAVED_PLAYLISTS_KEY, "[]") ?: "[]"
    val result = mutableListOf<Playlist>()

    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                Playlist(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    url = obj.getString("url")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val seedVersion = prefs.getInt(PLAYLIST_SEED_VERSION_KEY, 0)

    if (seedVersion == 0 && result.isEmpty()) {
        result.addAll(enabledBuiltInPlaylists)

        prefs.edit()
            .putString(SAVED_PLAYLISTS_KEY, result.toJsonString())
            .putInt(PLAYLIST_SEED_VERSION_KEY, CURRENT_PLAYLIST_SEED_VERSION)
            .apply()

        return result
    }

    if (seedVersion < CURRENT_PLAYLIST_SEED_VERSION) {
        result.removeAll { it.id in disabledBuiltInPlaylistIds }

        val defaultIndex = result.indexOfFirst { it.id == protectedDefaultPlaylist.id }
        if (defaultIndex >= 0) {
            result[defaultIndex] = protectedDefaultPlaylist
        } else {
            result.add(0, protectedDefaultPlaylist)
        }

        optionalBuiltInPlaylists
            .filter { it.enabled }
            .map { it.playlist }
            .forEach { builtInPlaylist ->
                val existingIndex = result.indexOfFirst { it.id == builtInPlaylist.id }
                if (existingIndex >= 0) {
                    result[existingIndex] = builtInPlaylist
                } else {
                    result.add(builtInPlaylist)
                }
            }

        prefs.edit()
            .putString(SAVED_PLAYLISTS_KEY, result.toJsonString())
            .putInt(PLAYLIST_SEED_VERSION_KEY, CURRENT_PLAYLIST_SEED_VERSION)
            .apply()

        return result
    }

    if (result.isEmpty()) {
        savePlaylists(context, enabledBuiltInPlaylists)
        return enabledBuiltInPlaylists
    }

    return result
}

private fun List<Playlist>.toJsonString(): String {
    val jsonArray = JSONArray()

    forEach { playlist ->
        val obj = JSONObject().apply {
            put("id", playlist.id)
            put("name", playlist.name)
            put("url", playlist.url)
        }
        jsonArray.put(obj)
    }

    return jsonArray.toString()
}
