package io.github.bszapp.wifitoolbox.contract

import kotlinx.coroutines.flow.StateFlow

interface IToolboxController {
    val state: StateFlow<ToolboxState>
    fun launch(mode: LaunchMode)
    fun cancel()   // 取消正在进行的启动，回到 IDLE
    fun stop()     // 停止已运行的服务，回到 IDLE
}