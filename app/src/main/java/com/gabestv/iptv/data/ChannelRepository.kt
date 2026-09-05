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

    /**
     * Loads and parses the M3U playlist.
     * If a remote URL is provided, it fetches via OkHttp with streaming input.
     * Otherwise, falls back to the bundled local `sample_channels.m3u` in assets.
     */
    suspend fun loadPlaylist(remoteUrl: String? = null): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream: InputStream = if (!remoteUrl.isNullOrBlank()) {
                val request = Request.Builder()
                    .url(remoteUrl)
                    .header("User-Agent", "GabesTV/1.0 (Android TV; TCL SmartTV)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Failed to download playlist: HTTP ${response.code}")
                }
                response.body?.byteStream() ?: throw IllegalStateException("Empty response body")
            } else {
                context.assets.open("sample_channels.m3u")
            }

            inputStream.use { stream ->
                val playlist = parser.parsePlaylist(stream.reader(), "GabesTV Playlist")
                cachedPlaylist = playlist
                playlist
            }
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
