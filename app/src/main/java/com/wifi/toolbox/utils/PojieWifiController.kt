@file:Suppress("DEPRECATION")

package com.wifi.toolbox.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.wifi.toolbox.R
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.structs.PojieHistoryItem
import com.wifi.toolbox.structs.PojieRunInfo
import com.wifi.toolbox.structs.PojieSettings
import com.wifi.toolbox.ui.LocalNavTarget
import com.wifi.toolbox.ui.items.checkShizukuUI
import com.wifi.toolbox.ui.items.pojie.ScanResult
import com.wifi.toolbox.ui.items.pojie.ScreenState
import com.wifi.toolbox.ui.items.pojie.StartScanResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface PojieWifiController {
    val uiState: ScreenState
    val isScanning: Boolean
    val trigger: Int
    val refreshErrorKey: Long
    val runningTasks: List<PojieRunInfo>
    val finishedInfo: SnapshotStateMap<String, String>
    fun reload()
    fun fetchResults(): ScanResult
    fun enableWifi()
    fun applyLocation()
    fun gotoSettings()
    fun gotoAppSettings()
    fun enableLocation()
    fun disconnectWifi()
}

@Composable
fun rememberPojieWifiController(
    context: Context, app: ToolboxApp, settings: PojieSettings,
    onChangePage: (Int) -> Unit
): PojieWifiController {
    val scope = rememberCoroutineScope()
    var uiState by rememberSaveable { mutableStateOf<ScreenState>(ScreenState.Idle) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var trigger by rememberSaveable { mutableIntStateOf(0) }
    var showScanResult by rememberSaveable { mutableStateOf(true) }
    var refreshErrorKey by remember { mutableLongStateOf(0L) }
    val currentRunningTasks = app.runningPojieTasks
    val currentFinishedTasks = app.finishedPojieTasksTip
    val navTarget = LocalNavTarget.current
    val historyList by app.pojieHistory.historyFlow.collectAsState(initial = emptyList())
    var cachedSavedNetworks by remember { mutableStateOf<List<android.net.wifi.WifiConfiguration>>(emptyList()) }
    var cachedHistory by remember { mutableStateOf<List<PojieHistoryItem>>(emptyList()) }

    var onWifiEnabledAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(context) {
        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action) {
                    val state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN
                    )
                    if (state == WifiManager.WIFI_STATE_ENABLED) {
                        onWifiEnabledAction?.invoke()
                        onWifiEnabledAction = null
                    }
                }
            }
        }
        context.registerReceiver(wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        onDispose {
            context.unregisterReceiver(wifiReceiver)
        }
    }

    val MIN_SCAN_TIME = 500
    val MAX_SCAN_TIME = 3000
    val SCAN_INTERVAL = 250

    val scanInternal: () -> StartScanResult = {
        if (settings.scanMode == 0) StartScanResult(
            code = StartScanResult.CODE_NOT_SET,
            errorMessage = context.getString(R.string.error_scan_impl_empty)
        )
        else if (!ApiUtil.isWifiEnabled(context)) StartScanResult(
            code = StartScanResult.CODE_WIFI_NOT_ENABLED
        )
        else {
            try {
                when (settings.scanMode) {
                    1 -> {
                        if (checkShizukuUI(app, onGranted = { trigger++ })) {
                            if (ShizukuUtil.startWifiScan(settings.allowScanUseCommand)) StartScanResult(
                                code = StartScanResult.CODE_SUCCESS
                            )
                            else StartScanResult(code = StartScanResult.CODE_SEND_FAIL)
                        } else StartScanResult(
                            code = StartScanResult.CODE_SCAN_FAIL,
                            errorMessage = context.getString(R.string.error_shizuku_no_permission)
                        )
                    }

                    2 -> {
                        try {
                            if (AidlServiceHelper.startWifiScan(
                                    app,
                                    settings.allowScanUseCommand
                                )
                            ) StartScanResult(
                                code = StartScanResult.CODE_SUCCESS
                            )
                            else StartScanResult(code = StartScanResult.CODE_SEND_FAIL)
                        } catch (e: Exception) {
                            StartScanResult(
                                code = StartScanResult.CODE_SERVICE_NOT_BOUND,
                                errorMessage = e.message
                            )
                        }
                    }

                    3 -> {
                        if (ApiUtil.hasLocationPermission(context)) {
                            if (ApiUtil.isLocationEnabled(context)) {
                                if (ApiUtil.startScan(context)) StartScanResult(code = StartScanResult.CODE_SUCCESS)
                                else StartScanResult(code = StartScanResult.CODE_SEND_FAIL)
                            } else StartScanResult(code = StartScanResult.CODE_LOCATION_NOT_ENABLED)
                        } else StartScanResult(code = StartScanResult.CODE_LOCATION_NOT_ALLOWED)
                    }

                    else -> StartScanResult(
                        code = StartScanResult.CODE_UNKNOWN,
                        errorMessage = context.getString(R.string.tip_not_completed) + "\n(scanMode=${settings.scanMode})"
                    )
                }
            } catch (e: Exception) {
                StartScanResult(StartScanResult.CODE_SCAN_FAIL, e.message)
            }
        }
    }

    val performReload: () -> Unit = {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val start = scanInternal()
            when (start.code) {
                StartScanResult.CODE_SUCCESS, StartScanResult.CODE_SEND_FAIL -> {
                    cachedHistory = historyList
                    cachedSavedNetworks = try {
                        when (settings.scanMode) {
                            1 -> ShizukuUtil.getSavedWifiList()
                            2 -> AidlServiceHelper.getSavedWifiList(app)
                            3 -> if (ApiUtil.hasLocationPermission(context)) ApiUtil.getSavedWifiList(app) else emptyList()
                            else -> emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val sendSucceed = start.code == StartScanResult.CODE_SUCCESS
                    uiState = ScreenState.Success(sendSucceed)
                    if (sendSucceed) {
                        showScanResult = false
                        repeat(MIN_SCAN_TIME / SCAN_INTERVAL) {
                            trigger++
                            delay(SCAN_INTERVAL.toLong())
                        }
                        showScanResult = true
                        repeat((MAX_SCAN_TIME - MIN_SCAN_TIME) / SCAN_INTERVAL) {
                            trigger++
                            delay(SCAN_INTERVAL.toLong())
                        }
                    } else {
                        trigger++
                        showScanResult = false
                        delay(MIN_SCAN_TIME.toLong())
                        showScanResult = true
                        trigger++
                    }
                    refreshJob = null
                }

                StartScanResult.CODE_SCAN_FAIL -> uiState = ScreenState.Error(
                    start.errorMessage ?: context.getString(R.string.error_scan_fail),
                    StartScanResult.CODE_SCAN_FAIL
                )

                StartScanResult.CODE_WIFI_NOT_ENABLED -> uiState = ScreenState.Error(
                    context.getString(R.string.error_wifi_not_enabled),
                    StartScanResult.CODE_WIFI_NOT_ENABLED
                )

                StartScanResult.CODE_LOCATION_NOT_ENABLED -> uiState = ScreenState.Error(
                    context.getString(R.string.error_location_not_enabled),
                    StartScanResult.CODE_LOCATION_NOT_ENABLED,
                )

                StartScanResult.CODE_LOCATION_NOT_ALLOWED -> uiState = ScreenState.Error(
                    context.getString(R.string.error_location_not_allowed),
                    StartScanResult.CODE_LOCATION_NOT_ALLOWED,
                )

                StartScanResult.CODE_NOT_SET -> uiState = ScreenState.Error(
                    start.errorMessage ?: "",
                    StartScanResult.CODE_NOT_SET,
                )

                StartScanResult.CODE_SERVICE_NOT_BOUND -> uiState = ScreenState.Error(
                    start.errorMessage ?: "",
                    StartScanResult.CODE_SERVICE_NOT_BOUND,
                )

                else -> uiState = ScreenState.Error(
                    context.getString(
                        R.string.error_unknown_with_message,
                        start.errorMessage ?: ""
                    ),
                    StartScanResult.CODE_UNKNOWN,
                )
            }
            refreshErrorKey++
        }
    }

    LaunchedEffect(settings.scanMode) {
        performReload()
    }

    return remember(
        settings,
        uiState,
        refreshJob,
        trigger,
        refreshErrorKey,
        showScanResult,
        currentRunningTasks.size,
        onWifiEnabledAction
    ) {
        object : PojieWifiController {
            override val uiState = uiState
            override val isScanning = refreshJob?.isActive == true
            override val trigger = trigger
            override val refreshErrorKey = refreshErrorKey
            override val runningTasks = currentRunningTasks
            override val finishedInfo = currentFinishedTasks

            override fun reload() = performReload()

            override fun fetchResults(): ScanResult {
                if (!showScanResult) return ScanResult()
                return try {
                    when (settings.scanMode) {
                        0 -> ScanResult(
                            code = StartScanResult.CODE_NOT_SET,
                            errorMessage = context.getString(R.string.error_scan_impl_empty)
                        )

                        1 -> {
                            val results = ShizukuUtil.getWifiScanResults()
                                .filter { it.ssid.isNotEmpty() }
                                .distinctBy { it.ssid }
                                .map { info ->
                                    info.copy(
                                        savedInfo = cachedSavedNetworks.find { s -> s.SSID == "\"${info.ssid}\"" || s.SSID == info.ssid },
                                        pojieHistoryItem = cachedHistory.find { h -> h.ssid == info.ssid }
                                    )
                                }
                            ScanResult(ScanResult.CODE_SUCCESS, null, results)
                        }

                        2 -> {
                            val results = AidlServiceHelper.getWifiScanResults(app)
                                .filter { it.ssid.isNotEmpty() }
                                .distinctBy { it.ssid }
                                .map { info ->
                                    info.copy(
                                        savedInfo = cachedSavedNetworks.find { s -> s.SSID == "\"${info.ssid}\"" || s.SSID == info.ssid },
                                        pojieHistoryItem = cachedHistory.find { h -> h.ssid == info.ssid }
                                    )
                                }
                            ScanResult(ScanResult.CODE_SUCCESS, null, results)
                        }

                        3 -> {
                            val results = ApiUtil.getScanResults(context)
                                .filter { it.ssid.isNotEmpty() }
                                .distinctBy { it.ssid }
                                .map { info ->
                                    info.copy(
                                        savedInfo = cachedSavedNetworks.find { s -> s.SSID == "\"${info.ssid}\"" || s.SSID == info.ssid },
                                        pojieHistoryItem = cachedHistory.find { h -> h.ssid == info.ssid }
                                    )
                                }
                            ScanResult(ScanResult.CODE_SUCCESS, null, results)
                        }

                        else -> ScanResult(
                            code = StartScanResult.CODE_UNKNOWN,
                            errorMessage = context.getString(R.string.tip_not_completed) + "\n(scanMode=${settings.scanMode})"
                        )
                    }
                } catch (e: Exception) {
                    ScanResult(errorMessage = e.message)
                }
            }

            override fun enableWifi() {
                if (ApiUtil.isWifiEnabled(context)) {
                    reload()
                    return
                }
                onWifiEnabledAction = { reload() }
                try {
                    when (settings.enableMode) {
                        0 -> app.alert(
                            context.getString(R.string.error_missing_params),
                            context.getString(R.string.error_enable_wifi_impl_empty)
                        )

                        1 -> checkShizukuUI(app) { ShizukuUtil.setWifiEnabled(true) }
                        2 -> {
                            AidlServiceHelper.setWifiEnabled(app, true)
                        }

                        3 -> ApiUtil.setWifiEnabled(context, true)
                        else -> app.alert(
                            context.getString(R.string.error_missing_params),
                            context.getString(R.string.tip_not_completed) + "(enableMode=${settings.enableMode})"
                        )
                    }
                } catch (e: Exception) {
                    app.alert(
                        "执行出错",
                        e.message.toString()
                    )
                }
            }

            override fun applyLocation() {
                if (ApiUtil.requestLocationPermission(context as Activity) { reload() }) reload()
            }

            override fun gotoSettings() = onChangePage(3)
            override fun gotoAppSettings() {
                navTarget.value = "Settings"
            }

            override fun enableLocation() {
                if (ApiUtil.enableLocation(context) { reload() }) reload()
            }

            override fun disconnectWifi() {
                when (settings.enableMode) {
                    0 -> app.alert(
                        context.getString(R.string.error_missing_params),
                        context.getString(R.string.error_manage_saved_wifi_impl_empty)
                    )

                    1 -> {
                        ShizukuUtil.disconnectWifi()
                        trigger++
                    }

                    2 -> {
                        AidlServiceHelper.disconnectWifi(app)
                        trigger++
                    }

                    else -> app.alert(
                        context.getString(R.string.error_missing_params),
                        context.getString(R.string.tip_not_completed) + "(enableMode=${settings.enableMode})"
                    )
                }
            }
        }
    }
}