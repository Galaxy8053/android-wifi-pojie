package com.wifi.toolbox.structs

data class PojieHistoryItem(

    val ssid: String,
    val passwords: List<String>,
    val progress: Int,
    val password: String? = null,
    val lasttime: Long = 0L
)