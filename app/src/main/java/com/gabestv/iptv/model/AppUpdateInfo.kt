package com.gabestv.iptv.model

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String = "",
    val isMandatory: Boolean = false,
    val sha256: String? = null,
    val fileSizeBytes: Long = 0L
)
