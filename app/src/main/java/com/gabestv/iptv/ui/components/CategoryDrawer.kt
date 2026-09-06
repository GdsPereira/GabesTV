package com.gabestv.iptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.GoldAccent
import com.gabestv.iptv.ui.theme.SurfaceDark
import com.gabestv.iptv.ui.theme.SurfaceVariantDark
import com.gabestv.iptv.viewmodel.MainViewModel

@Composable
fun CategoryDrawer(
    categories: List<ChannelCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (ChannelCategory) -> Unit,
    modifier: Modifier = Modifier,
    favoriteCount: Int = 0,
    onSelectFavorites: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(270.dp)
            .background(SurfaceDark)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        // App Title / New Brand Logo
        BrandLogo(
            size = BrandLogoSize.MEDIUM,
            showSubtitle = true,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "MENU & CATEGORIAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8E90A6),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Favorites Option on TV
            if (favoriteCount > 0 && onSelectFavorites != null) {
                item {
                    CategoryDrawerItemRow(
                        name = "Favoritos",
                        channelCount = favoriteCount,
                        isSelected = selectedCategoryId == MainViewModel.FAVORITES_CATEGORY_ID,
                        icon = Icons.Default.Star,
                        accentColor = GoldAccent,
                        onClick = onSelectFavorites
                    )
                }
            }

            items(categories, key = { it.id }) { category ->
                CategoryDrawerItemRow(
                    name = category.name,
                    channelCount = category.channelCount,
                    isSelected = category.id == selectedCategoryId,
                    icon = Icons.Default.Category,
                    accentColor = CyberPurple,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryDrawerItemRow(
    name: String,
    channelCount: Int,
    isSelected: Boolean,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> CyberPurple
            isSelected -> SurfaceVariantDark
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "itemBg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> accentColor
            else -> Color(0xFFDCDCE6)
        },
        animationSpec = tween(150),
        label = "textColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.White else accentColor,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = name,
                color = textColor,
                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.5.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$channelCount",
                fontSize = 11.sp,
                color = if (isFocused) Color.White else Color(0xFFA7A9BE)
            )
        }
    }
}
