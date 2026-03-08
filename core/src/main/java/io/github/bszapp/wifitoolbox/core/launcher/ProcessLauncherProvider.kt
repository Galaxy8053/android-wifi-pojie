package io.github.bszapp.wifitoolbox.core.launcher

object ProcessLauncherProvider {
    private var launcher: ProcessLauncher? = null

    fun register(l: ProcessLauncher) { launcher = l }

    fun get(): ProcessLauncher = launcher
        ?: error("ProcessLauncher 未注册，请在 Application.onCreate 中调用 ProcessLauncherProvider.register()")
}