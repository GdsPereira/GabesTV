package com.gabestv.iptv.player

import android.content.Context
import com.gabestv.iptv.model.Channel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayerManagerTest {

    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var playerManager: PlayerManager

    private val sampleChannel = Channel(
        id = "test_1",
        name = "Test Channel",
        streamUrl = "https://example.com/live.m3u8",
        groupTitle = "Test"
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        playerManager = PlayerManager(
            context = context,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        playerManager.release()
    }

    @Test
    fun initialState_isIdle() {
        assertThat(playerManager.playerState.value).isEqualTo(PlayerState.Idle)
        assertThat(playerManager.isPlaying.value).isFalse()
    }

    @Test
    fun getPlayer_initializesExoPlayer() {
        val player = playerManager.getPlayer()
        assertThat(player).isNotNull()
    }

    @Test
    fun play_preparesAndSetsChannel() {
        playerManager.play(sampleChannel)
        val player = playerManager.getPlayer()
        assertThat(player).isNotNull()
        assertThat(player.mediaItemCount).isEqualTo(1)
    }

    @Test
    fun release_cleansUpPlayerResources() {
        playerManager.getPlayer()
        playerManager.release()
        val newPlayer = playerManager.getPlayer()
        assertThat(newPlayer).isNotNull()
        newPlayer.release()
    }
}
