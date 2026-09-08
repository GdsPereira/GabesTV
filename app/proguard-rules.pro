# ====================================================================
# GabesTV ProGuard / R8 Rules for Release Build
# ====================================================================

# Keep application data models
-keep class com.gabestv.iptv.model.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp & Okio (suppress warnings — R8 bundles consumer rules)
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil SVG decoder
-dontwarn com.caverock.androidsvg.**

# Dagger & Hilt
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclassmembers,allowobfuscation class * {
    @javax.inject.Inject *;
    @dagger.Provides *;
}
-dontwarn dagger.hilt.**

# AndroidX Media3 / ExoPlayer (suppress warnings — consumer rules handle keeps)
-dontwarn androidx.media3.**

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
