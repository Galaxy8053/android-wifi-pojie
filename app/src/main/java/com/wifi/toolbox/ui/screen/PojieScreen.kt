package com.wifi.toolbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wifi.toolbox.ui.screen.pojie.*
import com.wifi.toolbox.MyApplication
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.items.pojie.ResourceSelectSheet
import com.wifi.toolbox.ui.items.pojie.RunListView
import com.wifi.toolbox.ui.items.pojie.ScanResult
import com.wifi.toolbox.ui.items.pojie.ScreenState
import com.wifi.toolbox.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PojieScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as? MyApplication }

    var pojieSettings by rememberPojieSettings(context)

    val controller = if (app != null) {
        rememberPojieWifiController(context, app, pojieSettings)
    } else {
        remember {
            object : PojieWifiController {
                override val uiState = ScreenState.Success(true)
                override val isScanning = false
                override val trigger = 0
                override val runningTasks = emptyList<PojieRunInfo>()
                override val finishedInfo = SnapshotStateMap<String, String>()
                override fun reload() {}
                override fun fetchResults() = ScanResult(ScanResult.CODE_SUCCESS, null, emptyList())
                override fun toggleWifiOn() {}
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
    app: MyApplication?,
    pojieSettings: PojieSettings,
    pojieWifiController: PojieWifiController,
    onSettingsUpdate: (PojieSettings) -> Unit
) {
    var currentTargetSsid by rememberSaveable { mutableStateOf("") }
    var showResourceSheet by rememberSaveable { mutableStateOf(false) }

    var showResourcesFabDialog by rememberSaveable { mutableStateOf(false) }

    val pages = remember(pojieSettings, pojieWifiController, showResourcesFabDialog) {
        listOf(
            object : NavPage {
                override val name = "资源"
                override val selectedIcon = Icons.Filled.Inbox
                override val unselectedIcon = Icons.Outlined.Inbox
                override val content = @Composable {
                    ResourcesPage(showFabDialog = showResourcesFabDialog) {
                        showResourcesFabDialog = it
                    }
                }
            },
            object : NavPage {
                override val name = "历史"
                override val selectedIcon = Icons.Filled.History
                override val unselectedIcon = Icons.Outlined.History
                override val content = @Composable { HistoryPage() }
            },
            object : NavPage {
                override val name = "运行"
                override val selectedIcon = Icons.Filled.Home
                override val unselectedIcon = Icons.Outlined.Home
                override val content = @Composable {
                    HomePage(
                        runListView = {
                            RunListView(
                                controller = pojieWifiController,
                                onStartClick = { ssid ->
                                    currentTargetSsid = ssid
                                    showResourceSheet = true
                                },
                                onStopClick = { ssid -> app?.stopTaskByName(ssid) }
                            )
                        }
                    )
                }
            },
            object : NavPage {
                override val name = "设置"
                override val selectedIcon = Icons.Filled.Settings
                override val unselectedIcon = Icons.Outlined.Settings
                override val content = @Composable {
                    SettingsPage(pojieSettings) { onSettingsUpdate(it) }
                }
            },
            object : NavPage {
                override val name = "帮助"
                override val selectedIcon = Icons.Filled.Info
                override val unselectedIcon = Icons.Outlined.Info
                override val content = @Composable { HelpPage() }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        NavContainer(pages, 2, "密码字典破解", onMenuClick)
        ResourceSelectSheet(
            show = showResourceSheet,
            onDismiss = { showResourceSheet = false },
            wifiInfo = WifiInfo(
                ssid = currentTargetSsid,
                level = 0,
                capabilities = ""
            ),
            onConfirm = { allPasswords ->
                app?.startTask(
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
}