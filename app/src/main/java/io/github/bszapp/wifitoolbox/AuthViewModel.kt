package io.github.bszapp.wifitoolbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IToolboxController = ToolboxControllerProvider.get()

    val state: StateFlow<ToolboxState> = controller.state

    fun launch(mode: LaunchMode) = controller.launch(mode)
    fun cancel() = controller.cancel()
}