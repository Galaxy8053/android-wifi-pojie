package io.github.bszapp.wifitoolbox.services.mainservice

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.IBinder
import android.os.WorkSource
import android.util.Log
import androidx.annotation.Keep

@Keep
@SuppressLint("PrivateApi")
class MainService : IMainService.Stub() {
    override fun isAlive() = true
    override fun getUid(): Int = android.os.Process.myUid()

    private val sdk = android.os.Build.VERSION.SDK_INT


    private val packageName = when (getUid()) {
        0, 1000 -> "android"
        else -> "com.android.shell"
    }

    private fun getWifiService(): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, "wifi") as IBinder
        return Class.forName("android.net.wifi.IWifiManager\$Stub")
            .getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
    }

    override fun startScan(): Boolean {
        return try {
            val wifiService = getWifiService()
            val clazz = wifiService::class.java
            val methodName = "startScan"
            val stringClass = String::class.java

            when {
                sdk >= 30 -> clazz.getMethod(methodName, stringClass, stringClass)
                    .invoke(wifiService, null, null)

                sdk >= 28 -> clazz.getMethod(methodName, stringClass)
                    .invoke(wifiService, null)

                else -> {
                    val scanSettingsClass = Class.forName("android.net.wifi.ScanSettings")
                    when {
                        sdk >= 26 -> clazz.getMethod(
                            methodName,
                            scanSettingsClass,
                            WorkSource::class.java,
                            stringClass
                        ).invoke(wifiService, null, null, null)

                        else -> clazz.getMethod(
                            methodName,
                            scanSettingsClass,
                            WorkSource::class.java
                        ).invoke(wifiService, null, null)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getScanResults(): List<ScanResult> {
        return try {
            val wifiService = getWifiService()
            val clazz = wifiService::class.java
            val methodName = "getScanResults"
            val stringClass = String::class.java

            val result = if (sdk >= 30)
                clazz.getMethod(methodName, stringClass, stringClass)
                    .invoke(wifiService, packageName, null)  // 传包名，featureId 给 null 即可
            else
                clazz.getMethod(methodName, stringClass)
                    .invoke(wifiService, packageName)

            if (result == null) return emptyList()

            @Suppress("UNCHECKED_CAST")
            result.javaClass.getMethod("getList").invoke(result) as? List<ScanResult> ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}