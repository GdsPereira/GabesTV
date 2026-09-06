package com.gabestv.iptv.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.ui.components.BrandLogo
import com.gabestv.iptv.ui.components.BrandLogoSize
import com.gabestv.iptv.ui.components.ChannelCard
import com.gabestv.iptv.ui.components.ChannelListItem
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.DeepDarkBackground
import com.gabestv.iptv.ui.theme.ElectricCyan
import com.gabestv.iptv.ui.theme.GoldAccent
import com.gabestv.iptv.ui.theme.SurfaceDark
import com.gabestv.iptv.viewmodel.MainViewModel

/**
 * Mobile-first Main Screen optimized for touch gestures, vertical scrolling,
 * instant search, favorite filters, and list/grid layout toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileMainScreen(
    categories: List<ChannelCategory>,
    channels: List<Channel>,
    selectedCategoryId: String?,
    searchQuery: String,
    favoriteChannelIds: Set<String>,
    isListView: Boolean,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onRefreshPlaylist: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }

    // Filter channels based on selected category, favorites, and search query
    val filteredChannels = remember(channels, selectedCategoryId, searchQuery, favoriteChannelIds, categories) {
        var list = when (selectedCategoryId) {
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

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.groupTitle.lowercase().contains(q)
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchExpanded) {
                        BrandLogo(size = BrandLogoSize.COMPACT)
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text("Buscar canais...", color = Color.Gray, fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color(0xFF33324D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca", tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) {
                            onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar canais",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onToggleViewMode) {
                        Icon(
                            imageVector = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Alternar modo de exibição",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onRefreshPlaylist) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar playlist",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepDarkBackground
                )
            )
        },
        containerColor = DeepDarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Chips (Horizontal Carousel)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Todos" Chip
                item {
                    val isAllSelected = selectedCategoryId == null || selectedCategoryId == MainViewModel.ALL_CHANNELS_CATEGORY_ID
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { onCategorySelected(MainViewModel.ALL_CHANNELS_CATEGORY_ID) },
                        label = {
                            Text(
                                text = "Todos (${channels.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberPurple,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = Color(0xFFA7A9BE)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAllSelected,
                            borderColor = if (isAllSelected) CyberPurple else Color(0xFF26253B)
                        )
                    )
                }

                // "⭐ Favoritos" Chip
                item {
                    val isFavSelected = selectedCategoryId == MainViewModel.FAVORITES_CATEGORY_ID
                    FilterChip(
                        selected = isFavSelected,
                        onClick = { onCategorySelected(MainViewModel.FAVORITES_CATEGORY_ID) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isFavSelected) GoldAccent else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Favoritos (${favoriteChannelIds.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberPurple,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = Color(0xFFA7A9BE)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isFavSelected,
                            borderColor = if (isFavSelected) GoldAccent else Color(0xFF26253B)
                        )
                    )
                }

                // Category items
                items(categories, key = { it.id }) { category ->
                    val isSelected = category.id == selectedCategoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(category.id) },
                        label = {
                            Text(
                                text = "${category.name} (${category.channelCount})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberPurple,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = Color(0xFFA7A9BE)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CyberPurple else Color(0xFF26253B)
                        )
                    )
                }
            }

            // Results count info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredChannels.size} canais disponíveis",
                    fontSize = 12.sp,
                    color = Color(0xFF8E90A6)
                )

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Filtro: \"$searchQuery\"",
                        fontSize = 12.sp,
                        color = ElectricCyan
                    )
                }
            }

            // Channel Content List / Grid
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedCategoryId == MainViewModel.FAVORITES_CATEGORY_ID)
                                "Nenhum canal favoritado ainda.\nToque no ícone de coração em qualquer canal para salvá-lo aqui!"
                            else
                                "Nenhum canal encontrado para a busca.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else if (isListView) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        ChannelListItem(
                            channel = channel,
                            isFavorite = favoriteChannelIds.contains(channel.id),
                            onClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = favoriteChannelIds.contains(channel.id),
                            onClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) }
                        )
                    }
                }
            }
        }
    }
}
