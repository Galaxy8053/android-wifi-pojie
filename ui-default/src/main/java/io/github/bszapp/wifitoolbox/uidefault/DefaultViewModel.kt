package io.github.bszapp.wifitoolbox.uidefault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DefaultViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IToolboxController = ToolboxControllerProvider.get()

    val state: StateFlow<ToolboxState> = controller.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, controller.state.value)

    // DefaultViewModel 不暴露 launch/stop
    // 那是 MainActivity 的事
}