package io.github.bszapp.wifitoolbox.uidefault

import io.github.bszapp.wifitoolbox.contract.IAppController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StartupUiState(private val controller: IAppController, scope: CoroutineScope) {

    val uid = controller.startup.state
        .map { it.serverUid }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val mode = controller.startup.state
        .map { it.selectedMode }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun stop() = controller.startup.stop()
}