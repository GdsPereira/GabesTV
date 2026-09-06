package com.gabestv.iptv.data

import android.content.Context
import android.content.res.AssetManager
import com.gabestv.iptv.model.Channel
import com.gabestv.iptv.model.ChannelCategory
import com.gabestv.iptv.model.Playlist
import com.gabestv.iptv.parser.M3UParser
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

class ChannelRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager
    private lateinit var parser: M3UParser
    private lateinit var repository: ChannelRepository

    private val sampleLocalM3u = """
        #EXTM3U
        #EXTINF:-1 tvg-id="local.espn" group-title="Esportes",ESPN Local
        https://test.local/espn.m3u8
        #EXTINF:-1 tvg-id="local.news" group-title="Notícias",News Local
        https://test.local/news.m3u8
    """.trimIndent()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder().build()
        mockContext = mockk(relaxed = true)
        mockAssetManager = mockk(relaxed = true)

        every { mockContext.assets } returns mockAssetManager
        every { mockAssetManager.open("sample_channels.m3u") } answers {
            ByteArrayInputStream(sampleLocalM3u.toByteArray())
        }

        parser = M3UParser()
        repository = ChannelRepository(
            context = mockContext,
            parser = parser,
            okHttpClient = okHttpClient
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        unmockkStatic(Log::class)
    }

    @Test
    fun loadPlaylist_remoteSuccess_parsesAndSanitizesChannels() = runTest {
        val remoteM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ch1" group-title="Sports",Channel One
            http://localhost:34400/stream/ch1.m3u8
            #EXTINF:-1 tvg-id="ch2" group-title="News",Channel Two
            https://external.stream.com/live.m3u8
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(remoteM3u)
        )

        val targetUrl = mockWebServer.url("/playlist.m3u").toString()
        val result = repository.loadPlaylist(targetUrl)

        assertThat(result.isSuccess).isTrue()
        val playlist = result.getOrThrow()
        assertThat(playlist.channels).hasSize(2)
        assertThat(playlist.title).isEqualTo("GabesTV Cloud")

        // First channel should be rewritten using backend base URL
        val firstChannel = playlist.channels[0]
        assertThat(firstChannel.streamUrl).doesNotContain("localhost:34400")
        assertThat(firstChannel.streamUrl).contains("/stream/ch1.m3u8")

        // Second channel should keep external stream url intact
        val secondChannel = playlist.channels[1]
        assertThat(secondChannel.streamUrl).isEqualTo("https://external.stream.com/live.m3u8")

        // Cache should be updated
        assertThat(repository.getCachedPlaylist()).isEqualTo(playlist)
    }

    @Test
    fun loadPlaylist_remoteFailsWithHttpError_fallsBackToLocalAsset() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val targetUrl = mockWebServer.url("/error.m3u").toString()
        val result = repository.loadPlaylist(targetUrl)

        assertThat(result.isSuccess).isTrue()
        val playlist = result.getOrThrow()
        assertThat(playlist.title).isEqualTo("GabesTV Local")
        assertThat(playlist.channels).hasSize(2)
        assertThat(playlist.channels[0].name).isEqualTo("ESPN Local")
    }

    @Test
    fun loadPlaylist_remoteReturnsEmptyChannels_fallsBackToLocalAsset() = runTest {
        val emptyM3u = "#EXTM3U\n"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(emptyM3u)
        )

        val targetUrl = mockWebServer.url("/empty.m3u").toString()
        val result = repository.loadPlaylist(targetUrl)

        assertThat(result.isSuccess).isTrue()
        val playlist = result.getOrThrow()
        assertThat(playlist.title).isEqualTo("GabesTV Local")
        assertThat(playlist.channels).hasSize(2)
    }

    @Test
    fun loadPlaylist_bothRemoteAndLocalFail_returnsDescriptiveFailure() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )

        every { mockAssetManager.open("sample_channels.m3u") } throws IOException("Asset disk read error")

        val targetUrl = mockWebServer.url("/notfound.m3u").toString()
        val result = repository.loadPlaylist(targetUrl)

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isNotNull()
        assertThat(exception?.message).contains("Falha crítica")
        assertThat(exception?.message).contains("404")
    }

    @Test
    fun getNextChannel_and_getPreviousChannel_cycleWithinCategory() {
        val chSports1 = Channel(id = "1", name = "ESPN 1", streamUrl = "http://1", groupTitle = "Esportes")
        val chSports2 = Channel(id = "2", name = "ESPN 2", streamUrl = "http://2", groupTitle = "Esportes")
        val chSports3 = Channel(id = "3", name = "Fox Sports", streamUrl = "http://3", groupTitle = "Esportes")
        val chNews1 = Channel(id = "4", name = "CNN", streamUrl = "http://4", groupTitle = "Notícias")

        val testPlaylist = Playlist(
            title = "Test",
            channels = listOf(chSports1, chSports2, chSports3, chNews1),
            categories = listOf(
                ChannelCategory("cat_sports", "Esportes", 3),
                ChannelCategory("cat_news", "Notícias", 1)
            )
        )
        repository.setCachedPlaylistForTest(testPlaylist)

        // Forward cycling
        assertThat(repository.getNextChannel(chSports1).id).isEqualTo("2")
        assertThat(repository.getNextChannel(chSports2).id).isEqualTo("3")
        // Wrap around to first channel in category
        assertThat(repository.getNextChannel(chSports3).id).isEqualTo("1")

        // Backward cycling
        assertThat(repository.getPreviousChannel(chSports2).id).isEqualTo("1")
        // Wrap around from first to last channel in category
        assertThat(repository.getPreviousChannel(chSports1).id).isEqualTo("3")
    }

    @Test
    fun zapping_singleChannelInCategory_returnsSameChannel() {
        val chNews = Channel(id = "10", name = "BBC News", streamUrl = "http://10", groupTitle = "Notícias")
        val testPlaylist = Playlist(
            title = "Test Single",
            channels = listOf(chNews),
            categories = listOf(ChannelCategory("cat_news", "Notícias", 1))
        )
        repository.setCachedPlaylistForTest(testPlaylist)

        assertThat(repository.getNextChannel(chNews).id).isEqualTo("10")
        assertThat(repository.getPreviousChannel(chNews).id).isEqualTo("10")
    }

    @Test
    fun zapping_channelNotInCategoryList_returnsFirstChannelSafely() {
        val chSports1 = Channel(id = "1", name = "ESPN", streamUrl = "http://1", groupTitle = "Esportes")
        val chUnknown = Channel(id = "999", name = "Ghost", streamUrl = "http://ghost", groupTitle = "Esportes")

        val testPlaylist = Playlist(
            title = "Test Unknown",
            channels = listOf(chSports1),
            categories = listOf(ChannelCategory("cat_sports", "Esportes", 1))
        )
        repository.setCachedPlaylistForTest(testPlaylist)

        assertThat(repository.getNextChannel(chUnknown).id).isEqualTo("1")
        assertThat(repository.getPreviousChannel(chUnknown).id).isEqualTo("1")
    }

    @Test
    fun zapping_emptyPlaylist_returnsCurrentChannel() {
        val ch = Channel(id = "1", name = "ESPN", streamUrl = "http://1", groupTitle = "Esportes")
        repository.setCachedPlaylistForTest(null)

        assertThat(repository.getNextChannel(ch).id).isEqualTo("1")
        assertThat(repository.getPreviousChannel(ch).id).isEqualTo("1")
    }
}
