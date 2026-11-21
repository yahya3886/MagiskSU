package com.magisksu.manager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.magisksu.manager.data.SettingsStore
import com.magisksu.manager.ui.screens.HomeScreen
import com.magisksu.manager.ui.screens.LogScreen
import com.magisksu.manager.ui.screens.ModuleScreen
import com.magisksu.manager.ui.screens.SettingsScreen
import com.magisksu.manager.ui.screens.SuperuserScreen

/** Bottom navigation destinations, mirroring KernelSU's four tabs. */
enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Rounded.Home),
    SUPERUSER("Superuser", Icons.Rounded.AdminPanelSettings),
    MODULE("Modül", Icons.Rounded.Extension),
    SETTINGS("Ayarlar", Icons.Rounded.Settings),
}

/**
 * Root scaffold: app bar + bottom navigation + tab content.
 * The log viewer is opened from Settings (like KernelSU) and is handled here
 * so it can render its own back-enabled top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagiskSUScaffold(settingsStore: SettingsStore) {
    var tabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    var showLogs by rememberSaveable { mutableStateOf(false) }

    val tab = MainTab.entries.firstOrNull { it.name == tabName } ?: MainTab.HOME

    BackHandler(enabled = showLogs) { showLogs = false }

    Scaffold(
        topBar = {
            if (showLogs) {
                CenterAlignedTopAppBar(
                    title = { Text("Log görüntüleyici", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { showLogs = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("MagiskSU", fontWeight = FontWeight.SemiBold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                )
            }
        },
        bottomBar = {
            if (!showLogs) {
                NavigationBar {
                    MainTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tabName = item.name },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                showLogs -> LogScreen()
                tab == MainTab.HOME -> HomeScreen(onNavigate = { tabName = it.name })
                tab == MainTab.SUPERUSER -> SuperuserScreen()
                tab == MainTab.MODULE -> ModuleScreen()
                tab == MainTab.SETTINGS -> SettingsScreen(
                    settingsStore = settingsStore,
                    onOpenLogs = { showLogs = true },
                )
            }
        }
    }
}
