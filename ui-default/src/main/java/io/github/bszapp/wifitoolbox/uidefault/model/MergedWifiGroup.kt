@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.uidefault.model

import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration

data class MergedWifiGroup(
    val networks: List<ScanResult>,
    val savedWifiList: List<WifiConfiguration>
) {
    val strongest: ScanResult get() = networks.first()

    val displaySsid: String
        get() = strongest.SSID
            ?.takeIf { it.isNotEmpty() }
            ?: "<隐藏的网络>"

    val signalDisplay: String get() = "${strongest.level}dBm"

    companion object {
        fun buildFrom(
            results: List<ScanResult>,
            savedWifiList: List<WifiConfiguration>
        ): List<MergedWifiGroup> {
            val visible = results.filter { !it.SSID.isNullOrEmpty() }
            val hidden = results.filter { it.SSID.isNullOrEmpty() }

            val mergedVisible = visible
                .groupBy { it.SSID!! }
                .values
                .map { group ->
                    val ssid = group.first().SSID!!
                    MergedWifiGroup(
                        networks = group.sortedByDescending { it.level },
                        savedWifiList = savedWifiList.filter {
                            it.SSID?.trim('"') == ssid
                        }
                    )
                }

            val mergedHidden = if (hidden.isNotEmpty()) {
                listOf(
                    MergedWifiGroup(
                        networks = hidden.sortedByDescending { it.level },
                        savedWifiList = emptyList()
                    )
                )
            } else emptyList()

            return (mergedVisible + mergedHidden).sortedByDescending { it.strongest.level }
        }
    }
}