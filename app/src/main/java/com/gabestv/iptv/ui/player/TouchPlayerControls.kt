package com.gabestv.iptv.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.ui.components.LiveBadge
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.ElectricCyan
import kotlinx.coroutines.delay

enum class VideoResizeMode {
    FIT,
    FILL,
    ZOOM
}

/**
 * Advanced Touch Controls Overlay for Smartphones & Tablets.
 * Supports:
 * - Single tap: toggle overlay visibility
 * - Left drag: Brightness adjustment
 * - Right drag: Volume adjustment
 * - Double tap: zap previous/next
 * - PiP (Picture-in-Picture)
 * - Resize aspect ratio toggle
 */
@Composable
fun TouchPlayerControls(
    channel: Channel,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onZapNext: () -> Unit,
    onZapPrevious: () -> Unit,
    onClosePlayer: () -> Unit,
    onToggleResizeMode: () -> Unit,
    resizeMode: VideoResizeMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var isControlsVisible by remember { mutableStateOf(true) }

    // Gesture indicator states
    var isAdjustingVolume by remember { mutableStateOf(false) }
    var currentVolumePercent by remember {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        mutableFloatStateOf((curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }

    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var currentBrightnessPercent by remember {
        val curBright = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableFloatStateOf(if (curBright < 0) 0.5f else curBright)
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(isControlsVisible, isPlaying, channel) {
        if (isControlsVisible) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Auto-dismiss gesture feedback
    LaunchedEffect(isAdjustingVolume) {
        if (isAdjustingVolume) {
            delay(1500)
            isAdjustingVolume = false
        }
    }
    LaunchedEffect(isAdjustingBrightness) {
        if (isAdjustingBrightness) {
            delay(1500)
            isAdjustingBrightness = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Touch gestures detector (tap, double tap, drag for brightness/volume)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isControlsVisible = !isControlsVisible
                    },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            onZapPrevious()
                        } else {
                            onZapNext()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            isAdjustingBrightness = true
                        } else {
                            isAdjustingVolume = true
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width
                        val height = size.height
                        val deltaPercent = -dragAmount.y / (height * 0.45f)

                        if (change.position.x < width / 2) {
                            // Left half: Brightness
                            isAdjustingBrightness = true
                            val newBright = (currentBrightnessPercent + deltaPercent).coerceIn(0.02f, 1.0f)
                            currentBrightnessPercent = newBright
                            activity?.let { act ->
                                val lp = act.window.attributes
                                lp.screenBrightness = newBright
                                act.window.attributes = lp
                            }
                        } else {
                            // Right half: Volume
                            isAdjustingVolume = true
                            val newVol = (currentVolumePercent + deltaPercent).coerceIn(0.0f, 1.0f)
                            currentVolumePercent = newVol
                            audioManager?.let { am ->
                                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val targetVol = (newVol * max).toInt()
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        }
                    },
                    onDragEnd = {
                        // Indicators will auto dismiss via LaunchedEffect
                    },
                    onDragCancel = {
                        isAdjustingBrightness = false
                        isAdjustingVolume = false
                    }
                )
            }
    ) {
        // Brightness HUD Indicator in Center Left
        AnimatedVisibility(
            visible = isAdjustingBrightness,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureStatusPill(
                icon = Icons.Default.BrightnessMedium,
                title = "Brilho",
                progress = currentBrightnessPercent
            )
        }

        // Volume HUD Indicator in Center Right
        AnimatedVisibility(
            visible = isAdjustingVolume,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val volIcon = when {
                currentVolumePercent <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                currentVolumePercent < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }
            GestureStatusPill(
                icon = volIcon,
                title = "Volume",
                progress = currentVolumePercent
            )
        }

        // Main Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClosePlayer,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiveBadge()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channel.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = channel.groupTitle,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }

                    // Resize Mode Button
                    IconButton(
                        onClick = onToggleResizeMode,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Proporção da tela: $resizeMode",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Picture-in-Picture Button (if supported)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(
                            onClick = {
                                activity?.let { act ->
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            act.enterPictureInPictureMode(
                                                android.app.PictureInPictureParams.Builder().build()
                                            )
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture-in-Picture",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Play / Previous / Next Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Previous Channel
                    IconButton(
                        onClick = onZapPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Canal Anterior",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause Main Button
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CyberPurple)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next Channel
                    IconButton(
                        onClick = onZapNext,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Próximo Canal",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Deslize nas bordas para Volume/Brilho • 2 Toques para Zapping",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureStatusPill(
    icon: ImageVector,
    title: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$title: ${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ElectricCyan,
                trackColor = Color(0xFF33334D)
            )
        }
    }
}
