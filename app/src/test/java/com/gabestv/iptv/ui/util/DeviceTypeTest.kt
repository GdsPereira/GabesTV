@file:Suppress("DEPRECATION")
package com.gabestv.iptv.ui.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class DeviceTypeTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockUiModeManager: UiModeManager
    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockUiModeManager = mockk(relaxed = true)
        configuration = Configuration()

        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.getSystemService(Context.UI_MODE_SERVICE) } returns mockUiModeManager
    }

    @Test
    fun `DeviceType TV properties are verified`() {
        val type = DeviceType.TV
        assertThat(type.isTv).isTrue()
        assertThat(type.isTvDevice).isTrue()
        assertThat(type.isPhone).isFalse()
        assertThat(type.isTablet).isFalse()
        assertThat(type.isTouch).isFalse()
    }

    @Test
    fun `DeviceType PHONE properties are verified`() {
        val type = DeviceType.PHONE
        assertThat(type.isTv).isFalse()
        assertThat(type.isTvDevice).isFalse()
        assertThat(type.isPhone).isTrue()
        assertThat(type.isTablet).isFalse()
        assertThat(type.isTouch).isTrue()
    }

    @Test
    fun `DeviceType TABLET properties are verified`() {
        val type = DeviceType.TABLET
        assertThat(type.isTv).isFalse()
        assertThat(type.isTvDevice).isFalse()
        assertThat(type.isPhone).isFalse()
        assertThat(type.isTablet).isTrue()
        assertThat(type.isTouch).isTrue()
    }

    @Test
    fun `detectDeviceType returns TV when Leanback feature is present`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns true
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 360

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.TV)
        assertThat(result.isTv).isTrue()
        assertThat(result.isTouch).isFalse()
    }

    @Test
    fun `detectDeviceType returns TV when UI mode is TELEVISION`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_TELEVISION
        configuration.smallestScreenWidthDp = 360

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.TV)
        assertThat(result.isTv).isTrue()
    }

    @Test
    fun `detectDeviceType returns TV when deprecated television feature is present`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns true
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 360

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.TV)
        assertThat(result.isTv).isTrue()
    }

    @Test
    fun `detectDeviceType returns PHONE for touch device with smallestScreenWidthDp under 600`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 411

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.PHONE)
        assertThat(result.isPhone).isTrue()
        assertThat(result.isTouch).isTrue()
        assertThat(result.isTv).isFalse()
    }

    @Test
    fun `detectDeviceType returns TABLET for touch device with smallestScreenWidthDp of 600 or more`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 600

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.TABLET)
        assertThat(result.isTablet).isTrue()
        assertThat(result.isTouch).isTrue()
        assertThat(result.isTv).isFalse()
    }

    @Test
    fun `detectDeviceType returns TABLET even when hardware QWERTY keyboard is attached`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 800
        configuration.keyboard = Configuration.KEYBOARD_QWERTY
        configuration.hardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_NO

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.TABLET)
        assertThat(result.isTablet).isTrue()
        assertThat(result.isTouch).isTrue()
        assertThat(result.isTv).isFalse()
    }

    @Test
    fun `detectDeviceType returns PHONE even when hardware QWERTY keyboard is attached`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 390
        configuration.keyboard = Configuration.KEYBOARD_QWERTY

        val result = detectDeviceType(mockContext, configuration)
        assertThat(result).isEqualTo(DeviceType.PHONE)
        assertThat(result.isPhone).isTrue()
        assertThat(result.isTouch).isTrue()
        assertThat(result.isTv).isFalse()
    }

    @Test
    fun `detectDeviceType falls back to min of screen dimensions when smallestScreenWidthDp is undefined`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) } returns false
        every { mockUiModeManager.currentModeType } returns Configuration.UI_MODE_TYPE_NORMAL
        configuration.smallestScreenWidthDp = 0
        configuration.screenWidthDp = 1280
        configuration.screenHeightDp = 800

        val tabletResult = detectDeviceType(mockContext, configuration)
        assertThat(tabletResult).isEqualTo(DeviceType.TABLET)

        configuration.screenWidthDp = 390
        configuration.screenHeightDp = 844
        val phoneResult = detectDeviceType(mockContext, configuration)
        assertThat(phoneResult).isEqualTo(DeviceType.PHONE)
    }

    @Test
    fun `findActivity returns Activity directly when Context is Activity`() {
        val mockActivity: Activity = mockk()
        val found = mockActivity.findActivity()
        assertThat(found).isSameInstanceAs(mockActivity)
    }

    @Test
    fun `findActivity unwraps ContextWrapper chain to return root Activity`() {
        val mockActivity: Activity = mockk()
        val wrapper1: ContextWrapper = mockk {
            every { baseContext } returns mockActivity
        }
        val wrapper2: ContextWrapper = mockk {
            every { baseContext } returns wrapper1
        }

        val found = wrapper2.findActivity()
        assertThat(found).isSameInstanceAs(mockActivity)
    }

    @Test
    fun `findActivity returns null when Context is not an Activity`() {
        val nonActivityContext: Context = mockk()
        val found = nonActivityContext.findActivity()
        assertThat(found).isNull()
    }
}

