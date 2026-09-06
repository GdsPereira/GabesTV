package com.gabestv.iptv.data

import android.content.Context
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import com.gabestv.iptv.parser.M3UParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: M3UParser,
    private val okHttpClient: OkHttpClient
) {
    private var cachedPlaylist: Playlist? = null

    companion object {
        const val DEFAULT_THREADFIN_URL = "https://tv.gabesp.com.br/m3u/threadfin.m3u"
    }

    /**
     * Loads and parses the M3U playlist from Threadfin cloud backend.
     * Falls back to bundled local `sample_channels.m3u` if remote is unavailable or empty.
     */
    suspend fun loadPlaylist(remoteUrl: String? = null): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = remoteUrl ?: DEFAULT_THREADFIN_URL

            val parsedPlaylist = runCatching {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "GabesTV/1.0 (Android TV; TCL SmartTV)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        parser.parsePlaylist(stream.reader(), "GabesTV Cloud")
                    }
                } else null
            }.getOrNull()

            val finalPlaylist = if (parsedPlaylist != null && parsedPlaylist.channels.isNotEmpty()) {
                val sanitizedChannels = parsedPlaylist.channels.map { channel ->
                    val fixedUrl = if (channel.streamUrl.contains("/stream/")) {
                        val streamPath = channel.streamUrl.substring(channel.streamUrl.indexOf("/stream/"))
                        "https://tv.gabesp.com.br$streamPath"
                    } else {
                        channel.streamUrl
                            .replace("http://167.126.15.40:34400", "https://tv.gabesp.com.br")
                            .replace("https://167.126.15.40:34400", "https://tv.gabesp.com.br")
                            .replace("http://localhost:34400", "https://tv.gabesp.com.br")
                    }
                    channel.copy(streamUrl = fixedUrl)
                }
                parsedPlaylist.copy(channels = sanitizedChannels)
            } else {
                context.assets.open("sample_channels.m3u").use { stream ->
                    parser.parsePlaylist(stream.reader(), "GabesTV Local")
                }
            }

            cachedPlaylist = finalPlaylist
            finalPlaylist
        }
    }

    fun getCachedPlaylist(): Playlist? = cachedPlaylist

    /**
     * Finds the next channel in the current category for instant TV D-Pad zapping (Right / CH+).
     */
    fun getNextChannel(currentChannel: Channel): Channel {
        val list = cachedPlaylist?.channels ?: return currentChannel
        val categoryChannels = list.filter { it.groupTitle.equals(currentChannel.groupTitle, ignoreCase = true) }
        val activeList = if (categoryChannels.isNotEmpty()) categoryChannels else list
        val currentIndex = activeList.indexOfFirst { it.id == currentChannel.id }

        return if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % activeList.size
            activeList[nextIndex]
        } else {
            activeList.firstOrNull() ?: currentChannel
        }
    }

    /**
     * Finds the previous channel in the current category for instant TV D-Pad zapping (Left / CH-).
     */
    fun getPreviousChannel(currentChannel: Channel): Channel {
        val list = cachedPlaylist?.channels ?: return currentChannel
        val categoryChannels = list.filter { it.groupTitle.equals(currentChannel.groupTitle, ignoreCase = true) }
        val activeList = if (categoryChannels.isNotEmpty()) categoryChannels else list
        val currentIndex = activeList.indexOfFirst { it.id == currentChannel.id }

        return if (currentIndex != -1) {
            val prevIndex = (currentIndex - 1 + activeList.size) % activeList.size
            activeList[prevIndex]
        } else {
            activeList.lastOrNull() ?: currentChannel
        }
    }
}
