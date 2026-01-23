package com.wifi.toolbox.structs

import com.wifi.toolbox.R
import com.wifi.toolbox.ToolboxApp

data class PojieRunInfo(
    val ssid: String,
    val tryList: List<String>,
    val tryIndex: Int = 0,
    val lastTryTime: Long = 0,
    val retryCount: Int = 0,
    val textTip: String = ToolboxApp.instance.getString(R.string.task_preparing)
)