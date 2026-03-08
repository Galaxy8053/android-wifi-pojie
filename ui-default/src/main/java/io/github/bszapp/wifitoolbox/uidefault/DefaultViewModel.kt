package io.github.bszapp.wifitoolbox.uidefault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.bszapp.wifitoolbox.contract.IToolboxController
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider

class DefaultViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IToolboxController = ToolboxControllerProvider.get()

    fun stop() = controller.stop()
}