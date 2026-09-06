package com.gabestv.iptv

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gabestv.iptv.ui.MainScreen
import com.gabestv.iptv.ui.components.BrandLogo
import com.gabestv.iptv.ui.components.BrandLogoSize
import com.gabestv.iptv.ui.player.PlayerScreen
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.DeepDarkBackground
import com.gabestv.iptv.ui.theme.GabesTVTheme
import com.gabestv.iptv.ui.util.LocalDeviceType
import com.gabestv.iptv.ui.util.rememberDeviceType
import com.gabestv.iptv.viewmodel.MainUiState
import com.gabestv.iptv.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val deviceType = rememberDeviceType()

            CompositionLocalProvider(LocalDeviceType provides deviceType) {
                GabesTVTheme {
                    val viewModel: MainViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsState()

                    // Handle dynamic screen orientation for TV vs Mobile
                    val activePlaying = (state as? MainUiState.Success)?.activePlayingChannel
                    LaunchedEffect(deviceType, activePlaying) {
                        requestedOrientation = if (deviceType.isTv) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            if (activePlaying != null) {
                                // In video player on phone: allow sensor landscape/rotation
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                // In browsing on phone: portrait / user orientation
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                    }

                    // Auto-refresh playlist when app returns to foreground (e.g., categories added in backend)
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                viewModel.loadPlaylist()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    when (val uiState = state) {
                        is MainUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DeepDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    BrandLogo(
                                        size = BrandLogoSize.LARGE,
                                        showSubtitle = true
                                    )
                                    Spacer(modifier = Modifier.height(36.dp))
                                    CircularProgressIndicator(
                                        color = CyberPurple,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Carregando canais do GabesTV…",
                                        color = Color(0xFFA7A9BE),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        is MainUiState.Success -> {
                            val activeChannel = uiState.activePlayingChannel
                            if (activeChannel != null) {
                                PlayerScreen(
                                    channel = activeChannel,
                                    okHttpClient = okHttpClient,
                                    onZapNext = { viewModel.zapNext() },
                                    onZapPrevious = { viewModel.zapPrevious() },
                                    onClosePlayer = { viewModel.closePlayer() }
                                )
                            } else {
                                MainScreen(
                                    categories = uiState.playlist.categories,
                                    channels = uiState.playlist.channels,
                                    selectedCategoryId = uiState.selectedCategoryId,
                                    searchQuery = uiState.searchQuery,
                                    favoriteChannelIds = uiState.favoriteChannelIds,
                                    isListView = uiState.isListView,
                                    onCategorySelected = { categoryId ->
                                        viewModel.selectCategory(categoryId)
                                    },
                                    onSearchQueryChanged = { query ->
                                        viewModel.setSearchQuery(query)
                                    },
                                    onToggleFavorite = { channelId ->
                                        viewModel.toggleFavorite(channelId)
                                    },
                                    onToggleViewMode = {
                                        viewModel.toggleViewMode()
                                    },
                                    onRefreshPlaylist = {
                                        viewModel.loadPlaylist()
                                    },
                                    onChannelClick = { channel ->
                                        viewModel.playChannel(channel)
                                    }
                                )
                            }
                        }

                        is MainUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DeepDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    BrandLogo(
                                        size = BrandLogoSize.MEDIUM,
                                        showSubtitle = true
                                    )
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Text(
                                        text = uiState.message,
                                        color = Color(0xFFFF5252),
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { viewModel.loadPlaylist() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberPurple
                                        )
                                    ) {
                                        Text("Tentar Novamente", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
