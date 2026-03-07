package io.github.bszapp.wifitoolbox.defaultui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolboxViewModel: ViewModel() {
    private val _uiState = MutableStateFlow("Hello World!")
    val uiState = _uiState.asStateFlow()
}