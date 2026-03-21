package io.github.bszapp.wifitoolbox.contract.wifilist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WifiConfigPatch(
    val autoJoin: Boolean? = null,
    val enabled: Boolean? = null,
) : Parcelable