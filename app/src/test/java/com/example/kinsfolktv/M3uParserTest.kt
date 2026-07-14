package com.example.kinsfolktv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    @Test
    fun parse_extractsCommonIptvMetadata() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://cdn.example/logo.png" group-title="Movies",Cinema One
            #KODIPROP:inputstream.adaptive.manifest_type=mpd
            #KODIPROP:inputstream.adaptive.license_key=00112233445566778899aabbccddeeff:ffeeddccbbaa99887766554433221100
            #EXTVLCOPT:http-user-agent=KinsFolkTest
            #EXTVLCOPT:http-referer=https://example.com
            https://stream.example/cinema.mpd
        """.trimIndent()

        val channel = M3uParser.parse(content).single()

        assertEquals("Cinema One", channel.name)
        assertEquals("https://stream.example/cinema.mpd", channel.url)
        assertEquals("https://cdn.example/logo.png", channel.logoUrl)
        assertEquals("Movies", channel.groupTitle)
        assertEquals("Movies", channel.group)
        assertEquals("mpd", channel.manifestType)
        assertEquals("00112233445566778899aabbccddeeff:ffeeddccbbaa99887766554433221100", channel.drmLicenseKey)
        assertEquals("KinsFolkTest", channel.headers["User-Agent"])
        assertEquals("https://example.com", channel.headers["Referer"])
    }

    @Test
    fun parse_resetsMetadataBetweenChannels() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://cdn.example/logo.png" group-title="Movies",Movie Channel
            https://stream.example/movie.m3u8
            #EXTINF:-1,Plain Channel
            https://stream.example/plain.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(2, channels.size)
        assertEquals("Plain Channel", channels[1].name)
        assertNull(channels[1].logoUrl)
        assertNull(channels[1].groupTitle)
    }
}
