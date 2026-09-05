package com.gabestv.iptv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
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
    private val coroutineScope: CoroutineScope
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
        private const val DEFAULT_USER_AGENT = "GabesTV/1.0 (Android TV; TCL SmartTV; ExoPlayer)"
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

        player.stop()
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
        val userAgent = channel.httpUserAgent ?: DEFAULT_USER_AGENT

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)

        if (!channel.httpReferrer.isNullOrBlank()) {
            httpDataSourceFactory.setDefaultRequestProperties(
                mapOf("Referer" to channel.httpReferrer)
            )
        }

        val mediaItem = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .build()
            )
            .build()

        // DefaultMediaSourceFactory auto-detects HLS or TS stream container seamlessly
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
            .createMediaSource(mediaItem)
    }

    private fun handlePlaybackError(error: PlaybackException) {
        if (retryCount < MAX_RETRIES && currentChannel != null) {
            retryCount++
            val delayMs = BASE_RETRY_DELAY_MS * retryCount
            _playerState.value = PlayerState.Error(
                message = "Conexão instável. Reconectando (${retryCount}/$MAX_RETRIES)…",
                isRetrying = true
            )

            retryJob?.cancel()
            retryJob = coroutineScope.launch(Dispatchers.Main) {
                delay(delayMs)
                currentChannel?.let { play(it) }
            }
        } else {
            _playerState.value = PlayerState.Error(
                message = "Stream indisponível (HTTP ${error.errorCode}). Verifique o Threadfin.",
                isRetrying = false
            )
        }
    }

    fun release() {
        retryJob?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
