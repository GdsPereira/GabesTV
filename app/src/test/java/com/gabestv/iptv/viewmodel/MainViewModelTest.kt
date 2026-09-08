package com.gabestv.iptv.viewmodel

import com.gabestv.iptv.data.ChannelRepository
import com.gabestv.iptv.data.FavoritesDataStore
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ChannelRepository
    private lateinit var favoritesDataStore: FavoritesDataStore
    private lateinit var viewModel: MainViewModel
    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())

    private val sportsChannel = Channel(id = "1", name = "ESPN HD", streamUrl = "http://espn", groupTitle = "Sports")
    private val newsChannel = Channel(id = "2", name = "CNN Live", streamUrl = "http://cnn", groupTitle = "News")
    private val moviesChannel = Channel(id = "3", name = "HBO Cinema", streamUrl = "http://hbo", groupTitle = "Movies")

    private val samplePlaylist = Playlist(
        title = "Test Playlist",
        channels = listOf(sportsChannel, newsChannel, moviesChannel),
        categories = listOf(
            ChannelCategory(id = "cat_sports", name = "Sports", channelCount = 1),
            ChannelCategory(id = "cat_news", name = "News", channelCount = 1),
            ChannelCategory(id = "cat_movies", name = "Movies", channelCount = 1)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.loadPlaylist(any()) } returns Result.success(samplePlaylist)

        favoritesFlow.value = emptySet()
        favoritesDataStore = mockk(relaxed = true)
        every { favoritesDataStore.favoriteChannelIds } returns favoritesFlow
        coEvery { favoritesDataStore.toggleFavorite(any()) } answers {
            val id = firstArg<String>()
            favoritesFlow.value = if (favoritesFlow.value.contains(id)) {
                favoritesFlow.value - id
            } else {
                favoritesFlow.value + id
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsPlaylistAndSetsFirstCategoryByDefault() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MainUiState.Success::class.java)

        val success = state as MainUiState.Success
        assertThat(success.playlist.channels).hasSize(3)
        assertThat(success.selectedCategoryId).isEqualTo("cat_sports")
        assertThat(success.activePlayingChannel).isNull()

        coVerify(exactly = 1) { repository.loadPlaylist(null) }
    }

    @Test
    fun selectCategory_updatesSelectedCategoryId() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.selectCategory("cat_news")

        val state = viewModel.uiState.value as MainUiState.Success
        assertThat(state.selectedCategoryId).isEqualTo("cat_news")
    }

    @Test
    fun playChannel_setsActivePlayingChannel() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.playChannel(newsChannel)

        val state = viewModel.uiState.value as MainUiState.Success
        assertThat(state.activePlayingChannel).isEqualTo(newsChannel)
    }

    @Test
    fun closePlayer_clearsActivePlayingChannelWhilePreservingCategory() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.selectCategory("cat_movies")
        viewModel.playChannel(moviesChannel)

        val playingState = viewModel.uiState.value as MainUiState.Success
        assertThat(playingState.activePlayingChannel).isEqualTo(moviesChannel)
        assertThat(playingState.selectedCategoryId).isEqualTo("cat_movies")

        viewModel.closePlayer()

        val closedState = viewModel.uiState.value as MainUiState.Success
        assertThat(closedState.activePlayingChannel).isNull()
        // Crucial test: Category selection must not be reset on closing player!
        assertThat(closedState.selectedCategoryId).isEqualTo("cat_movies")
    }

    @Test
    fun zapNext_and_zapPrevious_updatePlayingChannel() = runTest {
        every { repository.getNextChannel(sportsChannel, any()) } returns newsChannel
        every { repository.getPreviousChannel(newsChannel, any()) } returns sportsChannel

        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.playChannel(sportsChannel)

        viewModel.zapNext()
        val nextState = viewModel.uiState.value as MainUiState.Success
        assertThat(nextState.activePlayingChannel).isEqualTo(newsChannel)

        viewModel.zapPrevious()
        val prevState = viewModel.uiState.value as MainUiState.Success
        assertThat(prevState.activePlayingChannel).isEqualTo(sportsChannel)
    }

    @Test
    fun loadPlaylist_failure_emitsErrorState() = runTest {
        coEvery { repository.loadPlaylist(any()) } returns Result.failure(IOException("Falha de rede ao carregar canais"))

        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MainUiState.Error::class.java)
        val error = state as MainUiState.Error
        assertThat(error.message).contains("Falha de rede ao carregar canais")
    }

    @Test
    fun loadPlaylist_preservesSelectedCategoryAcrossReload() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.selectCategory("cat_movies")
        assertThat((viewModel.uiState.value as MainUiState.Success).selectedCategoryId).isEqualTo("cat_movies")

        // Reload playlist
        viewModel.loadPlaylist()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MainUiState.Success
        // Selected category must still be cat_movies because it exists in the reloaded playlist
        assertThat(state.selectedCategoryId).isEqualTo("cat_movies")
    }

    @Test
    fun setSearchQuery_updatesSearchState() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.setSearchQuery("ESPN")

        val state = viewModel.uiState.value as MainUiState.Success
        assertThat(state.searchQuery).isEqualTo("ESPN")
    }

    @Test
    fun toggleFavorite_addsAndRemovesChannelId() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        viewModel.toggleFavorite("channel_123")
        advanceUntilIdle()
        val stateWithFav = viewModel.uiState.value as MainUiState.Success
        assertThat(stateWithFav.favoriteChannelIds).contains("channel_123")

        viewModel.toggleFavorite("channel_123")
        advanceUntilIdle()
        val stateWithoutFav = viewModel.uiState.value as MainUiState.Success
        assertThat(stateWithoutFav.favoriteChannelIds).doesNotContain("channel_123")
    }

    @Test
    fun toggleViewMode_switchesBetweenGridAndList() = runTest {
        viewModel = MainViewModel(repository, favoritesDataStore)
        advanceUntilIdle()

        val initial = (viewModel.uiState.value as MainUiState.Success).isListView
        viewModel.toggleViewMode()
        val toggled = (viewModel.uiState.value as MainUiState.Success).isListView
        assertThat(toggled).isEqualTo(!initial)
    }
}
