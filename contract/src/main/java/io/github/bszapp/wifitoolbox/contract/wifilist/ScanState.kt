package io.github.bszapp.wifitoolbox.contract.wifilist

import android.net.wifi.ScanResult

data class ScanState(
    val status: ScanStatus = ScanStatus.IDLE,
    val scanResults: List<ScanResult> = emptyList(),
    val errorException: Exception? = null
)