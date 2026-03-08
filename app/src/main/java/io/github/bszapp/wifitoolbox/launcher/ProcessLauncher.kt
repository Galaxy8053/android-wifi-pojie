package io.github.bszapp.wifitoolbox.launcher

import android.content.Context
import android.os.IBinder
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProcessLauncher(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var launchJob: Job? = null

    private val _state = MutableStateFlow(ToolboxState())
    val state: StateFlow<ToolboxState> = _state.asStateFlow()

    private var activeLauncher: AutoCloseable? = null
    var mainService: IMainService? = null
        private set

    fun launch(mode: LaunchMode) {
        launchJob?.cancel()
        launchJob = scope.launch {
            _state.value = ToolboxState(
                status = ToolboxStatus.LAUNCHING,
                selectedMode = mode
            )

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
                mainService!!.doPrivilegedThing()

                _state.value = ToolboxState(
                    status = ToolboxStatus.RUNNING,
                    selectedMode = mode
                )
            }.onFailure { e ->
                // 被 cancel() 取消时不更新为 ERROR，cancel() 自己负责重置
                if (e is kotlinx.coroutines.CancellationException) return@launch
                activeLauncher?.closeQuietly()
                activeLauncher = null
                mainService = null
                _state.value = ToolboxState(
                    status = ToolboxStatus.ERROR,
                    selectedMode = mode,
                    errorMessage = e.message ?: "未知错误"
                )
            }
        }
    }

    fun cancel() {
        launchJob?.cancel()
        launchJob = null
        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null
        _state.value = ToolboxState() // 回到初始 IDLE，selectedMode = null
    }

    fun stop() {
        launchJob?.cancel()
        launchJob = null
        val launcherToClose = activeLauncher
        activeLauncher = null
        mainService = null
        _state.value = ToolboxState()

        Thread { launcherToClose?.runCatching { close() } }.start()
    }

    private suspend fun launchViaShizukuDirect(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        return launcher to launcher.getDirectServiceBinder()
    }

    private suspend fun launchViaShizukuTerminal(): Pair<AutoCloseable, IBinder> {
        val launcher = ShizukuProcessLauncher(context)
        return launcher to launcher.getTerminalServiceBinder(MainService::class.java.name)
    }

    private suspend fun launchViaRoot(): Pair<AutoCloseable, IBinder> {
        val launcher = RootProcessLauncher(context)
        return launcher to launcher.getServiceBinder(MainService::class.java.name)
    }
}