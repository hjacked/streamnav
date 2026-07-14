package com.hjed.ottnavigator

fun filterChannels(
    channels: List<Channel>,
    searchQuery: String,
    selectedCategory: String,
    favoriteChannelUrls: Set<String> = emptySet()
): List<Channel> {
    var result = channels

    if (selectedCategory != "All channels") {
        result = when (selectedCategory) {
            "Movies" -> result.filter {
                it.matchesAny("movie", "movies", "cinema", "film", "films")
            }
            "Series" -> result.filter {
                it.matchesAny("series", "shows", "s01", "s02")
            }
            "Favorites" -> result.filter { it.url in favoriteChannelUrls }
            else -> result.filter {
                !it.matchesAny("movie", "movies", "cinema", "film", "films", "series", "shows")
            }
        }
    }

    if (searchQuery.isNotBlank()) {
        result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    return result
}

private fun Channel.matchesAny(vararg values: String): Boolean {
    val searchableText = listOfNotNull(name, group, groupTitle)
        .joinToString(" ")

    return values.any { value -> searchableText.contains(value, ignoreCase = true) }
}

fun getNextChannel(
    channels: List<Channel>,
    currentChannel: Channel?,
    offset: Int
): Channel? {
    if (channels.isEmpty()) return null

    val currentIndex = currentChannel?.let { current ->
        channels.indexOfFirst { it.url == current.url }
    } ?: -1

    val nextIndex = if (currentIndex == -1) {
        if (offset < 0) channels.lastIndex else 0
    } else {
        ((currentIndex + offset) % channels.size + channels.size) % channels.size
    }

    return channels[nextIndex]
}
