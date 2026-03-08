package io.github.bszapp.wifitoolbox.contract

data class ToolboxState(
    val status: ToolboxStatus = ToolboxStatus.IDLE,
    val message: String = "未启动"
)