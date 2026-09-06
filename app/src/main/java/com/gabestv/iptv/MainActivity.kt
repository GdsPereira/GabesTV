package com.gabestv.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gabestv.iptv.ui.MainScreen
import com.gabestv.iptv.ui.player.PlayerScreen
import com.gabestv.iptv.ui.theme.GabesTVTheme
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
            GabesTVTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                when (val uiState = state) {
                    is MainUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Carregando canais do GabesTV…",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    is MainUiState.Success -> {
                        val activeChannel = uiState.activePlayingChannel
                        if (activeChannel != null) {
                            // Fullscreen Video Player Mode with D-Pad Zapping
                            PlayerScreen(
                                channel = activeChannel,
                                okHttpClient = okHttpClient,
                                onZapNext = { viewModel.zapNext() },
                                onZapPrevious = { viewModel.zapPrevious() },
                                onClosePlayer = { viewModel.closePlayer() }
                            )
                        } else {
                            // Split-screen Browse Mode (Category Drawer + Channel Grid)
                            MainScreen(
                                categories = uiState.playlist.categories,
                                channels = uiState.playlist.channels,
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
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.message,
                                    color = Color(0xFFFF5252),
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadPlaylist() }
                                ) {
                                    Text("Tentar Novamente")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
