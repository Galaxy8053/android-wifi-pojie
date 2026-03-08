package io.github.bszapp.wifitoolbox.launcher

import android.content.Context
import android.os.IBinder
import io.github.bszapp.wifitoolbox.contract.ToolboxState   // ← 改这里
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus  // ← 改这里
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LaunchMode { SHIZUKU, ROOT }

class ProcessLauncher(private val context: Context) {

    private val _state = MutableStateFlow(ToolboxState())          // ← 改
    val state: StateFlow<ToolboxState> = _state.asStateFlow()      // ← 改

    private var activeLauncher: AutoCloseable? = null
    var mainService: IMainService? = null
        private set

    suspend fun launch(mode: LaunchMode) {
        _state.value = ToolboxState(ToolboxStatus.LAUNCHING, "正在启动...")  // ← 改

        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null

        runCatching {
            val (launcher, binder) = when (mode) {
                LaunchMode.SHIZUKU -> launchViaShizuku()
                LaunchMode.ROOT    -> launchViaRoot()
            }
            activeLauncher = launcher
            mainService = IMainService.Stub.asInterface(binder)

            val info = mainService!!.doPrivilegedThing()
            _state.value = ToolboxState(ToolboxStatus.RUNNING, "运行中 · $info")  // ← 改

        }.onFailure { e ->
            activeLauncher?.closeQuietly()
            activeLauncher = null
            mainService = null
            _state.value = ToolboxState(ToolboxStatus.ERROR, "启动失败: ${e.message}")  // ← 改
        }
    }

    fun stop() {
        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null
        _state.value = ToolboxState(ToolboxStatus.IDLE, "未启动")  // ← 改
    }

    private suspend fun launchViaShizuku(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        launcher.checkPermission()
        val binder = launcher.getServiceBinder(MainService::class.java.name)
        return launcher to binder
    }

    private suspend fun launchViaRoot(): Pair<AutoCloseable, IBinder> {
        val launcher = RootProcessLauncher(context)
        val binder = launcher.getServiceBinder(MainService::class.java.name)
        return launcher to binder
    }
}