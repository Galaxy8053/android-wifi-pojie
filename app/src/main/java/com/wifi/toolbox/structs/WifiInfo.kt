package com.wifi.toolbox.structs

data class WifiInfo(
    val ssid: String,
    val level: Int,
    val bssid: String,
    val capabilities: String
)