package io.github.bszapp.wifitoolbox.contract

enum class LaunchStatus { IDLE, LAUNCHING, RUNNING, ERROR }

data class LaunchState(
    val status: LaunchStatus = LaunchStatus.IDLE,
    val message: String = "未启动"
)