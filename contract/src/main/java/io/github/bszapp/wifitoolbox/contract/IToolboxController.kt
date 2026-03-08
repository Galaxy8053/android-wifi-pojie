package io.github.bszapp.wifitoolbox.contract

import kotlinx.coroutines.flow.StateFlow

interface IToolboxController {
    val state: StateFlow<ToolboxState>
    suspend fun launchShizuku()
    suspend fun launchRoot()
    fun stop()
}