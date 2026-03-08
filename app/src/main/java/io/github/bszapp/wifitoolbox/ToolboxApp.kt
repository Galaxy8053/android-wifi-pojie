package io.github.bszapp.wifitoolbox

import android.app.Application
import io.github.bszapp.wifitoolbox.core.launcher.ProcessLauncher
import io.github.bszapp.wifitoolbox.core.launcher.ProcessLauncherProvider

class ToolboxApp : Application() {
    lateinit var processLauncher: ProcessLauncher
        private set

    override fun onCreate() {
        super.onCreate()
        processLauncher = ProcessLauncher(this)
        ProcessLauncherProvider.register(processLauncher)
    }

    companion object {
        // 静态持有，让 defaultUI 的 ViewModel 能拿到，不用 cast Application
        lateinit var instance: ToolboxApp
            private set
    }

    init { instance = this }
}