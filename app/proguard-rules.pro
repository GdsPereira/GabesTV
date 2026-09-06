# ====================================================================
# GabesTV ProGuard / R8 Rules for Release Build
# ====================================================================

# Keep application data models and serializable classes
-keep class com.gabestv.iptv.model.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coil Image Loader (SVG & WebP)
-keep class coil.** { *; }
-dontwarn coil.**
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# Dagger & Hilt
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclassmembers,allowobfuscation class * {
    @javax.inject.Inject *;
    @dagger.Provides *;
}
-dontwarn dagger.hilt.**

# AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Jetpack Compose & TV Material3
-keep class androidx.compose.** { *; }
-keep class androidx.tv.** { *; }
