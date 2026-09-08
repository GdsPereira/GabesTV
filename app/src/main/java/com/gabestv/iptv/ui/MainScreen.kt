package com.gabestv.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.runtime.remember
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
import com.gabestv.iptv.ui.mobile.MobileMainScreen
import com.gabestv.iptv.ui.theme.DeepDarkBackground
import com.gabestv.iptv.ui.theme.GabesTVTheme
import com.gabestv.iptv.ui.util.LocalDeviceType
import com.gabestv.iptv.viewmodel.MainViewModel

@Composable
fun MainScreen(
    categories: ImmutableList<ChannelCategory>,
    channels: ImmutableList<Channel>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    favoriteChannelIds: Set<String> = emptySet(),
    isListView: Boolean = false,
    onSearchQueryChanged: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onToggleViewMode: () -> Unit = {},
    onRefreshPlaylist: () -> Unit = {}
) {
    val deviceType = LocalDeviceType.current

    if (!deviceType.isTv) {
        MobileMainScreen(
            categories = categories,
            channels = channels,
            selectedCategoryId = selectedCategoryId,
            searchQuery = searchQuery,
            favoriteChannelIds = favoriteChannelIds,
            isListView = isListView,
            onCategorySelected = onCategorySelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onToggleFavorite = onToggleFavorite,
            onToggleViewMode = onToggleViewMode,
            onRefreshPlaylist = onRefreshPlaylist,
            onChannelClick = onChannelClick,
            modifier = modifier
        )
        return
    }

    // Android TV / Tablet Split-Screen 10-foot UI
    val filteredChannels = remember(selectedCategoryId, channels, categories, favoriteChannelIds) {
        when (selectedCategoryId) {
            MainViewModel.FAVORITES_CATEGORY_ID -> {
                channels.filter { favoriteChannelIds.contains(it.id) }
            }
            MainViewModel.ALL_CHANNELS_CATEGORY_ID, null -> {
                channels
            }
            else -> {
                val cat = categories.find { it.id == selectedCategoryId }
                if (cat != null) channels.filter { it.groupTitle.equals(cat.name, ignoreCase = true) }
                else channels
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        // Left Side Navigation Drawer
        CategoryDrawer(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { category ->
                onCategorySelected(category.id)
            },
            favoriteCount = favoriteChannelIds.size,
            onSelectFavorites = {
                onCategorySelected(MainViewModel.FAVORITES_CATEGORY_ID)
            },
            onRefresh = onRefreshPlaylist
        )

        // Main Channel Grid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, start = 28.dp, end = 32.dp, bottom = 16.dp)
        ) {
            val currentCategoryName = when (selectedCategoryId) {
                MainViewModel.FAVORITES_CATEGORY_ID -> "Favoritos"
                MainViewModel.ALL_CHANNELS_CATEGORY_ID, null -> "Todos os Canais"
                else -> categories.find { it.id == selectedCategoryId }?.name ?: "Canais"
            }

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
                color = Color(0xFFA7A9BE)
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }, contentType = { "channel" }) { channel ->
                    ChannelCard(
                        channel = channel,
                        isFavorite = favoriteChannelIds.contains(channel.id),
                        onToggleFavorite = { onToggleFavorite(channel.id) },
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
            categories = sampleCategories.toImmutableList(),
            channels = sampleChannels.toImmutableList(),
            selectedCategoryId = "sports",
            onCategorySelected = {},
            onChannelClick = {}
        )
    }
}
