package com.gabestv.iptv.parser

import com.gabestv.iptv.model.Channel
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class M3UParserTest {

    private lateinit var parser: M3UParser

    @Before
    fun setUp() {
        parser = M3UParser()
    }

    @Test
    fun parse_standardM3u_extractsAllFieldsCorrectly() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN HD" tvg-logo="https://example.com/cnn.png" group-title="News",CNN USA (1080p)
            https://stream.example.com/cnn/index.m3u8
        """.trimIndent()

        val channels = parser.parse(raw)

        assertThat(channels).hasSize(1)
        val channel = channels.first()
        assertThat(channel.name).isEqualTo("CNN USA (1080p)")
        assertThat(channel.groupTitle).isEqualTo("News")
        assertThat(channel.logoUrl).isEqualTo("https://example.com/cnn.png")
        assertThat(channel.tvgId).isEqualTo("cnn.us")
        assertThat(channel.tvgName).isEqualTo("CNN HD")
        assertThat(channel.streamUrl).isEqualTo("https://stream.example.com/cnn/index.m3u8")
    }

    @Test
    fun parse_groupTitleWithComma_doesNotSplitChannelNamePrematurely() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 tvg-id="bbc" group-title="News, Weather & Docs",BBC World News, Live HD
            http://stream.example.com/bbc.m3u8
        """.trimIndent()

        val channels = parser.parse(raw)

        assertThat(channels).hasSize(1)
        val channel = channels.first()
        assertThat(channel.groupTitle).isEqualTo("News, Weather & Docs")
        assertThat(channel.name).isEqualTo("BBC World News, Live HD")
        assertThat(channel.streamUrl).isEqualTo("http://stream.example.com/bbc.m3u8")
    }

    @Test
    fun parse_extGrpDirective_usedWhenGroupTitleIsMissing() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="http://example.com/espn.png",ESPN Sports
            #EXTGRP:Sports & Entertainment
            http://stream.example.com/espn.m3u8
        """.trimIndent()

        val channels = parser.parse(raw)

        assertThat(channels).hasSize(1)
        val channel = channels.first()
        assertThat(channel.groupTitle).isEqualTo("Sports & Entertainment")
        assertThat(channel.logoUrl).isEqualTo("http://example.com/espn.png")
        assertThat(channel.name).isEqualTo("ESPN Sports")
    }

    @Test
    fun parse_vlcHttpUserAgentDirective_attachesToChannel() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Premium Stream",Sky Cinema
            #EXTVLCOPT:http-user-agent=TiviMate/4.7.0
            #EXTVLCOPT:http-referrer=https://provider.tv
            http://stream.example.com/sky.m3u8
        """.trimIndent()

        val channels = parser.parse(raw)

        assertThat(channels).hasSize(1)
        val channel = channels.first()
        assertThat(channel.name).isEqualTo("Sky Cinema")
        assertThat(channel.httpUserAgent).isEqualTo("TiviMate/4.7.0")
        assertThat(channel.httpReferrer).isEqualTo("https://provider.tv")
    }

    @Test
    fun parsePlaylist_aggregatesCategoriesCorrectly() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 group-title="News",Channel 1
            http://example.com/1.m3u8
            #EXTINF:-1 group-title="Sports",Channel 2
            http://example.com/2.m3u8
            #EXTINF:-1 group-title="News",Channel 3
            http://example.com/3.m3u8
            #EXTINF:-1,Channel 4 Without Category
            http://example.com/4.m3u8
        """.trimIndent()

        val playlist = parser.parsePlaylist(raw.reader(), "Test Playlist")

        assertThat(playlist.channels).hasSize(4)
        assertThat(playlist.categories).hasSize(3)

        val newsCategory = playlist.categories.find { it.name == "News" }
        val sportsCategory = playlist.categories.find { it.name == "Sports" }
        val uncatCategory = playlist.categories.find { it.name == Channel.DEFAULT_GROUP }

        assertThat(newsCategory?.channelCount).isEqualTo(2)
        assertThat(sportsCategory?.channelCount).isEqualTo(1)
        assertThat(uncatCategory?.channelCount).isEqualTo(1)
    }
}
