package io.github.bszapp.wifitoolbox.contract.startup

import kotlinx.coroutines.flow.StateFlow

interface IStartupController {
    val state: StateFlow<StartupState>
    fun launch(mode: StartupMode)
    fun cancel()
    fun stop(exit: Boolean)
}