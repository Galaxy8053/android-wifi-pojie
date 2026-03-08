package io.github.bszapp.wifitoolbox.contract

data class ToolboxState(
    val status: ToolboxStatus = ToolboxStatus.IDLE,
    val selectedMode: LaunchMode? = null,
    val errorMessage: String? = null
)