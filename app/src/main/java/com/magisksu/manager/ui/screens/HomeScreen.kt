package com.magisksu.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magisksu.manager.data.ModuleRepository
import com.magisksu.manager.root.MagiskCore
import com.magisksu.manager.root.MagiskStatus
import com.magisksu.manager.ui.MainTab
import com.magisksu.manager.ui.components.DataRow
import com.magisksu.manager.ui.components.SectionCard
import com.magisksu.manager.ui.components.StatCard
import com.magisksu.manager.ui.components.StatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home tab: same layout structure as KernelSU's landing screen — a hero status
 * card, superuser/module counters and device + Magisk information cards.
 */
@Composable
fun HomeScreen(onNavigate: (MainTab) -> Unit) {
    var status by remember { mutableStateOf<MagiskStatus?>(null) }
    var grantedApps by remember { mutableIntStateOf(0) }
    var enabledModules by remember { mutableIntStateOf(0) }
    var totalModules by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshToken) {
        val result = withContext(Dispatchers.IO) {
            val s = MagiskCore.status()
            val policies = MagiskCore.listPolicies()
            val modules = ModuleRepository.list()
            Triple(s, policies.count { it.granted }, Pair(modules.count { it.enabled }, modules.size))
        }
        status = result.first
        grantedApps = result.second
        enabledModules = result.third.first
        totalModules = result.third.second
        loading = false
    }

    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        )
        return
    }

    val currentStatus = status ?: MagiskStatus.empty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "MagiskSU",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Root yöneticisi • Magisk tabanlı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    StatusChip(
                        text = if (currentStatus.hasRoot) "Root aktif" else "Root yok",
                        container = MaterialTheme.colorScheme.surface,
                        content = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (!currentStatus.hasRoot) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = "Kök erişimi bulunamadı. Cihazınızda Magisk kurulu olmalı ve bu uygulamaya su izni verilmiş olmalı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        // Counters
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Superuser",
                    value = grantedApps.toString(),
                    subtitle = "izinli uygulama",
                    icon = Icons.Rounded.AdminPanelSettings,
                    onClick = { onNavigate(MainTab.SUPERUSER) },
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Modül",
                    value = "$enabledModules/$totalModules",
                    subtitle = "etkin modül",
                    icon = Icons.Rounded.Extension,
                    onClick = { onNavigate(MainTab.MODULE) },
                )
            }
        }

        // Device info
        item {
            SectionCard(title = "Sistem bilgisi") {
                DataRow(label = "Cihaz", value = currentStatus.deviceModel)
                DataRow(label = "Android", value = currentStatus.androidVersion)
                DataRow(label = "API seviyesi", value = currentStatus.apiLevel.toString())
                DataRow(label = "Çekirdek", value = currentStatus.kernelVersion)
            }
        }

        // Magisk info
        item {
            SectionCard(title = "Magisk durumu") {
                DataRow(label = "Sürüm", value = currentStatus.magiskVersion)
                DataRow(
                    label = "Sürüm kodu",
                    value = currentStatus.magiskVersionCode.toString().ifEmpty { "0" },
                )
                DataRow(label = "Yol", value = currentStatus.magiskPath)
                DataRow(label = "Zygisk", value = if (currentStatus.zygiskEnabled) "Açık" else "Kapalı")
                DataRow(label = "Denylist", value = if (currentStatus.denylistEnabled) "Açık" else "Kapalı")
            }
        }

        item {
            Text(
                text = "MagiskSU • GPLv3 • arayüz KernelSU esinlidir",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}
