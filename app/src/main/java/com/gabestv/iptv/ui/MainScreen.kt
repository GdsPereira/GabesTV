package com.gabestv.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.ui.components.CategoryDrawer
import com.gabestv.iptv.ui.components.ChannelCard
import com.gabestv.iptv.ui.theme.GabesTVTheme

@Composable
fun MainScreen(
    categories: List<ChannelCategory>,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull()?.id)
    }

    val filteredChannels = remember(selectedCategoryId, channels) {
        if (selectedCategoryId == null) channels
        else {
            val cat = categories.find { it.id == selectedCategoryId }
            if (cat != null) channels.filter { it.groupTitle.equals(cat.name, ignoreCase = true) }
            else channels
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left Side Navigation Drawer
        CategoryDrawer(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { category ->
                selectedCategoryId = category.id
            }
        )

        // Main Channel Grid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, start = 24.dp, end = 32.dp, bottom = 16.dp)
        ) {
            val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "All Channels"

            Text(
                text = currentCategoryName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = "${filteredChannels.size} canais disponíveis",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Android TV 1080p Split Screen",
    device = Devices.TV_1080p,
    showBackground = true,
    backgroundColor = 0xFF0F0E17
)
@Composable
fun MainScreenPreview() {
    val sampleCategories = listOf(
        ChannelCategory(id = "sports", name = "Sports", channelCount = 14),
        ChannelCategory(id = "news", name = "News", channelCount = 8),
        ChannelCategory(id = "movies", name = "Movies & Series", channelCount = 32),
        ChannelCategory(id = "docs", name = "Documentaries", channelCount = 5)
    )

    val sampleChannels = listOf(
        Channel(id = "1", name = "ESPN HD Live", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "2", name = "Fox Sports 1", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "3", name = "TNT Sports 4K", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "4", name = "SportTV 1 Portugal", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "5", name = "NBA TV FHD", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "6", name = "Formula 1 4K Live", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "7", name = "Eurosport 1 HD", streamUrl = "http://...", groupTitle = "Sports"),
        Channel(id = "8", name = "Sky Sports Premier League", streamUrl = "http://...", groupTitle = "Sports")
    )

    GabesTVTheme {
        MainScreen(
            categories = sampleCategories,
            channels = sampleChannels,
            onChannelClick = {}
        )
    }
}
