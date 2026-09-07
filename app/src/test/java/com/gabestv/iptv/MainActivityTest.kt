package com.gabestv.iptv

import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Test
    fun `MainActivity creates and resumes on mobile phone configuration without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.okHttpClient = OkHttpClient()

        controller.create()
        assertThat(activity.isFinishing).isFalse()

        controller.start().resume()
        assertThat(activity.isFinishing).isFalse()

        activity.onWindowFocusChanged(true)
        assertThat(activity.isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun `MainActivity handles orientation configuration change on mobile without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.okHttpClient = OkHttpClient()

        controller.create().start().resume()

        val newConfig = Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        controller.configurationChange(newConfig)

        assertThat(activity.isFinishing).isFalse()
        controller.pause().stop().destroy()
    }

    @Test
    fun `MainActivity handles picture in picture mode transition without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.okHttpClient = OkHttpClient()

        controller.create().start().resume()

        activity.onPictureInPictureModeChanged(true, activity.resources.configuration)
        assertThat(activity.isInPipMode).isTrue()

        activity.onPictureInPictureModeChanged(false, activity.resources.configuration)
        assertThat(activity.isInPipMode).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun `MainActivity creates on Android TV configuration without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.okHttpClient = OkHttpClient()

        activity.resources.configuration.uiMode = Configuration.UI_MODE_TYPE_TELEVISION

        controller.create().start().resume()
        assertThat(activity.isFinishing).isFalse()

        activity.onWindowFocusChanged(true)
        assertThat(activity.isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun `FLAG_KEEP_SCREEN_ON can be applied and removed on activity window`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        activity.okHttpClient = OkHttpClient()

        controller.create().start().resume()

        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val flagsAfterAdd = activity.window.attributes.flags
        assertThat(flagsAfterAdd and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON).isNotEqualTo(0)

        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val flagsAfterClear = activity.window.attributes.flags
        assertThat(flagsAfterClear and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON).isEqualTo(0)

        controller.pause().stop().destroy()
    }
}

