package com.gabestv.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabestv.iptv.data.ChannelRepository
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val playlist: Playlist,
        val selectedCategoryId: String?,
        val activePlayingChannel: Channel? = null
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    fun loadPlaylist(remoteUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            repository.loadPlaylist(remoteUrl)
                .onSuccess { playlist ->
                    val defaultCategory = playlist.categories.firstOrNull()?.id
                    _uiState.value = MainUiState.Success(
                        playlist = playlist,
                        selectedCategoryId = defaultCategory
                    )
                }
                .onFailure { error ->
                    _uiState.value = MainUiState.Error(
                        error.localizedMessage ?: "Erro desconhecido ao carregar canais"
                    )
                }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                current.copy(selectedCategoryId = categoryId)
            } else current
        }
    }

    fun playChannel(channel: Channel) {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                current.copy(activePlayingChannel = channel)
            } else current
        }
    }

    fun closePlayer() {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                current.copy(activePlayingChannel = null)
            } else current
        }
    }

    fun zapNext() {
        val current = _uiState.value as? MainUiState.Success ?: return
        val active = current.activePlayingChannel ?: return
        val next = repository.getNextChannel(active)
        _uiState.update { current.copy(activePlayingChannel = next) }
    }

    fun zapPrevious() {
        val current = _uiState.value as? MainUiState.Success ?: return
        val active = current.activePlayingChannel ?: return
        val prev = repository.getPreviousChannel(active)
        _uiState.update { current.copy(activePlayingChannel = prev) }
    }
}
