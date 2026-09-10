plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.gabestv.iptv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gabestv.iptv"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val defaultPlaylistUrl = (project.findProperty("GABESTV_PLAYLIST_URL") as? String)
            ?: System.getenv("GABESTV_PLAYLIST_URL")
            ?: "https://tv.gabesp.com.br/m3u/threadfin.m3u"
        val defaultBackendBaseUrl = (project.findProperty("GABESTV_BACKEND_BASE_URL") as? String)
            ?: System.getenv("GABESTV_BACKEND_BASE_URL")
            ?: "https://tv.gabesp.com.br"

        buildConfigField("String", "PLAYLIST_URL", "\"$defaultPlaylistUrl\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$defaultBackendBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            enableV1Signing = true
            enableV2Signing = true
            val customKeystore = System.getenv("KEYSTORE_FILE") ?: (project.findProperty("KEYSTORE_FILE") as? String)
            val bundledKeystore = file("keystore/gabestv-release.jks")

            if (!customKeystore.isNullOrEmpty() && file(customKeystore).exists()) {
                storeFile = file(customKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)
                keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("KEY_ALIAS") as? String)
                keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("KEY_PASSWORD") as? String)
            } else if (bundledKeystore.exists()) {
                storeFile = bundledKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "gabestv2026"
                keyAlias = System.getenv("KEY_ALIAS") ?: "gabestv"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "gabestv2026"
            } else {
                // Fallback to debug keystore for development if keystore is missing
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Android TV Jetpack Compose (androidx.tv.material3 & tv.foundation)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0-rc02")

    // Core Compose UI & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity & Lifecycle Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Media3 (ExoPlayer) - Core, HLS (.m3u8 live streaming), UI & OkHttp DataSource
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

    // OkHttp Client (Resilient streaming connection & custom IPTV User-Agent)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading for TV (Coil with SVG support for channel logos)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Dependency Injection - Hilt
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Immutable Collections for Compose stability
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    // DataStore Preferences for persistent favorites
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.robolectric:robolectric:4.12.2")
}
