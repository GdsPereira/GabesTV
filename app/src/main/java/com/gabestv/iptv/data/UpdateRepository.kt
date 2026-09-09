package com.gabestv.iptv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.gabestv.iptv.AppConstants
import com.gabestv.iptv.BuildConfig
import com.gabestv.iptv.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadProgress {
    data class Progress(
        val percentage: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadProgress
    data class Completed(val apkFile: File) : DownloadProgress
    data class Failed(val error: Throwable) : DownloadProgress
}

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpdateRepository"
        const val PRIMARY_UPDATE_PATH = "/api/version.json"
        const val GITHUB_FALLBACK_URL =
            "https://github.com/GdsPereira/GabesTV/releases/latest/download/version.json"
        const val GITHUB_API_URL =
            "https://api.github.com/repos/GdsPereira/GabesTV/releases/latest"
    }

    /**
     * Retorna a URL base configurada para checagem de versão.
     */
    val primaryUpdateUrl: String
        get() = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}$PRIMARY_UPDATE_PATH"

    /**
     * Consulta se existe uma nova versão do GabesTV disponível.
     * Retorna [AppUpdateInfo] se a versão remota for superior à instalada, ou null se já estiver atualizado.
     */
    suspend fun checkForUpdate(targetUrl: String? = null): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        val urlsToTry = if (!targetUrl.isNullOrBlank()) {
            listOf(targetUrl)
        } else {
            listOf(primaryUpdateUrl, GITHUB_FALLBACK_URL, GITHUB_API_URL)
        }

        var lastException: Throwable? = null

        for (url in urlsToTry) {
            try {
                Log.d(TAG, "Verificando atualizações em: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", AppConstants.USER_AGENT)
                    .header("Cache-Control", "no-cache, no-store")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val updateInfo = parseUpdateInfo(responseBody, url)
                        if (updateInfo != null) {
                            val currentVersionCode = BuildConfig.VERSION_CODE
                            if (updateInfo.versionCode > currentVersionCode) {
                                Log.i(TAG, "Nova versão identificada! Atual: $currentVersionCode, Remota: ${updateInfo.versionCode} (${updateInfo.versionName})")
                                return@withContext Result.success(updateInfo)
                            } else {
                                Log.i(TAG, "Aplicativo já está na versão mais recente ($currentVersionCode).")
                                return@withContext Result.success(null)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Endpoint $url retornou código HTTP ${response.code}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao consultar atualizações em $url: ${e.message}")
                lastException = e
            }
        }

        if (lastException != null) {
            Result.failure(lastException)
        } else {
            Result.success(null)
        }
    }

    /**
     * Interpreta o payload JSON vindo de version.json ou da GitHub Releases API.
     */
    fun parseUpdateInfo(jsonString: String, sourceUrl: String): AppUpdateInfo? {
        return try {
            val json = JSONObject(jsonString)

            if (json.has("versionCode")) {
                // Formato padrão GabesTV version.json
                val versionCode = json.getInt("versionCode")
                val versionName = json.optString("versionName", "1.0.$versionCode")
                val apkUrl = json.getString("apkUrl")
                val releaseNotes = json.optString("releaseNotes", "")
                val isMandatory = json.optBoolean("isMandatory", false)
                val sha256 = if (json.has("sha256")) json.optString("sha256") else null
                val fileSizeBytes = json.optLong("fileSizeBytes", 0L)

                AppUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    apkUrl = apkUrl,
                    releaseNotes = releaseNotes,
                    isMandatory = isMandatory,
                    sha256 = sha256,
                    fileSizeBytes = fileSizeBytes
                )
            } else if (json.has("tag_name") && json.has("assets")) {
                // Formato GitHub Releases API fallback
                val tagName = json.getString("tag_name")
                val releaseNotes = json.optString("body", "")
                val assets = json.getJSONArray("assets")
                var apkUrl: String? = null
                var assetSize: Long = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true) && !name.contains("debug", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        assetSize = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkUrl == null && assets.length() > 0) {
                    val firstAsset = assets.getJSONObject(0)
                    apkUrl = firstAsset.optString("browser_download_url")
                    assetSize = firstAsset.optLong("size", 0L)
                }

                val cleanVersion = tagName.removePrefix("v")
                val code = parseVersionCodeFromTag(cleanVersion)

                if (apkUrl != null) {
                    AppUpdateInfo(
                        versionCode = code,
                        versionName = cleanVersion,
                        apkUrl = apkUrl,
                        releaseNotes = releaseNotes,
                        isMandatory = false,
                        fileSizeBytes = assetSize
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar JSON de atualização da origem $sourceUrl: ${e.message}", e)
            null
        }
    }

    private fun parseVersionCodeFromTag(version: String): Int {
        val parts = version.split(".").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
            2 -> parts[0] * 10000 + parts[1] * 100
            1 -> parts[0]
            else -> BuildConfig.VERSION_CODE
        }
    }

    /**
     * Realiza o streaming e download do APK em segundo plano, emitindo o progresso percentual.
     */
    fun downloadApk(apkUrl: String, destinationFile: File? = null): Flow<DownloadProgress> = flow {
        val targetFile = destinationFile ?: File(
            File(context.cacheDir, "updates").apply { mkdirs() },
            "GabesTV-update.apk"
        )

        try {
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", AppConstants.USER_AGENT)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadProgress.Failed(IOException("Falha HTTP ${response.code} ao baixar APK de $apkUrl")))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadProgress.Failed(IOException("Corpo de resposta vazio ao baixar APK")))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val percentage = if (totalBytes > 0) {
                            downloadedBytes.toFloat() / totalBytes
                        } else {
                            -1f // Progresso indeterminado
                        }

                        emit(DownloadProgress.Progress(percentage, downloadedBytes, totalBytes))
                    }
                    output.flush()
                }
            }

            emit(DownloadProgress.Completed(targetFile))
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante download do APK: ${e.message}", e)
            emit(DownloadProgress.Failed(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Verifica se o aplicativo possui autorização para instalar pacotes externos (Android 8.0+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Retorna um Intent para direcionar o usuário à tela de concessão de permissão de fontes desconhecidas.
     */
    fun createInstallPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Inicia a tela oficial de instalação do Android fornecendo o URI seguro via FileProvider.
     */
    fun triggerInstall(apkFile: File): Result<Unit> {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao disparar instalador de pacote para ${apkFile.absolutePath}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
