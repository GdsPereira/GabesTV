package com.gabestv.iptv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.gabestv.iptv.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Buffering : PlayerState
    data object Ready : PlayerState
    data object Ended : PlayerState
    data class Error(val message: String, val isRetrying: Boolean) : PlayerState
}

/**
 * Media3 ExoPlayer Engine optimized for Threadfin IPTV middleware.
 * Supports both HLS (.m3u8) and MPEG-TS (.ts) infinite live streams.
 */
@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val okHttpClient: OkHttpClient = defaultOkHttpClient
) {
    private var exoPlayer: ExoPlayer? = null
    private var currentChannel: Channel? = null
    private var retryJob: Job? = null
    private var retryCount = 0

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    companion object {
        private const val MAX_RETRIES = 5
        private const val BASE_RETRY_DELAY_MS = 2000L

        val defaultOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        // Optimized LoadControl for fast channel zapping on TCL TVs (1.5s initial start buffer)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                playWhenReady = true
                addListener(playerListener)
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _playerState.value = PlayerState.Buffering
                Player.STATE_READY -> {
                    _playerState.value = PlayerState.Ready
                    retryCount = 0
                }
                Player.STATE_ENDED -> _playerState.value = PlayerState.Ended
                Player.STATE_IDLE -> _playerState.value = PlayerState.Idle
            }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackError(error)
        }
    }

    fun play(channel: Channel) {
        currentChannel = channel
        retryJob?.cancel()
        retryCount = 0

        val player = getPlayer()
        val mediaSource = buildMediaSource(channel)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    /**
     * Builds a MediaSource capable of playing both HLS (.m3u8) and MPEG-TS (.ts) from Threadfin.
     */
    private fun buildMediaSource(channel: Channel): MediaSource {
        val userAgent = channel.httpUserAgent ?: com.gabestv.iptv.AppConstants.USER_AGENT

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)

        if (!channel.httpReferrer.isNullOrBlank()) {
            httpDataSourceFactory.setDefaultRequestProperties(
                mapOf("Referer" to channel.httpReferrer)
            )
        }

        val streamUrl = channel.streamUrl.trim()
        val mimeType = when {
            streamUrl.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            streamUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            streamUrl.endsWith(".ts", ignoreCase = true) -> MimeTypes.VIDEO_MP2T
            streamUrl.contains("/stream/") -> MimeTypes.APPLICATION_M3U8 // Threadfin live streams
            else -> MimeTypes.APPLICATION_M3U8 // Default IPTV streams to HLS
        }

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMimeType(mimeType)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .build()
            )
            .build()

        // DefaultMediaSourceFactory creates HlsMediaSource or ProgressiveMediaSource based on mimeType
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
            .createMediaSource(mediaItem)
    }

    private fun handlePlaybackError(error: PlaybackException) {
        android.util.Log.e("GabesTV_Player", "Playback error for ${currentChannel?.name}: ${error.errorCodeName} (${error.errorCode})", error)

        // Check for HTTP errors like 404/410/403 (channel offline on remote upstream provider)
        var httpStatusCode: Int? = null
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                httpStatusCode = cause.responseCode
                break
            }
            cause = cause.cause
        }

        if (httpStatusCode != null && httpStatusCode in listOf(404, 410, 403, 502, 503)) {
            _playerState.value = PlayerState.Error(
                message = "Canal fora do ar no provedor original (HTTP $httpStatusCode). Use ◀ / ▶ para trocar de canal.",
                isRetrying = false
            )
            return
        }

        // Non-recoverable errors (decoding or invalid container format)
        val isUnrecoverable = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED

        if (isUnrecoverable) {
            _playerState.value = PlayerState.Error(
                message = "Formato de stream incompatível (${error.errorCodeName}). Troque de canal com ◀ / ▶.",
                isRetrying = false
            )
            return
        }

        if (retryCount < MAX_RETRIES && currentChannel != null) {
            retryCount++
            // Exponential backoff: 2s, 4s, 8s, 10s, 10s
            val delayMs = (BASE_RETRY_DELAY_MS * (1L shl (retryCount - 1))).coerceAtMost(10_000L)

            val statusDetail = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Falha de conexão"
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Erro no servidor de streaming"
                else -> "Instabilidade no stream"
            }

            _playerState.value = PlayerState.Error(
                message = "$statusDetail. Reconectando (${retryCount}/$MAX_RETRIES)…",
                isRetrying = true
            )

            retryJob?.cancel()
            retryJob = coroutineScope.launch(Dispatchers.Main) {
                delay(delayMs)
                currentChannel?.let { play(it) }
            }
        } else {
            _playerState.value = PlayerState.Error(
                message = "Stream indisponível (${error.errorCodeName}). Use ◀ / ▶ para trocar de canal.",
                isRetrying = false
            )
        }
    }

    fun retryCurrent() {
        currentChannel?.let {
            retryCount = 0
            play(it)
        }
    }

    fun release() {
        retryJob?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
