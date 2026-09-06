package com.gabestv.iptv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BrandLogoSize {
    COMPACT, // TopAppBar on Mobile
    MEDIUM,  // Drawer Header on TV
    LARGE    // Loading / Splash / About Screen
}

/**
 * Modern vector-rendered Brand Logo for GabesTV.
 * Scales dynamically across phone, tablet, and 4K TV screens with smooth anti-aliased Canvas rendering.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: BrandLogoSize = BrandLogoSize.MEDIUM,
    showSubtitle: Boolean = false
) {
    val iconSize: Dp = when (size) {
        BrandLogoSize.COMPACT -> 28.dp
        BrandLogoSize.MEDIUM -> 36.dp
        BrandLogoSize.LARGE -> 56.dp
    }

    val titleFontSize = when (size) {
        BrandLogoSize.COMPACT -> 18.sp
        BrandLogoSize.MEDIUM -> 22.sp
        BrandLogoSize.LARGE -> 32.sp
    }

    val subtitleFontSize = when (size) {
        BrandLogoSize.COMPACT -> 9.sp
        BrandLogoSize.MEDIUM -> 10.sp
        BrandLogoSize.LARGE -> 13.sp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing Canvas Icon
        Box(contentAlignment = Alignment.Center) {
            // Ambient Radial Glow
            Box(
                modifier = Modifier
                    .size(iconSize * 1.5f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C4DFF).copy(alpha = glowAlpha * 0.35f),
                                Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Vector Canvas Drawing of the GabesTV Emblem
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = this.size.width
                val h = this.size.height

                val gradientBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                // TV Screen Outer Rounded Rect
                val strokeWidth = w * 0.08f
                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(w * 0.08f, h * 0.12f),
                    size = Size(w * 0.84f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // TV Antenna Lines
                drawLine(
                    color = Color(0xFF7C4DFF),
                    start = Offset(w * 0.42f, h * 0.10f),
                    end = Offset(w * 0.30f, 0f),
                    strokeWidth = strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(w * 0.58f, h * 0.10f),
                    end = Offset(w * 0.70f, 0f),
                    strokeWidth = strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )

                // TV Stand Base
                drawLine(
                    color = Color(0xFF7C4DFF),
                    start = Offset(w * 0.42f, h * 0.82f),
                    end = Offset(w * 0.38f, h * 0.94f),
                    strokeWidth = strokeWidth * 0.75f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF7C4DFF),
                    start = Offset(w * 0.58f, h * 0.82f),
                    end = Offset(w * 0.62f, h * 0.94f),
                    strokeWidth = strokeWidth * 0.75f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF7C4DFF),
                    start = Offset(w * 0.34f, h * 0.94f),
                    end = Offset(w * 0.66f, h * 0.94f),
                    strokeWidth = strokeWidth * 0.75f,
                    cap = StrokeCap.Round
                )

                // Play Triangle Center
                val playPath = Path().apply {
                    moveTo(w * 0.44f, h * 0.35f)
                    lineTo(w * 0.64f, h * 0.46f)
                    lineTo(w * 0.44f, h * 0.57f)
                    close()
                }
                drawPath(
                    path = playPath,
                    brush = gradientBrush
                )

                // Tiny Live Pulse Dot
                drawCircle(
                    color = Color(0xFFFF2A55),
                    radius = w * 0.05f,
                    center = Offset(w * 0.82f, h * 0.22f)
                )
            }
        }

        Spacer(modifier = Modifier.width(iconSize * 0.3f))

        // Typography Header
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Gabes",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "TV",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF),
                    letterSpacing = (-0.5).sp
                )

                if (size != BrandLogoSize.COMPACT) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.25f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IPTV",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            if (showSubtitle) {
                Text(
                    text = "Ultra Fast Player",
                    fontSize = subtitleFontSize,
                    color = Color(0xFFA7A9BE),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
