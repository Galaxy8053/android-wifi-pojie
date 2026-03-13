package io.github.bszapp.wifitoolbox

import android.os.Process
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider
import io.github.bszapp.wifitoolbox.contract.IAppController
import io.github.bszapp.wifitoolbox.contract.startup.IStartupController
import io.github.bszapp.wifitoolbox.contract.startup.StartupMode
import io.github.bszapp.wifitoolbox.contract.startup.StartupStatus
import io.github.bszapp.wifitoolbox.contract.wifilist.IWifiListController
import io.github.bszapp.wifitoolbox.launcher.ProcessLauncher
import io.github.bszapp.wifitoolbox.wifilist.WifiListController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToolboxApp : Application(), IAppController {

    private lateinit var processLauncher: ProcessLauncher

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isExiting = MutableStateFlow(false)
    override val isExiting: StateFlow<Boolean> = _isExiting.asStateFlow()

    private val _scanResultsAvailable = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 1
    )

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d("ToolboxApp", "收到扫描广播，结果是否更新：$updated")
                _scanResultsAvailable.tryEmit(updated)
            }
        }
    }

    override val startup = object : IStartupController {
        override val state get() = processLauncher.state
        override fun launch(mode: StartupMode) = processLauncher.launch(mode)
        override fun cancel() = processLauncher.cancel()
        override fun stop(exit: Boolean) {
            processLauncher.stop()
            if (exit) {
                appScope.launch {
                    delay(200)
                    Process.killProcess(Process.myPid())
                }
                _isExiting.value = true
            }
        }
    }
    override val wifiList: IWifiListController by lazy {
        WifiListController(
            scope = appScope,
            getService = { processLauncher.mainService },
            scanResultsAvailable = _scanResultsAvailable
        )
    }

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        AppControllerProvider.register(this)

        registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        appScope.launch {
            startup.state.collect { state ->
                if (state.status == StartupStatus.RUNNING) {
                    Log.d("ToolboxApp", "首次启动扫描wifi")
                    wifiList.startScan()
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(scanReceiver)
        appScope.cancel()
    }
}