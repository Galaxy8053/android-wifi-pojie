package com.wifi.toolbox.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.items.pojie.ScanResult
import com.wifi.toolbox.ui.items.pojie.ScreenState
import com.wifi.toolbox.ui.items.pojie.StartScanResult
import kotlinx.coroutines.*
import  com.wifi.toolbox.R

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
                StartScanResult(StartScanResult.CODE_UNKNOWN, e.message)
            }
        }
    }

    val performReload: () -> Unit = {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val start = scanInternal()
            when (start.code) {
                StartScanResult.CODE_SUCCESS, StartScanResult.CODE_SEND_FAIL -> {
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

                        1 -> ScanResult(
                            ScanResult.CODE_SUCCESS,
                            null,
                            ShizukuUtil.getWifiScanResults().filter { it.ssid.isNotEmpty() }
                                .distinctBy { it.ssid })

                        2 -> ScanResult(
                            ScanResult.CODE_SUCCESS,
                            null,
                            ApiUtil.getScanResults(context).filter { it.ssid.isNotEmpty() }
                                .distinctBy { it.ssid })

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
                when (settings.enableMode) {
                    0 -> app.alert(
                        context.getString(R.string.error_missing_params),
                        context.getString(R.string.error_enable_wifi_impl_empty)
                    )

                    1 -> checkShizukuUI(app) { ShizukuUtil.setWifiEnabled(true) }
                    2 -> ApiUtil.setWifiEnabled(context, true)
                    else -> app.alert(
                        context.getString(R.string.error_missing_params),
                        context.getString(R.string.tip_not_completed) + "(enableMode=${settings.enableMode})"
                    )
                }
            }

            override fun applyLocation() {
                if (ApiUtil.requestLocationPermission(context as Activity) { reload() }) reload()
            }

            override fun gotoSettings() = onChangePage(3)

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

                    else -> app.alert(
                        context.getString(R.string.error_missing_params),
                        context.getString(R.string.tip_not_completed) + "(enableMode=${settings.enableMode})"
                    )
                }
            }
        }
    }
}