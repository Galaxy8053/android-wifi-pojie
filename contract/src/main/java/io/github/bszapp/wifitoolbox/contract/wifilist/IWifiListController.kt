package io.github.bszapp.wifitoolbox.contract.wifilist

import kotlinx.coroutines.flow.StateFlow

interface IWifiListController {
    val state: StateFlow<ScanState>
    fun startScan()
}