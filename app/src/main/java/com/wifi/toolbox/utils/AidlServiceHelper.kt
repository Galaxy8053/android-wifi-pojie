@file:Suppress("DEPRECATION")
package com.wifi.toolbox.utils

import android.net.wifi.WifiConfiguration
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.structs.WifiInfo

object AidlServiceHelper {
    fun getWifiScanResults(app: ToolboxApp): List<WifiInfo> {
        val result = app.aidl.ipc?.wifiScanResults ?: emptyList()

        val results = mutableListOf<WifiInfo>()
        result.forEach {
            val ssid = it.getString("ssid")!!
            val bssid = it.getString("bssid")!!
            val level = it.getInt("level")
            val capabilities = it.getString("capabilities")!!

            results.add(WifiInfo(ssid, level, bssid, capabilities))
        }
        results.sortByDescending { it.level }
        return results
    }

    fun getSavedWifiList(app: ToolboxApp): List<WifiConfiguration> {
        val result = app.aidl.ipc?.savedWifiList ?: emptyList()
        return result.map {
            WifiConfiguration().apply {
                networkId = it.getInt("netId")
                SSID = it.getString("ssid")
                preSharedKey = it.getString("password") ?: ""
            }
        }
    }
}