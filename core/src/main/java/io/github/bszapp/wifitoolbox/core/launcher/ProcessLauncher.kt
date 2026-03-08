package io.github.bszapp.wifitoolbox.core.launcher

import android.content.Context
import android.os.IBinder
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import io.github.bszapp.wifitoolbox.core.util.closeQuietly
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 启动方式枚举，UI 层用这个发指令
enum class LaunchMode { SHIZUKU, ROOT }

class ProcessLauncher(private val context: Context) {

    private val _state = MutableStateFlow(LaunchState())
    val state: StateFlow<LaunchState> = _state.asStateFlow()

    private var activeLauncher: AutoCloseable? = null

    // 对外暴露已连接的服务，外部可直接调用特权接口
    var mainService: IMainService? = null
        private set

    // UI 通过此方法发送启动指令
    suspend fun launch(mode: LaunchMode) {
        _state.value = LaunchState(LaunchStatus.LAUNCHING, "正在启动...")

        // 先关掉旧进程
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

            // 验证存活并拿到运行信息
            val info = mainService!!.doPrivilegedThing()
            _state.value = LaunchState(LaunchStatus.RUNNING, "运行中 · $info")

        }.onFailure { e ->
            activeLauncher?.closeQuietly()
            activeLauncher = null
            mainService = null
            _state.value = LaunchState(LaunchStatus.ERROR, "启动失败: ${e.message}")
        }
    }

    fun stop() {
        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null
        _state.value = LaunchState(LaunchStatus.IDLE, "未启动")
    }

    private suspend fun launchViaShizuku(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        launcher.checkPermission()  // 权限不足直接抛异常，走 onFailure
        val binder = launcher.getServiceBinder(MainService::class.java.name)
        return launcher to binder
    }

    private suspend fun launchViaRoot(): Pair<AutoCloseable, IBinder> {
        val launcher = RootProcessLauncher(context)
        val binder = launcher.getServiceBinder(MainService::class.java.name)
        return launcher to binder
    }
}