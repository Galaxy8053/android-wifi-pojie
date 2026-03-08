package io.github.bszapp.wifitoolbox

import android.app.Application
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.launcher.ProcessLauncher

class ToolboxApp : Application(), IToolboxController {

    private lateinit var processLauncher: ProcessLauncher

    override val state get() = processLauncher.state

    override fun launch(mode: LaunchMode) = processLauncher.launch(mode)
    override fun cancel() = processLauncher.cancel()
    override fun stop()   = processLauncher.stop()

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        ToolboxControllerProvider.register(this)
    }
}