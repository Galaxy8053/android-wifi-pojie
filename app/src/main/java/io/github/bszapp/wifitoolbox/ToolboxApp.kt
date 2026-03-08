package io.github.bszapp.wifitoolbox

import android.app.Application
import io.github.bszapp.wifitoolbox.contract.IAppController
import io.github.bszapp.wifitoolbox.contract.startup.StartupMode
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider
import io.github.bszapp.wifitoolbox.contract.startup.IStartupController
import io.github.bszapp.wifitoolbox.launcher.ProcessLauncher

class ToolboxApp : Application(), IAppController {

    private lateinit var processLauncher: ProcessLauncher

    override val startup = object : IStartupController {
        override val state get() = processLauncher.state
        override fun launch(mode: StartupMode) = processLauncher.launch(mode)
        override fun cancel() = processLauncher.cancel()
        override fun stop() = processLauncher.stop()
    }

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        AppControllerProvider.register(this)
    }
}