package io.github.bszapp.wifitoolbox.uidefault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider
import io.github.bszapp.wifitoolbox.contract.IAppController

class DefaultViewModel(app: Application) : AndroidViewModel(app) {

    private val controller: IAppController = AppControllerProvider.get()

    val startup = StartupUiState(controller, viewModelScope)
    val wifiList = WifiListUiState(controller, viewModelScope)
}