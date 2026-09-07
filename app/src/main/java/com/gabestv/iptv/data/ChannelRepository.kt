package com.gabestv.iptv.data

import android.content.Context
import android.util.Log
import com.gabestv.iptv.AppConstants
import com.gabestv.iptv.BuildConfig
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import com.gabestv.iptv.parser.M3UParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: M3UParser,
    private val okHttpClient: OkHttpClient
) {
    private val mutex = Mutex()
    private var cachedPlaylist: Playlist? = null

    companion object {
        private const val TAG = "ChannelRepository"
        val DEFAULT_PLAYLIST_URL: String
            get() = BuildConfig.PLAYLIST_URL
        val BASE_BACKEND_URL: String
            get() = BuildConfig.BACKEND_BASE_URL
    }

    /**
     * Loads and parses the M3U playlist from the configured backend URL.
     * If the remote request fails or returns an empty list, gracefully falls back to bundled `sample_channels.m3u`.
     * Emits descriptive error messages if both remote and local fail.
     */
    suspend fun loadPlaylist(remoteUrl: String? = null): Result<Playlist> = withContext(Dispatchers.IO) {
        val targetUrl = if (!remoteUrl.isNullOrBlank()) remoteUrl else DEFAULT_PLAYLIST_URL

        var remoteError: Throwable? = null
        val parsedPlaylist: Playlist? = try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", AppConstants.USER_AGENT)
                .header("Cache-Control", "no-cache, no-store")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { stream ->
                    parser.parsePlaylist(stream.reader(), "GabesTV Cloud")
                }
            } else {
                remoteError = IOException("Falha HTTP ${response.code} ao obter playlist de $targetUrl")
                Log.w(TAG, remoteError.message ?: "Falha ao obter playlist remota")
                null
            }
        } catch (e: Exception) {
            remoteError = e
            Log.w(TAG, "Erro de rede ao carregar playlist de $targetUrl: ${e.localizedMessage}", e)
            null
        }

        if (parsedPlaylist != null && parsedPlaylist.channels.isNotEmpty()) {
            val sanitizedChannels = parsedPlaylist.channels.map { channel ->
                channel.copy(streamUrl = sanitizeStreamUrl(channel.streamUrl))
            }
            val finalPlaylist = parsedPlaylist.copy(channels = sanitizedChannels)
            mutex.withLock { cachedPlaylist = finalPlaylist }
            Result.success(finalPlaylist)
        } else {
            // Remote failed or returned 0 channels; execute local fallback
            Log.i(TAG, "Ativando fallback local para sample_channels.m3u (Causa remota: ${remoteError?.message ?: "Sem canais"})")
            try {
                val localPlaylist = context.assets.open("sample_channels.m3u").use { stream ->
                    parser.parsePlaylist(stream.reader(), "GabesTV Local")
                }
                if (localPlaylist.channels.isNotEmpty()) {
                    mutex.withLock { cachedPlaylist = localPlaylist }
                    Result.success(localPlaylist)
                } else {
                    val errorMsg = "Fallback local carregou 0 canais. Erro remoto original: ${remoteError?.message ?: "Nenhum canal encontrado"}"
                    Log.e(TAG, errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (assetException: Exception) {
                val failureMsg = "Falha crítica: Não foi possível carregar playlist remota (${remoteError?.message}) nem o fallback local (${assetException.message})"
                Log.e(TAG, failureMsg, assetException)
                Result.failure(IOException(failureMsg, assetException))
            }
        }
    }

    fun getCachedPlaylist(): Playlist? = synchronized(this) { cachedPlaylist }

    /**
     * For unit testing: directly seed the cached playlist.
     */
    internal fun setCachedPlaylistForTest(playlist: Playlist?) {
        synchronized(this) { cachedPlaylist = playlist }
    }

    /**
     * Finds the next channel in the specified category (or currentChannel's category)
     * for instant TV D-Pad zapping (Right / CH+).
     */
    fun getNextChannel(currentChannel: Channel, categoryName: String? = null): Channel = synchronized(this) {
        val list = cachedPlaylist?.channels ?: return currentChannel
        if (list.isEmpty()) return currentChannel

        val targetCategory = categoryName?.trim()?.takeIf { it.isNotEmpty() }
            ?: currentChannel.groupTitle.trim()

        val categoryChannels = list.filter {
            it.groupTitle.trim().equals(targetCategory, ignoreCase = true)
        }
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
     * Finds the previous channel in the specified category (or currentChannel's category)
     * for instant TV D-Pad zapping (Left / CH-).
     */
    fun getPreviousChannel(currentChannel: Channel, categoryName: String? = null): Channel = synchronized(this) {
        val list = cachedPlaylist?.channels ?: return currentChannel
        if (list.isEmpty()) return currentChannel

        val targetCategory = categoryName?.trim()?.takeIf { it.isNotEmpty() }
            ?: currentChannel.groupTitle.trim()

        val categoryChannels = list.filter {
            it.groupTitle.trim().equals(targetCategory, ignoreCase = true)
        }
        val activeList = if (categoryChannels.isNotEmpty()) categoryChannels else list
        val currentIndex = activeList.indexOfFirst { it.id == currentChannel.id }

        return if (currentIndex != -1) {
            val prevIndex = (currentIndex - 1 + activeList.size) % activeList.size
            activeList[prevIndex]
        } else {
            activeList.lastOrNull() ?: currentChannel
        }
    }

    /**
     * Sanitizes stream URLs by replacing localhost references with the production backend.
     * Uses URI parsing for robust host/port replacement.
     */
    private fun sanitizeStreamUrl(url: String): String {
        val trimmed = url.trim()
        val backendBase = BASE_BACKEND_URL.trimEnd('/')
        if (trimmed.contains("/stream/")) {
            val streamPath = trimmed.substring(trimmed.indexOf("/stream/"))
            return "$backendBase$streamPath"
        }
        return try {
            val uri = java.net.URI(trimmed)
            if (uri.host == "localhost" && uri.port == 34400) {
                val baseUri = java.net.URI(backendBase)
                java.net.URI(baseUri.scheme, null, baseUri.host, baseUri.port, uri.path, uri.query, null).toString()
            } else trimmed
        } catch (_: Exception) { trimmed }
    }
}

