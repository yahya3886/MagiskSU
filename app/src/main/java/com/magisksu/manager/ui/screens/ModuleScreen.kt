package com.magisksu.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.magisksu.manager.data.ModuleInfo
import com.magisksu.manager.data.ModuleInstallResult
import com.magisksu.manager.data.ModuleRepository
import com.magisksu.manager.root.MagiskCore
import com.magisksu.manager.root.RootShell
import com.magisksu.manager.ui.components.ConfirmDialog
import com.magisksu.manager.ui.components.EmptyState
import com.magisksu.manager.ui.components.LoadingState
import com.magisksu.manager.ui.components.StatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Module tab: KernelSU-style list of /data/adb/modules with enable switches,
 * staged removal and a zip installer that stages into modules_update.
 */
@Composable
fun ModuleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var modules by remember { mutableStateOf<List<ModuleInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var rootAvailable by remember { mutableStateOf(true) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var pendingInstall by remember { mutableStateOf<Uri?>(null) }
    var installResult by remember { mutableStateOf<ModuleInstallResult?>(null) }
    var confirmRemove by remember { mutableStateOf<ModuleInfo?>(null) }
    var confirmReboot by remember { mutableStateOf(false) }

    val zipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingInstall = uri
    }

    LaunchedEffect(refreshToken) {
        loading = true
        val data = withContext(Dispatchers.IO) { ModuleRepository.list() }
        rootAvailable = RootShell.hasRoot()
        modules = data
        loading = false
    }

    LaunchedEffect(pendingInstall) {
        val uri = pendingInstall ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { ModuleRepository.install(context, uri) }
        installResult = result
        pendingInstall = null
        if (result.success) refreshToken++
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Modüller",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${modules.size} modül kuruldu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { zipLauncher.launch(arrayOf("*/*")) }) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Kur")
            }
        }

        when {
            loading -> LoadingState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            !rootAvailable -> EmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                icon = Icons.Rounded.Warning,
                title = "Root erişimi yok",
                subtitle = "Modülleri görmek için cihazınızda Magisk kurulu olmalı ve bu uygulamaya su izni verilmiş olmalı.",
            )

            modules.isEmpty() -> EmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                icon = Icons.Rounded.Extension,
                title = "Kurulu modül yok",
                subtitle = "Kur düğmesiyle bir Magisk modülü (zip) yükleyebilirsiniz.",
            )

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(modules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    ModuleRepository.setEnabled(module.id, enabled)
                                }
                                refreshToken++
                            }
                        },
                        onRemove = { confirmRemove = module },
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { confirmReboot = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
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

                item {
                    Text(
                        text = "Modüller /data/adb/modules dizininden okunur. Kaldırma işlemi, Magisk modülü bir sonraki açılışta siler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    installResult?.let { result ->
        AlertDialog(
            onDismissRequest = { installResult = null },
            title = {
                Text(
                    text = if (result.success) "Kurulum tamamlandı" else "Kurulum hatası",
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    text = buildString {
                        if (result.moduleName.isNotEmpty()) appendLine(result.moduleName)
                        append(result.message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { installResult = null }) { Text("Tamam") }
            },
        )
    }

    confirmRemove?.let { module ->
        ConfirmDialog(
            title = "Modül kaldırılsın mı?",
            text = "${module.name} modülü bir sonraki yeniden başlatmada kalıcı olarak silinecek.",
            confirmLabel = "Kaldır",
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { ModuleRepository.scheduleRemoval(module.id) }
                    refreshToken++
                }
            },
            onDismiss = { confirmRemove = null },
        )
    }

    if (confirmReboot) {
        ConfirmDialog(
            title = "Cihaz yeniden başlatılsın mı?",
            text = "Yeniden başlatma, root erişimini ve modül değişikliklerini etkinleştirir. İşlem geri alınamaz.",
            confirmLabel = "Yeniden başlat",
            onConfirm = {
                scope.launch { withContext(Dispatchers.IO) { MagiskCore.reboot() } }
            },
            onDismiss = { confirmReboot = false },
        )
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusChip(
                        text = module.statusLabel,
                        container = when {
                            module.pendingRemove -> MaterialTheme.colorScheme.errorContainer
                            !module.enabled -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        content = when {
                            module.pendingRemove -> MaterialTheme.colorScheme.onErrorContainer
                            !module.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
                if (module.description.isNotBlank()) {
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "${module.version} • ${module.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = module.enabled && !module.pendingRemove,
                    onCheckedChange = onCheckedChange,
                    enabled = !module.pendingRemove,
                )
                IconButton(onClick = onRemove, enabled = !module.pendingRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Kaldır",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
