package io.github.bszapp.wifitoolbox.contract

import kotlinx.coroutines.flow.StateFlow

interface IToolboxController {
    val state: StateFlow<ToolboxState>
    fun launch(mode: LaunchMode)
    fun stop()
}