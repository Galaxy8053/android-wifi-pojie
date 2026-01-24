package com.wifi.toolbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.ui.screen.pojie.*
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.items.pojie.ResourceSelectSheet
import com.wifi.toolbox.ui.items.pojie.RunListView
import com.wifi.toolbox.ui.items.pojie.ScanResult
import com.wifi.toolbox.ui.items.pojie.ScreenState
import com.wifi.toolbox.utils.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wifi.toolbox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PojieScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as? ToolboxApp }

    var pojieSettings by rememberPojieSettings(context)

    val controller = if (app != null) {
        rememberPojieWifiController(context, app, pojieSettings)
    } else {
        remember {
            object : PojieWifiController {
                override val uiState = ScreenState.Success(true)
                override val isScanning = false
                override val trigger = 0
                override val refreshErrorKey=0L
                override val runningTasks = emptyList<PojieRunInfo>()
                override val finishedInfo = SnapshotStateMap<String, String>()
                override fun reload() {}
                override fun fetchResults() = ScanResult(ScanResult.CODE_SUCCESS, null, emptyList())
                override fun enableWifi() {}
                override fun applyLocation() {}
                override fun enableLocation() {}
                override fun disconnectWifi() {}
            }
        }
    }

    PojieScreenContent(
        onMenuClick = onMenuClick,
        app = app,
        pojieSettings = pojieSettings,
        pojieWifiController = controller,
        onSettingsUpdate = { pojieSettings = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PojieScreenContent(
    onMenuClick: () -> Unit,
    app: ToolboxApp?,
    pojieSettings: PojieSettings,
    pojieWifiController: PojieWifiController,
    onSettingsUpdate: (PojieSettings) -> Unit
) {
    val context = LocalContext.current
    var currentTargetSsid by rememberSaveable { mutableStateOf("") }
    var showResourceSheet by rememberSaveable { mutableStateOf(false) }

    var showResourcesFabDialog by rememberSaveable { mutableStateOf(false) }

    var showHistoryConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val historyItem = remember(currentTargetSsid, app?.pojieHistory?.historyFlow?.collectAsState()?.value) {
        app?.pojieHistory?.historyFlow?.value?.find { it.ssid == currentTargetSsid }
    }

    val pages = remember(pojieSettings, pojieWifiController, showResourcesFabDialog) {
        listOf(
            object : NavPage {
                override val name = context.getString(R.string.resource)
                override val selectedIcon = Icons.Filled.Inbox
                override val unselectedIcon = Icons.Outlined.Inbox
                override val content = @Composable {
                    ResourcesPage(showFabDialog = showResourcesFabDialog) {
                        showResourcesFabDialog = it
                    }
                }
            },
            object : NavPage {
                override val name =  context.getString(R.string.hitsory)
                override val selectedIcon = Icons.Filled.History
                override val unselectedIcon = Icons.Outlined.History
                override val content = @Composable { HistoryPage() }
            },
            object : NavPage {
                override val name =  context.getString(R.string.run)
                override val selectedIcon = Icons.Filled.Home
                override val unselectedIcon = Icons.Outlined.Home
                override val content = @Composable {
                    HomePage(
                        runListView = {
                            RunListView(
                                controller = pojieWifiController,
                                onStartClick = { ssid ->
                                    currentTargetSsid = ssid
                                    // 如果历史记录中有这个 SSID 且还没跑完
                                    val hasHistory = app?.pojieHistory?.historyFlow?.value?.any { it.ssid == ssid } == true
                                    if (hasHistory) {
                                        showHistoryConfirmDialog = true
                                    } else {
                                        showResourceSheet = true
                                    }
                                },
                                onStopClick = { ssid -> app?.pojieTask?.stop(ssid) }
                            )
                        }
                    )
                }
            },
            object : NavPage {
                override val name =  context.getString(R.string.settings)
                override val selectedIcon = Icons.Filled.Settings
                override val unselectedIcon = Icons.Outlined.Settings
                override val content = @Composable {
                    SettingsPage(pojieSettings) { onSettingsUpdate(it) }
                }
            },
            object : NavPage {
                override val name =  context.getString(R.string.help)
                override val selectedIcon = Icons.Filled.Info
                override val unselectedIcon = Icons.Outlined.Info
                override val content = @Composable { HelpPage() }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        NavContainer(pages, 2, stringResource(R.string.wifi_pojie_name), onMenuClick)
        ResourceSelectSheet(
            show = showResourceSheet,
            onDismiss = { showResourceSheet = false },
            wifiInfo = WifiInfo(
                ssid = currentTargetSsid,
                bssid="",
                level = 0,
                capabilities = ""
            ),
            onConfirm = { allPasswords ->
                app?.pojieTask?.start(
                    PojieRunInfo(
                        ssid = currentTargetSsid,
                        tryList = allPasswords,
                        lastTryTime = System.currentTimeMillis()
                    )
                )
                showResourceSheet = false
            }
        )
    }

    if (showHistoryConfirmDialog && historyItem != null) {
        HistoryRecoveryDialog(
            item = historyItem,
            onDismiss = { showHistoryConfirmDialog = false },
            onContinue = {
                app?.pojieTask?.start(
                    PojieRunInfo(
                        ssid = historyItem.ssid,
                        tryList = historyItem.passwords,
                        tryIndex = historyItem.progress,
                        lastTryTime = System.currentTimeMillis()
                    )
                )
                showHistoryConfirmDialog = false
            },
            onReSelect = {
                showHistoryConfirmDialog = false
                showResourceSheet = true
            }
        )
    }
}

@Composable
fun HistoryRecoveryDialog(
    item: PojieHistoryItem,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onReSelect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.found_history)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.found_history_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Surface(
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()) {
                        Text(stringResource(R.string.ssid_string, item.ssid), style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(
                                R.string.wifi_pojie_history_progress_number,
                                item.progress,
                                item.passwords.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReSelect,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Text(stringResource(R.string.re_select_password))
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.use_latest_password))
                }
            }
        },
        dismissButton = null
    )
}