package io.github.bszapp.wifitoolbox.uidefault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.bszapp.wifitoolbox.contract.IAppController
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider

class DefaultViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IAppController = AppControllerProvider.get()

    fun stop() = controller.stop()
}