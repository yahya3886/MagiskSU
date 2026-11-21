package com.magisksu.manager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magisksu.manager.BuildConfig
import com.magisksu.manager.data.SettingsStore
import com.magisksu.manager.data.ThemeMode
import com.magisksu.manager.root.MagiskCore
import com.magisksu.manager.root.MagiskStatus
import com.magisksu.manager.ui.components.ConfirmDialog
import com.magisksu.manager.ui.components.DataRow
import com.magisksu.manager.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPO_URL = "https://github.com/yahya3886/MagiskSU"

/**
 * Settings tab: theme controls (as in KernelSU), Magisk daemon toggles,
 * the log viewer entry point, reboot and the about section.
 */
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onOpenLogs: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by settingsStore.dynamicColor.collectAsState(initial = true)

    var status by remember { mutableStateOf<MagiskStatus?>(null) }
    var statusLoading by remember { mutableStateOf(true) }
    var confirmReboot by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        status = withContext(Dispatchers.IO) { MagiskCore.status() }
        statusLoading = false
    }

    val currentStatus = status
    val hasRoot = currentStatus?.hasRoot == true

    fun setMagiskSetting(key: String, value: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                MagiskCore.putSetting(key, if (value) 1 else 0)
                status = MagiskCore.status()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Appearance
        item {
            SectionCard(title = "Görünüm") {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "Sistem"
                            ThemeMode.LIGHT -> "Açık"
                            ThemeMode.DARK -> "Koyu"
                        }
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { scope.launch { settingsStore.setThemeMode(mode) } },
                            label = { Text(label) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Dinamik renkler",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Material You (Android 12+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = { value -> scope.launch { settingsStore.setDynamicColor(value) } },
                    )
                }
            }
        }

        // Root / Magisk
        item {
            SectionCard(title = "Root") {
                SettingSwitchRow(
                    title = "Zygisk",
                    subtitle = "Uygulama gizleme altyapısı. Değişiklik yeniden başlatma gerektirir.",
                    checked = currentStatus?.zygiskEnabled == true,
                    enabled = hasRoot && !statusLoading,
                    onCheckedChange = { setMagiskSetting("zygisk", it) },
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SettingSwitchRow(
                    title = "Denylist",
                    subtitle = "Seçili paketleri root'tan gizler. Yeniden başlatma önerilir.",
                    checked = currentStatus?.denylistEnabled == true,
                    enabled = hasRoot && !statusLoading,
                    onCheckedChange = { setMagiskSetting("denylist", it) },
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenLogs)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Log görüntüleyici",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Magisk logları ve su istekleri",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                when {
                    statusLoading -> DataRow(label = "Magisk", value = "Yükleniyor...")
                    else -> {
                        DataRow(label = "Magisk sürümü", value = currentStatus?.magiskVersion ?: "-")
                        DataRow(label = "Sürüm kodu", value = (currentStatus?.magiskVersionCode ?: 0).toString())
                        DataRow(label = "Çekirdek", value = currentStatus?.kernelVersion ?: "-")
                    }
                }

                Spacer(Modifier.size(10.dp))
                OutlinedButton(
                    onClick = { confirmReboot = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasRoot,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Cihazı yeniden başlat")
                }
            }
        }

        // About
        item {
            SectionCard(title = "Hakkında") {
                DataRow(label = "Uygulama", value = "MagiskSU ${BuildConfig.VERSION_NAME}")
                DataRow(label = "Taban", value = "Magisk")
                DataRow(label = "Arayüz", value = "KernelSU esinli")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)),
                                )
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "GitHub deposu",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                DataRow(label = "Lisans", value = "GPL-3.0")
            }
        }

        item {
            Text(
                text = "MagiskSU bağımsız bir topluluk projesidir; topjohnwu/Magisk veya tiann/KernelSU ile resmi bağlantısı yoktur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmReboot) {
        ConfirmDialog(
            title = "Cihaz yeniden başlatılsın mı?",
            text = "Yeniden başlatma, kök erişimini ve modül değişikliklerini etkinleştirir.",
            confirmLabel = "Yeniden başlat",
            onConfirm = {
                scope.launch { withContext(Dispatchers.IO) { MagiskCore.reboot() } }
            },
            onDismiss = { confirmReboot = false },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
