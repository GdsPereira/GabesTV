package com.gabestv.iptv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabestv.iptv.data.ChannelRepository
import com.gabestv.iptv.data.DownloadProgress
import com.gabestv.iptv.data.FavoritesDataStore
import com.gabestv.iptv.data.UpdateRepository
import com.gabestv.iptv.model.AppUpdateInfo
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Checking(val isManual: Boolean = false) : UpdateUiState
    data class Available(val info: AppUpdateInfo) : UpdateUiState
    data class Downloading(
        val info: AppUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState
    data class ReadyToInstall(val info: AppUpdateInfo, val apkFile: File) : UpdateUiState
    data class Error(val message: String, val info: AppUpdateInfo? = null) : UpdateUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val favoritesDataStore: FavoritesDataStore,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    companion object {
        const val FAVORITES_CATEGORY_ID = "__favorites__"
        const val ALL_CHANNELS_CATEGORY_ID = "__all__"
    }

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        // Load persisted favorites reactively from device-specific DataStore
        viewModelScope.launch {
            favoritesDataStore.favoriteChannelIds.collect { savedIds ->
                _uiState.update { current ->
                    if (current is MainUiState.Success) {
                        current.copy(favoriteChannelIds = savedIds)
                    } else current
                }
            }
        }
        loadPlaylist()
        checkForUpdates(isManual = false)
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) {
                _updateState.value = UpdateUiState.Checking(isManual = true)
            }
            updateRepository.checkForUpdate()
                .onSuccess { updateInfo ->
                    if (updateInfo != null) {
                        _updateState.value = UpdateUiState.Available(updateInfo)
                    } else {
                        if (isManual) {
                            _updateState.value = UpdateUiState.Idle
                        }
                    }
                }
                .onFailure { error ->
                    if (isManual) {
                        _updateState.value = UpdateUiState.Error(
                            error.localizedMessage ?: "Não foi possível verificar atualizações"
                        )
                    }
                }
        }
    }

    fun startUpdateDownload() {
        val current = _updateState.value
        val info = when (current) {
            is UpdateUiState.Available -> current.info
            is UpdateUiState.Error -> current.info ?: return
            else -> return
        }

        viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading(
                info = info,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = info.fileSizeBytes
            )

            updateRepository.downloadApk(info.apkUrl).collect { downloadProgress ->
                when (downloadProgress) {
                    is DownloadProgress.Progress -> {
                        _updateState.value = UpdateUiState.Downloading(
                            info = info,
                            progress = downloadProgress.percentage,
                            downloadedBytes = downloadProgress.downloadedBytes,
                            totalBytes = downloadProgress.totalBytes
                        )
                    }
                    is DownloadProgress.Completed -> {
                        _updateState.value = UpdateUiState.ReadyToInstall(
                            info = info,
                            apkFile = downloadProgress.apkFile
                        )
                        triggerInstall(downloadProgress.apkFile)
                    }
                    is DownloadProgress.Failed -> {
                        _updateState.value = UpdateUiState.Error(
                            message = downloadProgress.error.localizedMessage ?: "Falha ao baixar atualização",
                            info = info
                        )
                    }
                }
            }
        }
    }

    fun triggerInstall(apkFile: File) {
        updateRepository.triggerInstall(apkFile)
    }

    fun dismissUpdateDialog() {
        _updateState.value = UpdateUiState.Idle
    }

    fun canInstallPackages(): Boolean {
        return updateRepository.canRequestPackageInstalls()
    }

    fun getInstallPermissionIntent() = updateRepository.createInstallPermissionIntent()

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
        viewModelScope.launch {
            favoritesDataStore.toggleFavorite(channelId)
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
        _uiState.update { current ->
            if (current !is MainUiState.Success) return@update current
            val active = current.activePlayingChannel ?: return@update current
            val activeCategoryName = current.playlist.categories
                .find { it.id == current.selectedCategoryId }?.name
            val next = repository.getNextChannel(active, activeCategoryName)
            current.copy(activePlayingChannel = next)
        }
    }

    fun zapPrevious() {
        _uiState.update { current ->
            if (current !is MainUiState.Success) return@update current
            val active = current.activePlayingChannel ?: return@update current
            val activeCategoryName = current.playlist.categories
                .find { it.id == current.selectedCategoryId }?.name
            val prev = repository.getPreviousChannel(active, activeCategoryName)
            current.copy(activePlayingChannel = prev)
        }
    }
}
