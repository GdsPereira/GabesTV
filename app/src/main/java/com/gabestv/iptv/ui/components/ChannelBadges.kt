package com.gabestv.iptv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabestv.iptv.ui.theme.ElectricCyan
import com.gabestv.iptv.ui.theme.GoldAccent
import com.gabestv.iptv.ui.theme.NeonRed

/**
 * Animated "AO VIVO" Badge with glowing pulsing live dot.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NeonRed.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = dotAlpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "AO VIVO",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Resolution / Quality Tag (e.g. 4K, FHD, 60FPS) inferred from channel name.
 */
@Composable
fun QualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when {
        quality.contains("4K", ignoreCase = true) || quality.contains("UHD", ignoreCase = true) ->
            Triple(GoldAccent.copy(alpha = 0.18f), GoldAccent, GoldAccent.copy(alpha = 0.4f))
        quality.contains("FHD", ignoreCase = true) || quality.contains("1080", ignoreCase = true) ->
            Triple(ElectricCyan.copy(alpha = 0.15f), ElectricCyan, ElectricCyan.copy(alpha = 0.35f))
        else ->
            Triple(Color.White.copy(alpha = 0.12f), Color(0xFFD1D5DB), Color.White.copy(alpha = 0.2f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(0.7.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = quality,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Extracts quality tags (4K, FHD, 60FPS, etc.) from channel names.
 */
fun extractChannelQuality(name: String): String? {
    val upper = name.uppercase()
    return when {
        upper.contains("4K") || upper.contains("UHD") -> "4K"
        upper.contains("FHD") || upper.contains("1080") -> "FHD"
        upper.contains("HD") || upper.contains("720") -> "HD"
        else -> null
    }
}
