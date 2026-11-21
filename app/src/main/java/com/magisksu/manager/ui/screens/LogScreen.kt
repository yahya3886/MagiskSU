package com.magisksu.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magisksu.manager.data.LogRepository
import com.magisksu.manager.ui.components.LoadingState
import com.magisksu.manager.ui.components.MonospaceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Log viewer (opened from Settings, like KernelSU): shows the Magisk daemon
 * log plus su requests captured in logcat. Text is selectable.
 */
@Composable
fun LogScreen() {
    val scope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var clearing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshToken) {
        logs = withContext(Dispatchers.IO) { LogRepository.load() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Magisk kayıtları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        clearing = true
                        withContext(Dispatchers.IO) { LogRepository.clear() }
                        clearing = false
                        refreshToken++
                    }
                },
                enabled = !clearing,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Temizle")
            }
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = { refreshToken++ }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Yenile")
            }
        }

        val content = logs
        if (content == null) {
            LoadingState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    MonospaceText(
                        text = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
}
