package com.gabestv.iptv.model

/**
 * Represents a playable IPTV channel parsed from an M3U playlist.
 *
 * @property id Unique identifier for list keys and focus management in Compose for TV.
 * @property name Cleaned display name of the channel.
 * @property streamUrl Direct media stream URL (typically HLS .m3u8, MPEG-TS, or MP4).
 * @property logoUrl Remote image URL for the channel logo (`tvg-logo`).
 * @property groupTitle Category or genre of the channel (`group-title`).
 * @property tvgId EPG XMLTV identifier for future electronic program guide mapping (`tvg-id`).
 * @property tvgName EPG name identifier (`tvg-name`).
 * @property tvgChno Optional channel number (`tvg-chno`).
 * @property httpUserAgent Custom User-Agent header required by some IPTV providers (`#EXTVLCOPT:http-user-agent`).
 * @property httpReferrer Custom HTTP referrer required by some IPTV providers (`#EXTVLCOPT:http-referrer`).
 */
data class Channel(
    val id: String = "",
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = DEFAULT_GROUP,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgChno: String? = null,
    val httpUserAgent: String? = null,
    val httpReferrer: String? = null
) {
    companion object {
        const val DEFAULT_GROUP = "Uncategorized"

        /** Generates a deterministic ID based on unique channel data */
        fun generateId(streamUrl: String, name: String): String =
            "${streamUrl.hashCode()}_${name.hashCode()}"
    }
}

/**
 * Category grouping for the Leanback Navigation Drawer.
 *
 * @property id Normalized identifier (e.g. lowercase slug).
 * @property name Human-readable category title.
 * @property channelCount Number of channels in this category.
 */
data class ChannelCategory(
    val id: String,
    val name: String,
    val channelCount: Int = 0
)

/**
 * Aggregated playlist result containing all channels and extracted categories.
 */
data class Playlist(
    val title: String = "IPTV Playlist",
    val channels: List<Channel> = emptyList(),
    val categories: List<ChannelCategory> = emptyList()
)
