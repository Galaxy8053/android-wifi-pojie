package com.wifi.toolbox.ui.screen.pojie

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.utils.PojieStore

class PojieViewModel : ViewModel() {
    var id by mutableStateOf("")
    var name by mutableStateOf("")
    var desc by mutableStateOf("")
    var type by mutableStateOf(0)
    var contentValue by mutableStateOf(TextFieldValue(""))

    private var isInitialized = false

    fun initData(context: Context, targetId: String?) {
        if (isInitialized) return
        if (targetId != null) {
            PojieStore.getAll(context).find { it.id == targetId }?.let {
                id = it.id
                name = it.name
                desc = it.description
                type = it.type
                contentValue = TextFieldValue(it.content)
            }
        } else {
            id = PojieStore.generateId()
        }
        isInitialized = true
    }

    fun reset() {
        id = ""
        name = ""
        desc = ""
        type = 0
        contentValue = TextFieldValue("")
        isInitialized = false
    }
}