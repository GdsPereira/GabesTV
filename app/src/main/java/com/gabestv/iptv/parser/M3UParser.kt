package com.gabestv.iptv.parser

import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.util.UUID

/**
 * High-performance, memory-efficient M3U / M3U8 IPTV Playlist Parser.
 *
 * Designed for Android TV devices (e.g. TCL Google TVs) where low memory usage
 * and fast streaming execution are critical.
 *
 * Features:
 * - Line-by-line stream parsing with zero full-file buffering in memory.
 * - Handles malformed attribute quotes and commas inside attribute values.
 * - Extracts `tvg-logo`, `group-title`, `tvg-id`, `tvg-name`, `tvg-chno`.
 * - Supports IPTV directives like `#EXTGRP` and `#EXTVLCOPT:http-user-agent`.
 * - Exposes both synchronous list generation and reactive Coroutine Flow parsing.
 */
class M3UParser {

    companion object {
        private const val EXT_M3U = "#EXTM3U"
        private const val EXT_INF = "#EXTINF:"
        private const val EXT_GRP = "#EXTGRP:"
        private const val EXT_VLC_OPT = "#EXTVLCOPT:"

        // Regex for capturing key="value", key='value', or unquoted key=value
        private val ATTRIBUTE_REGEX = Regex("""([a-zA-Z0-9_-]+)=(?:"([^"]*)"|'([^']*)'|([^,\s]+))""")
    }

    /**
     * Parses an M3U playlist from a raw String.
     */
    fun parse(rawM3u: String): List<Channel> {
        return parse(StringReader(rawM3u))
    }

    /**
     * Parses an M3U playlist from an [InputStream].
     */
    fun parse(inputStream: InputStream): List<Channel> {
        return parse(InputStreamReader(inputStream, Charsets.UTF_8))
    }

    /**
     * Parses an M3U playlist line-by-line from a [Reader] to avoid OutOfMemory errors
     * on large IPTV playlists (50k+ channels).
     */
    fun parse(reader: Reader): List<Channel> {
        val channels = mutableListOf<Channel>()
        parseInternal(reader) { channel ->
            channels.add(channel)
        }
        return channels
    }

    /**
     * Parses an M3U playlist and returns a [Playlist] object containing channels
     * and aggregated, sorted [ChannelCategory] items for the TV navigation drawer.
     */
    fun parsePlaylist(reader: Reader, playlistTitle: String = "GabesTV Playlist"): Playlist {
        val channels = parse(reader)
        val categoryMap = linkedMapOf<String, Int>()

        for (channel in channels) {
            val group = channel.groupTitle.ifBlank { Channel.DEFAULT_GROUP }
            categoryMap[group] = (categoryMap[group] ?: 0) + 1
        }

        val categories = categoryMap.map { (name, count) ->
            ChannelCategory(
                id = name.lowercase().replace("\\s+".toRegex(), "_"),
                name = name,
                channelCount = count
            )
        }

        return Playlist(
            title = playlistTitle,
            channels = channels,
            categories = categories
        )
    }

