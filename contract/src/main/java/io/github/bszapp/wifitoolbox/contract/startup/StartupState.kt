package io.github.bszapp.wifitoolbox.contract.startup

data class StartupState(
    val status: StartupStatus = StartupStatus.IDLE,
    val selectedMode: StartupMode? = null,
    val errorMessage: String? = null
)