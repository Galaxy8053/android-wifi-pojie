package io.github.bszapp.wifitoolbox.uidefault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.bszapp.wifitoolbox.contract.IAppController
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class DefaultViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IAppController = AppControllerProvider.get()

    val uid = controller.startup.state
        .map { it.serverUid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val mode = controller.startup.state
        .map { it.selectedMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun stop() = controller.startup.stop()
}