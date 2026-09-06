package com.gabestv.iptv.ui.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

enum class DeviceType {
    TV,
    PHONE,
    TABLET;

    val isTv: Boolean get() = this == TV
    val isPhone: Boolean get() = this == PHONE
    val isTablet: Boolean get() = this == TABLET
    val isTouch: Boolean get() = this != TV
}

val LocalDeviceType = staticCompositionLocalOf { DeviceType.TV }

/**
 * Detects whether the device is an Android TV / Google TV / Fire TV, a Smartphone, or a Tablet.
 */
@Composable
fun rememberDeviceType(): DeviceType {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        detectDeviceType(context, configuration)
    }
}

fun detectDeviceType(context: Context, configuration: Configuration): DeviceType {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTvUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    @Suppress("DEPRECATION")
    val hasTelevision = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)

    if (isTvUiMode || hasLeanback || hasTelevision) {
        return DeviceType.TV
    }

    val smallestWidth = configuration.smallestScreenWidthDp
    return if (smallestWidth >= 600) {
        DeviceType.TABLET
    } else {
        DeviceType.PHONE
    }
}
