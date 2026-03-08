package io.github.bszapp.wifitoolbox.launcher

import android.content.Context
import android.os.IBinder
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProcessLauncher(private val context: Context) {

    private val _state = MutableStateFlow(ToolboxState())
    val state: StateFlow<ToolboxState> = _state.asStateFlow()

    private var activeLauncher: AutoCloseable? = null
    var mainService: IMainService? = null
        private set

    suspend fun launch(mode: LaunchMode) {
        _state.value = ToolboxState(ToolboxStatus.LAUNCHING, "正在启动...")

        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null

        runCatching {
            val (launcher, binder) = when (mode) {
                LaunchMode.SHIZUKU          -> launchViaShizukuDirect()
                LaunchMode.SHIZUKU_TERMINAL -> launchViaShizukuTerminal()
                LaunchMode.ROOT             -> launchViaRoot()
            }
            activeLauncher = launcher
            mainService = IMainService.Stub.asInterface(binder)

            val info = mainService!!.doPrivilegedThing()
            _state.value = ToolboxState(ToolboxStatus.RUNNING, "运行中 · $info")

        }.onFailure { e ->
            activeLauncher?.closeQuietly()
            activeLauncher = null
            mainService = null
            _state.value = ToolboxState(ToolboxStatus.ERROR, "启动失败: ${e.message}")
        }
    }

    fun stop() {
        val launcherToClose = activeLauncher
        activeLauncher = null
        mainService = null
        _state.value = ToolboxState(ToolboxStatus.IDLE, "未启动")

        Thread { launcherToClose?.runCatching { close() } }.start()
    }

    private suspend fun launchViaShizukuDirect(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        val binder = launcher.getDirectServiceBinder()
        return launcher to binder
    }

    private suspend fun launchViaShizukuTerminal(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        launcher.checkPermission()
        val binder = launcher.getTerminalServiceBinder(MainService::class.java.name)
        return launcher to binder
    }

    private suspend fun launchViaRoot(): Pair<AutoCloseable, IBinder> {
        val launcher = RootProcessLauncher(context)
        val binder = launcher.getServiceBinder(MainService::class.java.name)
        return launcher to binder
    }
}