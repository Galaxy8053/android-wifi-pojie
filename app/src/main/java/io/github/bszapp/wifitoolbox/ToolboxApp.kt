package io.github.bszapp.wifitoolbox

import android.app.Application
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import io.github.bszapp.wifitoolbox.launcher.LaunchMode
import io.github.bszapp.wifitoolbox.launcher.ProcessLauncher
import kotlinx.coroutines.flow.StateFlow

class ToolboxApp : Application(), IToolboxController {

    private lateinit var processLauncher: ProcessLauncher

    override val state: StateFlow<ToolboxState>
        get() = processLauncher.state

    override suspend fun launchShizuku() = processLauncher.launch(LaunchMode.SHIZUKU)
    override suspend fun launchRoot()    = processLauncher.launch(LaunchMode.ROOT)
    override fun stop()                  = processLauncher.stop()

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        ToolboxControllerProvider.register(this)  // ← 关键：注册自己
    }
}