    /**
     * Emits parsed [Channel] items reactively as a [Flow] on [Dispatchers.IO],
     * allowing the Android TV UI to display channels progressively while parsing.
     */
    fun parseAsFlow(reader: Reader): Flow<Channel> = flow {
        parseInternal(reader) { channel ->
            emit(channel)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Core line-by-line parsing loop.
     */
    private inline fun parseInternal(reader: Reader, onChannelParsed: (Channel) -> Unit) {
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)

        var pendingExtInf: ExtInfMetadata? = null
        var pendingGroupOverride: String? = null
        var pendingUserAgent: String? = null
        var pendingReferrer: String? = null

        bufferedReader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()

                if (line.isEmpty()) continue

                when {
                    line.startsWith(EXT_INF, ignoreCase = true) -> {
                        // If there was a previous uncommitted EXTINF without URL, discard or process
                        pendingExtInf = parseExtInfLine(line)
                        pendingGroupOverride = null
                        pendingUserAgent = null
                        pendingReferrer = null
                    }

                    line.startsWith(EXT_GRP, ignoreCase = true) -> {
                        pendingGroupOverride = line.substring(EXT_GRP.length).trim()
                    }

                    line.startsWith(EXT_VLC_OPT, ignoreCase = true) -> {
                        val opt = line.substring(EXT_VLC_OPT.length).trim()
                        when {
                            opt.startsWith("http-user-agent=", ignoreCase = true) -> {
                                pendingUserAgent = opt.substring("http-user-agent=".length).trim('"', '\'', ' ')
                            }
                            opt.startsWith("http-referrer=", ignoreCase = true) -> {
                                pendingReferrer = opt.substring("http-referrer=".length).trim('"', '\'', ' ')
                            }
                        }
                    }

                    line.startsWith("#") -> {
                        // Other directives or comments (e.g. #EXTM3U, #EXT-X-VERSION)
                        continue
                    }

                    else -> {
                        // Any non-empty, non-comment line is treated as the Stream URL
                        val currentExtInf = pendingExtInf
                        if (currentExtInf != null) {
                            val streamUrl = line
                            val group = pendingGroupOverride
                                ?: currentExtInf.attributes["group-title"]
                                ?: Channel.DEFAULT_GROUP

                            val channelName = when {
                                currentExtInf.channelName.isNotBlank() -> currentExtInf.channelName
                                !currentExtInf.attributes["tvg-name"].isNullOrBlank() -> currentExtInf.attributes["tvg-name"]!!
                                else -> "Channel ${UUID.randomUUID().toString().take(6)}"
                            }

                            val channel = Channel(
                                id = UUID.randomUUID().toString(),
                                name = channelName,
                                streamUrl = streamUrl,
                                logoUrl = currentExtInf.attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
                                groupTitle = group.trim().ifEmpty { Channel.DEFAULT_GROUP },
                                tvgId = currentExtInf.attributes["tvg-id"]?.takeIf { it.isNotBlank() },
                                tvgName = currentExtInf.attributes["tvg-name"]?.takeIf { it.isNotBlank() },
                                tvgChno = currentExtInf.attributes["tvg-chno"]?.takeIf { it.isNotBlank() },
                                httpUserAgent = pendingUserAgent,
                                httpReferrer = pendingReferrer
                            )

                            onChannelParsed(channel)

                            // Reset state for next channel
                            pendingExtInf = null
                            pendingGroupOverride = null
                            pendingUserAgent = null
                            pendingReferrer = null
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses an #EXTINF line by safely separating the attribute header from the channel title,
     * accounting for commas that may appear inside quoted attribute values (e.g. group-title="News, Sports").
     */
    private fun parseExtInfLine(line: String): ExtInfMetadata {
        // Strip the #EXTINF: prefix
        val content = line.substring(EXT_INF.length).trim()

        // Find the comma that separates attributes from channel name, ignoring commas inside quotes
        val commaIndex = findUnquotedCommaIndex(content)

        val metadataChunk: String
        val channelName: String

        if (commaIndex != -1) {
            metadataChunk = content.substring(0, commaIndex).trim()
            channelName = content.substring(commaIndex + 1).trim()
        } else {
            metadataChunk = content
            channelName = ""
        }

        val attributes = mutableMapOf<String, String>()
        for (match in ATTRIBUTE_REGEX.findAll(metadataChunk)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty {
                    match.groupValues[4]
                }
            }
            attributes[key] = value
        }

        return ExtInfMetadata(
            attributes = attributes,
            channelName = channelName
        )
    }

    /**
     * Scans through the string to locate the first comma that does not reside
     * inside a single or double quote.
     */
    private fun findUnquotedCommaIndex(text: String): Int {
        var inQuotes = false
        var quoteChar = '"'

        for (i in text.indices) {
            val c = text[i]
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true
                quoteChar = c
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false
            } else if (!inQuotes && c == ',') {
                return i
            }
        }

        return -1
    }

    private data class ExtInfMetadata(
        val attributes: Map<String, String>,
        val channelName: String
    )
}
