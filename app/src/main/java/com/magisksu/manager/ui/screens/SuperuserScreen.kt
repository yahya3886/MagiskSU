package com.magisksu.manager.ui.screens

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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magisksu.manager.data.AppIcons
import com.magisksu.manager.data.SuperuserApp
import com.magisksu.manager.data.SuperuserRepository
import com.magisksu.manager.root.SuPolicyValue
import com.magisksu.manager.ui.components.AppIconHolder
import com.magisksu.manager.ui.components.ConfirmDialog
import com.magisksu.manager.ui.components.EmptyState
import com.magisksu.manager.ui.components.LoadingState
import com.magisksu.manager.ui.components.StatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Superuser tab: same list-based layout as KernelSU — app icon, label,
 * package name, policy chip and a switch that revokes or grants su access
 * directly in Magisk's `policies` table.
 */
@Composable
fun SuperuserScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var result by remember { mutableStateOf<SuperuserRepository.LoadResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var refreshToken by remember { mutableIntStateOf(0) }
    var confirmRevoke by remember { mutableStateOf<SuperuserApp?>(null) }
    var selectedApp by remember { mutableStateOf<SuperuserApp?>(null) }

    LaunchedEffect(refreshToken) {
        loading = true
        result = withContext(Dispatchers.IO) { SuperuserRepository.load(context) }
        loading = false
    }

    val apps = result?.apps ?: emptyList()
    val filtered = apps.filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Uygulama ara") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Temizle")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        when {
            loading -> LoadingState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            result == null || !result!!.rootAvailable -> EmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                icon = Icons.Rounded.Shield,
                title = "Root erişimi yok",
                subtitle = "Süper kullanıcı listesini görmek için cihazınızda Magisk kurulu olmalı ve bu uygulamaya su izni verilmiş olmalı.",
            )

            apps.isEmpty() -> EmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                icon = Icons.Rounded.Apps,
                title = "Henüz root izni yok",
                subtitle = "Bir uygulama kök erişimi istediğinde kararı buradan yönetebilirsiniz.",
            )

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.uid }) { app ->
                    SuperuserRow(
                        app = app,
                        onCheckedChange = { granted ->
                            if (granted) {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        SuperuserRepository.grant(app.uid)
                                    }
                                    if (ok) {
                                        result = result?.let { r ->
                                            r.copy(
                                                apps = r.apps.map {
                                                    if (it.uid == app.uid) {
                                                        it.copy(policy = SuPolicyValue.ALLOW)
                                                    } else {
                                                        it
                                                    }
                                                },
                                            )
                                        }
                                    } else {
                                        refreshToken++
                                    }
                                }
                            } else {
                                confirmRevoke = app
                            }
                        },
                        onClick = { selectedApp = app },
                    )
                }
            }
        }
    }

    selectedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text(app.label, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Paket: ${app.packageName}", style = MaterialTheme.typography.bodyMedium)
                    Text("UID: ${app.uid}", style = MaterialTheme.typography.bodyMedium)
                    Text("Karar: ${app.policyLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (app.isSystemApp) "Sistem uygulaması" else "Kullanıcı uygulaması",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedApp = null }) { Text("Tamam") }
            },
        )
    }

    confirmRevoke?.let { app ->
        ConfirmDialog(
            title = "Root izni kaldırılsın mı?",
            text = "${app.label} uygulamasının su erişimi kaldırılsın mı? Uygulama bir daha izin istediğinde yeniden sorulacak.",
            confirmLabel = "İzni kaldır",
            onConfirm = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { SuperuserRepository.revoke(app.uid) }
                    if (ok) {
                        result = result?.let { r ->
                            r.copy(apps = r.apps.filterNot { it.uid == app.uid })
                        }
                    } else {
                        refreshToken++
                    }
                }
            },
            onDismiss = { confirmRevoke = null },
        )
    }
}

@Composable
private fun SuperuserRow(
    app: SuperuserApp,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) { AppIcons.iconOf(context, app.packageName) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconHolder(
                bitmap = icon,
                fallback = Icons.Rounded.Apps,
                contentDescription = app.label,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.size(6.dp))
                StatusChip(
                    text = app.policyLabel,
                    container = if (app.granted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    content = if (app.granted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
            }
            Switch(
                checked = app.granted,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
