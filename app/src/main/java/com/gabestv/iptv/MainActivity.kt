package com.gabestv.iptv

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.toImmutableList
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
import com.gabestv.iptv.ui.util.detectDeviceType
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

    var isInPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true

        val isTv = detectDeviceType(this, resources.configuration).isTv
        if (!isTv) {
            enableEdgeToEdge()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val targetMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                val params = window.attributes
                params.layoutInDisplayCutoutMode = targetMode
                window.attributes = params
            }
            hideSystemBars()
        }

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

                    // Keep screen awake continuously while app is in foreground
                    DisposableEffect(Unit) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose { }
                    }

                    // Auto-refresh playlist when app returns to foreground (e.g., categories added in backend)
                    DisposableEffect(this@MainActivity) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                viewModel.loadPlaylist()
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
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
                                    isInPipMode = isInPipMode,
                                    onZapNext = { viewModel.zapNext() },
                                    onZapPrevious = { viewModel.zapPrevious() },
                                    onClosePlayer = { viewModel.closePlayer() }
                                )
                            } else {
                                MainScreen(
                                    categories = uiState.playlist.categories.toImmutableList(),
                                    channels = uiState.playlist.channels.toImmutableList(),
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

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            hideSystemBars()
        }
    }

    /**
     * Hides system bars with transient swipe-to-reveal behavior on touch/mobile devices,
     * preserving native Leanback behavior on Android TV.
     */
    private fun hideSystemBars() {
        val isTv = detectDeviceType(this, resources.configuration).isTv
        if (!isTv) {
            try {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            } catch (_: Exception) {
                // Prevent crash on legacy devices / custom vendor insets implementations
            }
        }
    }
}
