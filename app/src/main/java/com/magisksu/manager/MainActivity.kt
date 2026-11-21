package com.magisksu.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.magisksu.manager.data.SettingsStore
import com.magisksu.manager.ui.MagiskSUScaffold
import com.magisksu.manager.ui.theme.MagiskSUTheme

class MainActivity : ComponentActivity() {

    private val settingsStore by lazy { SettingsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsStore.themeMode.collectAsState(
                initial = com.magisksu.manager.data.ThemeMode.SYSTEM,
            )
            val dynamicColor by settingsStore.dynamicColor.collectAsState(initial = true)

            MagiskSUTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                MagiskSUScaffold(settingsStore = settingsStore)
            }
        }
    }
}
