package com.gabestv.iptv.ui.player

import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import com.gabestv.iptv.ui.util.findActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.player.PlayerManager
import com.gabestv.iptv.player.PlayerState
import com.gabestv.iptv.ui.components.LiveBadge
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.util.LocalDeviceType
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onZapNext: () -> Unit,
    onZapPrevious: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    okHttpClient: OkHttpClient? = null,
    isInPipMode: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val deviceType = LocalDeviceType.current

    val playerManager = remember(okHttpClient) {
        PlayerManager(
            context = context,
            coroutineScope = coroutineScope,
            okHttpClient = okHttpClient ?: PlayerManager.defaultOkHttpClient
        )
    }

    val playerState by playerManager.playerState.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    var showTvHud by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(VideoResizeMode.FIT) }

    // Intercept hardware Back button to exit player (disabled in PiP so OS manages window)
    BackHandler(enabled = !isInPipMode) {
        onClosePlayer()
    }

    // Auto-hide TV HUD after 4 seconds of inactivity
    LaunchedEffect(showTvHud, channel) {
        if (showTvHud) {
            delay(4000)
            showTvHud = false
        }
    }

    // Start playing current channel and switch on zapping
    LaunchedEffect(channel) {
        showTvHud = true
        playerManager.play(channel)
    }

    // Keep screen turned on while playing video to prevent OS screen dimming or timeout
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    // Cleanup player when leaving screen
    DisposableEffect(playerManager) {
        onDispose {
            playerManager.release()
        }
    }

    // Auto focus for key event intercept (D-Pad on TV, Hardware keyboard on Tablet)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (deviceType.isTv) {
                        showTvHud = true
                    }
                    when (event.key) {
                        // Channel Zapping (Next / Previous)
                        Key.DirectionRight, Key.ChannelUp -> {
                            onZapNext()
                            true
                        }
                        Key.DirectionLeft, Key.ChannelDown -> {
                            onZapPrevious()
                            true
                        }
                        // Play / Pause toggle or retry (supports Enter, D-Pad Center, and Spacebar)
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (playerState is PlayerState.Error) {
                                playerManager.retryCurrent()
                            } else {
                                playerManager.togglePlayPause()
                            }
                            true
                        }
                        // Back to Grid
                        Key.Back, Key.Escape -> {
                            onClosePlayer()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            if (deviceType.isTv) {
                                showTvHud = !showTvHud
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Fullscreen ExoPlayer View with AspectRatio control
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.getPlayer()
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.keepScreenOn = true
                playerView.resizeMode = when (resizeMode) {
                    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering & Error Indicator (hidden in PiP)
        if (!isInPipMode) {
            when (val state = playerState) {
                is PlayerState.Buffering -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyberPurple,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                is PlayerState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (state.isRetrying) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(
                                    color = CyberPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                                androidx.compose.material3.Button(
                                    onClick = { playerManager.retryCurrent() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = CyberPurple
                                    )
                                ) {
                                    androidx.compose.material3.Text("Tentar Novamente", color = Color.White)
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // Overlay Controls: Touch-First on Phone/Tablet vs D-Pad HUD on TV (hidden in PiP)
        if (!isInPipMode) {
            if (deviceType.isTouch) {
                TouchPlayerControls(
                    channel = channel,
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        if (playerState is PlayerState.Error) {
                            playerManager.retryCurrent()
                        } else {
                            playerManager.togglePlayPause()
                        }
                    },
                    onZapNext = onZapNext,
                    onZapPrevious = onZapPrevious,
                    onClosePlayer = onClosePlayer,
                    onToggleResizeMode = {
                        resizeMode = when (resizeMode) {
                            VideoResizeMode.FIT -> VideoResizeMode.FILL
                            VideoResizeMode.FILL -> VideoResizeMode.ZOOM
                            VideoResizeMode.ZOOM -> VideoResizeMode.FIT
                        }
                    },
                    resizeMode = resizeMode
                )
            } else {
                // Android TV On-Screen Overlay (HUD)
                AnimatedVisibility(
                    visible = showTvHud,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                        // Top Header Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo or Fallback
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B1A2C)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!channel.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier.fillMaxSize().padding(6.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = CyberPurple,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LiveBadge()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = channel.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = channel.groupTitle,
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = if (isPlaying) "Reproduzindo" else "Pausado",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }

                        // Bottom D-Pad Controller Guide
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "◀ / ▶ Zapping de Canal",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "OK Pausar / Retomar",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "Voltar Sair do Player",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}


