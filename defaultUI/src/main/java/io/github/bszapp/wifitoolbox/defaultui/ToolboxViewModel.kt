package io.github.bszapp.wifitoolbox.defaultui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.bszapp.wifitoolbox.core.launcher.LaunchMode
import io.github.bszapp.wifitoolbox.core.launcher.LaunchState
import io.github.bszapp.wifitoolbox.core.launcher.ProcessLauncher
import io.github.bszapp.wifitoolbox.core.launcher.ProcessLauncherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToolboxViewModel(app: Application) : AndroidViewModel(app) {

    // 通过接口拿 ProcessLauncher，不 import ToolboxApp
    private val launcher: ProcessLauncher = ProcessLauncherProvider.get()

    val launchState: StateFlow<LaunchState> = launcher.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, launcher.state.value)

    fun launchWithShizuku() {
        viewModelScope.launch { launcher.launch(LaunchMode.SHIZUKU) }
    }

    fun launchWithRoot() {
        viewModelScope.launch { launcher.launch(LaunchMode.ROOT) }
    }

    fun stop() { launcher.stop() }
}