package io.github.bszapp.wifitoolbox.contract

import io.github.bszapp.wifitoolbox.contract.startup.StartupMode
import io.github.bszapp.wifitoolbox.contract.startup.StartupState
import kotlinx.coroutines.flow.StateFlow

interface IAppController {
    val state: StateFlow<StartupState>
    fun launch(mode: StartupMode)
    fun cancel()   // 取消正在进行的启动，回到 IDLE
    fun stop()     // 停止已运行的服务，回到 IDLE
}