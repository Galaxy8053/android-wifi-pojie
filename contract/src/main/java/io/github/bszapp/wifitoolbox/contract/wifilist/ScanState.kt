package io.github.bszapp.wifitoolbox.contract.wifilist

import android.net.wifi.ScanResult

data class ScanState(
    val status: ScanStatus = ScanStatus.SCANNING,
    val scanResults: List<ScanResult> = emptyList(),
    val startResult: Boolean = true,
    val errorException: Exception? = null
)