package com.example.kinsfolktv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelFiltersTest {
    private val channels = listOf(
        Channel(name = "Metro News", url = "news", groupTitle = "Live TV", group = "Live TV"),
        Channel(name = "Cinema One", url = "movie", groupTitle = "Movies", group = "Movies"),
        Channel(name = "Drama Hub", url = "series", groupTitle = "Series", group = "Series")
    )

    @Test
    fun filterChannels_usesGroupMetadataForCategories() {
        assertEquals(listOf("Cinema One"), filterChannels(channels, "", "Movies").map { it.name })
        assertEquals(listOf("Drama Hub"), filterChannels(channels, "", "Series").map { it.name })
        assertEquals(listOf("Metro News"), filterChannels(channels, "", "Live TV").map { it.name })
    }

    @Test
    fun filterChannels_returnsFavoriteChannels() {
        val favorites = setOf("news", "series")

        assertEquals(
            listOf("Metro News", "Drama Hub"),
            filterChannels(channels, "", "Favorites", favorites).map { it.name }
        )
    }
    @Test
    fun getNextChannel_wrapsAroundByUrl() {
        assertEquals("news", getNextChannel(channels, channels.last(), 1)?.url)
        assertEquals("series", getNextChannel(channels, channels.first(), -1)?.url)
        assertNull(getNextChannel(emptyList(), null, 1))
    }
}
