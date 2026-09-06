package com.gabestv.iptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.ElectricCyan
import com.gabestv.iptv.ui.theme.NeonRed
import com.gabestv.iptv.ui.theme.SurfaceDark
import com.gabestv.iptv.ui.theme.SurfaceVariantDark

/**
 * Modern Channel Card designed for both 10-foot TV viewing (D-Pad zoom & glow)
 * and mobile/tablet responsive touch interaction.
 */
@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val quality = extractChannelQuality(channel.name)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "cardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> CyberPurple
            else -> Color(0xFF26253B)
        },
        animationSpec = tween(durationMillis = 180),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) SurfaceVariantDark else SurfaceDark,
        animationSpec = tween(durationMillis = 180),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 18.dp else 3.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = CyberPurple
            )
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .aspectRatio(16f / 10.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Channel Logo Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C0B14)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = if (isFocused) ElectricCyan else Color(0xFF6B6E8C),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Badges Row (Top End: Category Tag / Quality Badge)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quality?.let {
                        QualityBadge(quality = it)
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = channel.groupTitle,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                }

                // Favorite Button (Top Start, if action provided)
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(28.dp)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) NeonRed else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Title
            Text(
                text = channel.name,
                fontSize = 13.5.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isFocused) Color.White else Color(0xFFF0F0F5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
