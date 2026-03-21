package io.github.bszapp.wifitoolbox.contract.wifilist

import android.net.wifi.WifiConfiguration
import kotlinx.coroutines.flow.StateFlow

interface IWifiListController {
    val state: StateFlow<ScanState>
    val savedWifiList: StateFlow<List<WifiConfiguration>>
    fun startScan()
    fun refreshSavedWifiList()

    fun setWifiEnabled(enabled: Boolean)
    fun updateWifiConfig(networkId: Int, patch: WifiConfigPatch)
}