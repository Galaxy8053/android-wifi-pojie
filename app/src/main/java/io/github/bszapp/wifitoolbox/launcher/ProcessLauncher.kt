package io.github.bszapp.wifitoolbox.launcher

import android.content.Context
import android.os.IBinder
import io.github.bszapp.wifitoolbox.contract.startup.StartupMode
import io.github.bszapp.wifitoolbox.contract.startup.StartupState
import io.github.bszapp.wifitoolbox.contract.startup.StartupStatus
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

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    private var activeLauncher: AutoCloseable? = null
    var mainService: IMainService? = null
        private set

    fun launch(mode: StartupMode) {
        launchJob?.cancel()
        launchJob = scope.launch {
            _state.value = StartupState(
                status = StartupStatus.LAUNCHING,
                selectedMode = mode
            )

            activeLauncher?.closeQuietly()
            activeLauncher = null
            mainService = null

            runCatching {
                val (launcher, binder) = when (mode) {
                    StartupMode.SHIZUKU -> launchViaShizukuDirect()
                    StartupMode.SHIZUKU_TERMINAL -> launchViaShizukuTerminal()
                    StartupMode.ROOT -> launchViaRoot()
                }
                activeLauncher = launcher
                mainService = IMainService.Stub.asInterface(binder)
                val uid = mainService!!.getUid()

                _state.value = StartupState(
                    status = StartupStatus.RUNNING,
                    selectedMode = mode,
                    serverUid = uid
                )
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) return@launch
                activeLauncher?.closeQuietly()
                activeLauncher = null
                mainService = null
                _state.value = StartupState(
                    status = StartupStatus.ERROR,
                    selectedMode = mode,
                    errorMessage = e.message
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
        _state.value = StartupState()
    }

    fun stop() {
        launchJob?.cancel()
        launchJob = null
        val launcherToClose = activeLauncher
        activeLauncher = null
        mainService = null
        _state.value = StartupState()

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