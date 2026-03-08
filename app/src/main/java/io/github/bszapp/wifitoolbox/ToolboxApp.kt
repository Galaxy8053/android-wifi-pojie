package io.github.bszapp.wifitoolbox

import android.app.Application
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.launcher.ProcessLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToolboxApp : Application(), IToolboxController {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var processLauncher: ProcessLauncher

    override val state get() = processLauncher.state

    override fun launch(mode: LaunchMode) {
        applicationScope.launch { processLauncher.launch(mode) }
    }

    override fun stop() = processLauncher.stop()

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        ToolboxControllerProvider.register(this)
    }
}