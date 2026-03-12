package io.github.bszapp.wifitoolbox.launcher

import android.content.Context
import android.os.IBinder
import android.util.Log
import io.github.bszapp.wifitoolbox.contract.startup.StartupMode
import io.github.bszapp.wifitoolbox.contract.startup.StartupState
import io.github.bszapp.wifitoolbox.contract.startup.StartupStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ProcessLauncher(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var launchJob: Job? = null

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    private var activeLauncher: AutoCloseable? = null
    private var activeBinder: IBinder? = null
    private var deathRecipient: IBinder.DeathRecipient? = null
    var mainService: IMainService? = null
        private set

    private fun cleanupActive() {
        deathRecipient?.let { activeBinder?.unlinkToDeath(it, 0) }
        deathRecipient = null
        activeBinder = null
        activeLauncher?.closeQuietly()
        activeLauncher = null
        mainService = null
    }

    fun launch(mode: StartupMode) {
        val modeName = when (mode) {
            StartupMode.SHIZUKU -> "Shizuku"
            StartupMode.SHIZUKU_TERMINAL -> "Shizuku Terminal"
            StartupMode.ROOT -> "Root"
        }
        Log.d("ProcessLauncher", "开始以 $modeName 模式启动服务")
        launchJob?.cancel()
        launchJob = scope.launch {
            _state.value = StartupState(
                status = StartupStatus.LAUNCHING,
                selectedMode = mode
            )

            cleanupActive()

            runCatching {
                withTimeout(5_000L) {
                    val (launcher, binder) = when (mode) {
                        StartupMode.SHIZUKU -> launchViaShizukuDirect()
                        StartupMode.SHIZUKU_TERMINAL -> launchViaShizukuTerminal()
                        StartupMode.ROOT -> launchViaRoot()
                    }
                    activeLauncher = launcher
                    activeBinder = binder
                    mainService = IMainService.Stub.asInterface(binder)
                    val uid = mainService!!.getUid()

                    val recipient = IBinder.DeathRecipient {
                        Log.e("ProcessLauncher", "$modeName 服务进程崩溃或被终止")
                        scope.launch(Dispatchers.Main) {
                            if (_state.value.status == StartupStatus.RUNNING) {
                                cleanupActive()
                                _state.value = StartupState(
                                    status = StartupStatus.ERROR,
                                    selectedMode = mode,
                                    errorException = Exception("服务进程已崩溃或被终止")
                                )
                            }
                        }
                    }
                    binder.linkToDeath(recipient, 0)
                    deathRecipient = recipient

                    _state.value = StartupState(
                        status = StartupStatus.RUNNING,
                        selectedMode = mode,
                        serverUid = uid
                    )
                    Log.d("ProcessLauncher", "$modeName 服务启动成功，uid=$uid")
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException && e !is TimeoutCancellationException) return@launch
                if (!isActive) return@launch
                cleanupActive()
                val errorException = when (e) {
                    is TimeoutCancellationException -> Exception("等待服务连接超时，su模式请以root或者system身份运行")
                    else -> e as? Exception ?: Exception(e.message)
                }
                Log.e("ProcessLauncher", "$modeName 服务启动失败：${errorException.message}")
                _state.value = StartupState(
                    status = StartupStatus.ERROR,
                    selectedMode = mode,
                    errorException = errorException
                )
            }
        }
    }

    fun cancel() {
        launchJob?.cancel()
        launchJob = null
        cleanupActive()
        _state.value = StartupState()
    }

    fun stop() {
        launchJob?.cancel()
        launchJob = null
        val launcherToClose = activeLauncher
        deathRecipient?.let { activeBinder?.unlinkToDeath(it, 0) }
        deathRecipient = null
        activeBinder = null
        activeLauncher = null
        mainService = null
        _state.value = StartupState()

        Log.d("ProcessLauncher", "停止服务")
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