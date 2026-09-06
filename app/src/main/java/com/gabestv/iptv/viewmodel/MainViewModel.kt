package com.gabestv.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabestv.iptv.data.ChannelRepository
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
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
        val activePlayingChannel: Channel? = null,
        val searchQuery: String = "",
        val favoriteChannelIds: Set<String> = emptySet(),
        val isListView: Boolean = false
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    companion object {
        const val FAVORITES_CATEGORY_ID = "__favorites__"
        const val ALL_CHANNELS_CATEGORY_ID = "__all__"
    }

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    fun loadPlaylist(remoteUrl: String? = null) {
        viewModelScope.launch {
            val currentSuccess = _uiState.value as? MainUiState.Success
            val previousCategory = currentSuccess?.selectedCategoryId
            val previousActiveChannel = currentSuccess?.activePlayingChannel
            val previousFavorites = currentSuccess?.favoriteChannelIds ?: emptySet()
            val previousIsList = currentSuccess?.isListView ?: false

            if (currentSuccess == null) {
                _uiState.value = MainUiState.Loading
            }
            repository.loadPlaylist(remoteUrl)
                .onSuccess { playlist ->
                    val defaultCategory = playlist.categories.firstOrNull()?.id
                    val effectiveCategory = if (previousCategory != null && (previousCategory == FAVORITES_CATEGORY_ID || previousCategory == ALL_CHANNELS_CATEGORY_ID || playlist.categories.any { it.id == previousCategory })) {
                        previousCategory
                    } else {
                        defaultCategory
                    }
                    val updatedActiveChannel = if (previousActiveChannel != null) {
                        playlist.channels.find { it.id == previousActiveChannel.id } ?: previousActiveChannel
                    } else null

                    _uiState.value = MainUiState.Success(
                        playlist = playlist,
                        selectedCategoryId = effectiveCategory,
                        activePlayingChannel = updatedActiveChannel,
                        favoriteChannelIds = previousFavorites,
                        isListView = previousIsList
                    )
                }
                .onFailure { error ->
                    if (currentSuccess == null) {
                        _uiState.value = MainUiState.Error(
                            error.localizedMessage ?: "Erro desconhecido ao carregar canais"
                        )
                    }
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

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                current.copy(searchQuery = query)
            } else current
        }
    }

    fun toggleFavorite(channelId: String) {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                val newFavorites = if (current.favoriteChannelIds.contains(channelId)) {
                    current.favoriteChannelIds - channelId
                } else {
                    current.favoriteChannelIds + channelId
                }
                current.copy(favoriteChannelIds = newFavorites)
            } else current
        }
    }

    fun toggleViewMode() {
        _uiState.update { current ->
            if (current is MainUiState.Success) {
                current.copy(isListView = !current.isListView)
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
        val activeCategoryName = current.playlist.categories.find { it.id == current.selectedCategoryId }?.name
        val next = repository.getNextChannel(active, activeCategoryName)
        _uiState.update { current.copy(activePlayingChannel = next) }
    }

    fun zapPrevious() {
        val current = _uiState.value as? MainUiState.Success ?: return
        val active = current.activePlayingChannel ?: return
        val activeCategoryName = current.playlist.categories.find { it.id == current.selectedCategoryId }?.name
        val prev = repository.getPreviousChannel(active, activeCategoryName)
        _uiState.update { current.copy(activePlayingChannel = prev) }
    }
}
