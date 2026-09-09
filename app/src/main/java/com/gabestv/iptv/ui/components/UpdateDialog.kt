package com.gabestv.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gabestv.iptv.BuildConfig
import com.gabestv.iptv.ui.theme.CyberPurple
import com.gabestv.iptv.ui.theme.CyberPurpleLight
import com.gabestv.iptv.ui.theme.DeepDarkBackground
import com.gabestv.iptv.ui.theme.ElectricCyan
import com.gabestv.iptv.ui.theme.NeonRed
import com.gabestv.iptv.ui.theme.SurfaceDark
import com.gabestv.iptv.ui.theme.SurfaceElevated
import com.gabestv.iptv.ui.theme.SurfaceVariantDark
import com.gabestv.iptv.ui.theme.TextDim
import com.gabestv.iptv.ui.theme.TextMuted
import com.gabestv.iptv.ui.util.LocalDeviceType
import com.gabestv.iptv.viewmodel.UpdateUiState
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onStartDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is UpdateUiState.Idle || state is UpdateUiState.Checking) return

    val deviceType = LocalDeviceType.current
    val isTv = deviceType.isTv
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (isTv && (state is UpdateUiState.Available || state is UpdateUiState.ReadyToInstall || state is UpdateUiState.Error)) {
            try {
                primaryFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore if component not yet attached to composition
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f)
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {}, // Intercept clicks outside
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(min = 320.dp, max = 560.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(
                    width = 1.dp,
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            when (state) {
                is UpdateUiState.Available -> {
                    AvailableUpdateContent(
                        info = state.info,
                        primaryFocusRequester = primaryFocusRequester,
                        onStartDownload = onStartDownload,
                        onDismiss = onDismiss
                    )
                }

                is UpdateUiState.Downloading -> {
                    DownloadingContent(
                        info = state.info,
                        progress = state.progress,
                        downloadedBytes = state.downloadedBytes,
                        totalBytes = state.totalBytes
                    )
                }

                is UpdateUiState.ReadyToInstall -> {
                    ReadyToInstallContent(
                        info = state.info,
                        primaryFocusRequester = primaryFocusRequester,
                        onInstall = { onInstall(state.apkFile) },
                        onDismiss = onDismiss
                    )
                }

                is UpdateUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        primaryFocusRequester = primaryFocusRequester,
                        onRetry = onRetry,
                        onDismiss = onDismiss
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun AvailableUpdateContent(
    info: com.gabestv.iptv.model.AppUpdateInfo,
    primaryFocusRequester: FocusRequester,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DialogHeaderIcon(
            icon = Icons.Default.SystemUpdate,
            iconTint = ElectricCyan,
            backgroundTint = ElectricCyan.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nova Atualização Disponível!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 13.sp,
                color = TextDim
            )
            Text(
                text = " ➔ ",
                fontSize = 13.sp,
                color = ElectricCyan
            )
            Text(
                text = "v${info.versionName}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElectricCyan
            )
        }

        if (info.releaseNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepDarkBackground)
                    .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text(
                        text = "O que há de novo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.releaseNotes,
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!info.isMandatory) {
                DialogButton(
                    text = "Depois",
                    isPrimary = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }

            DialogButton(
                text = "Atualizar Agora",
                isPrimary = true,
                focusRequester = primaryFocusRequester,
                onClick = onStartDownload,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DownloadingContent(
    info: com.gabestv.iptv.model.AppUpdateInfo,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DialogHeaderIcon(
            icon = Icons.Default.Download,
            iconTint = CyberPurpleLight,
            backgroundTint = CyberPurple.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Baixando GabesTV v${info.versionName}…",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        val progressText = if (progress >= 0f) {
            "${(progress * 100).toInt()}%"
        } else {
            "Preparando…"
        }

        val sizeText = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            formatBytes(downloadedBytes)
        }

        Text(
            text = "$progressText • $sizeText",
            fontSize = 13.sp,
            color = ElectricCyan,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (progress >= 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyberPurple,
                trackColor = SurfaceVariantDark
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyberPurple,
                trackColor = SurfaceVariantDark
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Por favor, aguarde enquanto o novo instalador é preparado.",
            fontSize = 12.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReadyToInstallContent(
    info: com.gabestv.iptv.model.AppUpdateInfo,
    primaryFocusRequester: FocusRequester,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DialogHeaderIcon(
            icon = Icons.Default.InstallMobile,
            iconTint = ElectricCyan,
            backgroundTint = ElectricCyan.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Download Concluído!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "O instalador do GabesTV v${info.versionName} está pronto. Clique abaixo para iniciar a instalação.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!info.isMandatory) {
                DialogButton(
                    text = "Fechar",
                    isPrimary = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }

            DialogButton(
                text = "Instalar Agora",
                isPrimary = true,
                focusRequester = primaryFocusRequester,
                onClick = onInstall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    primaryFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DialogHeaderIcon(
            icon = Icons.Default.ErrorOutline,
            iconTint = NeonRed,
            backgroundTint = NeonRed.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Falha na Atualização",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DialogButton(
                text = "Fechar",
                isPrimary = false,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )

            DialogButton(
                text = "Tentar Novamente",
                isPrimary = true,
                focusRequester = primaryFocusRequester,
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DialogHeaderIcon(
    icon: ImageVector,
    iconTint: Color,
    backgroundTint: Color
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundTint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun DialogButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> if (isPrimary) CyberPurpleLight else SurfaceElevated
        isPrimary -> CyberPurple
        else -> SurfaceVariantDark
    }

    val borderColor = if (isFocused) {
        ElectricCyan
    } else {
        Color.Transparent
    }

    val textColor = when {
        isFocused -> Color.White
        isPrimary -> Color.White
        else -> TextMuted
    }

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(if (isFocused) 2.dp else 0.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isPrimary || isFocused) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val cleanIndex = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, cleanIndex.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[cleanIndex])
}
