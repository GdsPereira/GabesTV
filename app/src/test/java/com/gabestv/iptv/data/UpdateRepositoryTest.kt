package com.gabestv.iptv.data

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UpdateRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var context: Context
    private lateinit var repository: UpdateRepository
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder().build()
        context = mockk(relaxed = true)

        tempDir = Files.createTempDirectory("gabestv_test_cache").toFile()
        every { context.cacheDir } returns tempDir
        every { context.packageName } returns "com.gabestv.iptv"

        repository = UpdateRepository(context, okHttpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        tempDir.deleteRecursively()
    }

    @Test
    fun parseUpdateInfo_validCustomVersionJson_parsesFieldsCorrectly() {
        val json = """
            {
                "versionCode": 10,
                "versionName": "1.2.0",
                "apkUrl": "https://tv.gabesp.com.br/download/app.apk",
                "releaseNotes": "Buffer aprimorado e novos canais esportivos.",
                "isMandatory": true,
                "fileSizeBytes": 2500000
            }
        """.trimIndent()

        val info = repository.parseUpdateInfo(json, "test_source")
        assertThat(info).isNotNull()
        assertThat(info!!.versionCode).isEqualTo(10)
        assertThat(info.versionName).isEqualTo("1.2.0")
        assertThat(info.apkUrl).isEqualTo("https://tv.gabesp.com.br/download/app.apk")
        assertThat(info.releaseNotes).contains("Buffer aprimorado")
        assertThat(info.isMandatory).isTrue()
        assertThat(info.fileSizeBytes).isEqualTo(2500000L)
    }

    @Test
    fun parseUpdateInfo_gitHubReleasesJson_parsesAssetsCorrectly() {
        val json = """
            {
                "tag_name": "v1.5.0",
                "body": "Novidades da versao 1.5",
                "assets": [
                    {
                        "name": "checksums.txt",
                        "browser_download_url": "https://github.com/checksums.txt",
                        "size": 120
                    },
                    {
                        "name": "GabesTV-AndroidTV-release.apk",
                        "browser_download_url": "https://github.com/download/GabesTV-AndroidTV-release.apk",
                        "size": 2600000
                    }
                ]
            }
        """.trimIndent()

        val info = repository.parseUpdateInfo(json, "github_source")
        assertThat(info).isNotNull()
        assertThat(info!!.versionName).isEqualTo("1.5.0")
        assertThat(info.apkUrl).isEqualTo("https://github.com/download/GabesTV-AndroidTV-release.apk")
        assertThat(info.releaseNotes).isEqualTo("Novidades da versao 1.5")
        assertThat(info.fileSizeBytes).isEqualTo(2600000L)
    }

    @Test
    fun parseUpdateInfo_malformedJson_returnsNull() {
        val json = "{ invalid json content"
        val info = repository.parseUpdateInfo(json, "invalid_source")
        assertThat(info).isNull()
    }

    @Test
    fun checkForUpdate_whenRemoteVersionIsHigher_returnsAppUpdateInfo() = runBlocking {
        val serverJson = """
            {
                "versionCode": 9999,
                "versionName": "9.9.9",
                "apkUrl": "${mockWebServer.url("/GabesTV.apk")}",
                "releaseNotes": "Atualizacao gigante"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(serverJson)
        )

        val targetUrl = mockWebServer.url("/version.json").toString()
        val result = repository.checkForUpdate(targetUrl)

        assertThat(result.isSuccess).isTrue()
        val updateInfo = result.getOrNull()
        assertThat(updateInfo).isNotNull()
        assertThat(updateInfo!!.versionCode).isEqualTo(9999)
        assertThat(updateInfo.versionName).isEqualTo("9.9.9")
    }

    @Test
    fun checkForUpdate_whenRemoteVersionIsEqualOrLower_returnsNull() = runBlocking {
        val serverJson = """
            {
                "versionCode": 1,
                "versionName": "1.0.0",
                "apkUrl": "${mockWebServer.url("/GabesTV.apk")}",
                "releaseNotes": "Versao antiga ou mesma"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(serverJson)
        )

        val targetUrl = mockWebServer.url("/version.json").toString()
        val result = repository.checkForUpdate(targetUrl)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun downloadApk_downloadsBytesAndEmitsCompleted() = runBlocking {
        val apkContent = "SIMULATED_APK_BINARY_BYTES_12345"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", apkContent.length)
                .setBody(apkContent)
        )

        val destinationFile = File(tempDir, "test_download.apk")
        val apkUrl = mockWebServer.url("/download/test.apk").toString()

        val progressList = repository.downloadApk(apkUrl, destinationFile).toList()

        assertThat(progressList).isNotEmpty()
        val lastEvent = progressList.last()
        assertThat(lastEvent).isInstanceOf(DownloadProgress.Completed::class.java)

        val completed = lastEvent as DownloadProgress.Completed
        assertThat(completed.apkFile.exists()).isTrue()
        assertThat(completed.apkFile.readText()).isEqualTo(apkContent)
    }
}
