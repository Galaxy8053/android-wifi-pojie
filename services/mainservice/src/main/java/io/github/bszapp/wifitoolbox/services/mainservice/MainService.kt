package io.github.bszapp.wifitoolbox.services.mainservice

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.Process
import android.os.Build
import android.os.IBinder
import android.os.WorkSource
import android.util.Log
import androidx.annotation.Keep

@Keep
@SuppressLint("PrivateApi")
class MainService : IMainService.Stub() {
    override fun isAlive() = true
    override fun getUid(): Int = android.os.Process.myUid()
    override fun getUidStr(): String =
        Runtime.getRuntime().exec("id").inputStream.bufferedReader().readText().trim()

    private val sdk = Build.VERSION.SDK_INT

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
        Log.d(TAG, "startScan() called | SDK=$sdk | uid=${getUid()} | package=$packageName")
        return try {
            val wifiService = getWifiService()
            val clazz = wifiService::class.java
            val methodName = "startScan"
            val stringClass = String::class.java

            Log.d(TAG, "WifiService obtained: ${clazz.name}")

            when {
                // API 30-36: boolean startScan(String packageName, String featureId)
                // featureId 可为 null，但 packageName 必须有值
                sdk >= 30 -> {
                    Log.d(TAG, "Branch: SDK>=30 → startScan(String, String) with ($packageName, null)")
                    clazz.getMethod(methodName, stringClass, stringClass)
                        .invoke(wifiService, packageName, null)
                    Log.d(TAG, "startScan($packageName, null) invoked successfully")
                }

                // API 28-29: boolean startScan(String packageName)
                // packageName 必须有值
                sdk >= 28 -> {
                    Log.d(TAG, "Branch: SDK>=28 → startScan(String) with ($packageName)")
                    clazz.getMethod(methodName, stringClass)
                        .invoke(wifiService, packageName)
                    Log.d(TAG, "startScan($packageName) invoked successfully")
                }

                else -> {
                    val scanSettingsClass = Class.forName("android.net.wifi.ScanSettings")
                    Log.d(TAG, "ScanSettings class loaded: ${scanSettingsClass.name}")

                    when {
                        // API 26-27: void startScan(ScanSettings, WorkSource, String packageName)
                        sdk >= 26 -> {
                            Log.d(TAG, "Branch: SDK>=26 → startScan(ScanSettings, WorkSource, String) with (null, null, $packageName)")
                            clazz.getMethod(
                                methodName,
                                scanSettingsClass,
                                WorkSource::class.java,
                                stringClass
                            ).invoke(wifiService, null, null, packageName)
                            Log.d(TAG, "startScan(null, null, $packageName) invoked successfully")
                        }

                        // API 24-25: void startScan(ScanSettings, WorkSource)
                        else -> {
                            Log.d(TAG, "Branch: SDK<26 → startScan(ScanSettings, WorkSource) with (null, null)")
                            clazz.getMethod(
                                methodName,
                                scanSettingsClass,
                                WorkSource::class.java
                            ).invoke(wifiService, null, null)
                            Log.d(TAG, "startScan(null, null) invoked successfully")
                        }
                    }
                }
            }
            Log.d(TAG, "startScan() completed successfully → returning true")
            true
        } catch (e: Exception) {
            Log.e(TAG, "startScan() failed: ${e::class.java.simpleName}: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
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

            when {
                // API 35-36: ParceledListSlice getScanResults(String, String)
                // 返回值是 ParceledListSlice，需调用 .getList() 取出 List<ScanResult>
                sdk >= 35 -> {
                    val result = clazz.getMethod(methodName, stringClass, stringClass)
                        .invoke(wifiService, packageName, null)
                        ?: return emptyList()
                    @Suppress("UNCHECKED_CAST")
                    result.javaClass.getMethod("getList").invoke(result) as? List<ScanResult>
                        ?: emptyList()
                }

                // API 30-34: List<ScanResult> getScanResults(String, String)
                // 返回值已经是 List<ScanResult>，直接转型，不能调 .getList()
                sdk >= 30 -> {
                    val result = clazz.getMethod(methodName, stringClass, stringClass)
                        .invoke(wifiService, packageName, null)
                        ?: return emptyList()
                    @Suppress("UNCHECKED_CAST")
                    result as? List<ScanResult> ?: emptyList()
                }

                // API 24-29: List<ScanResult> getScanResults(String)
                // 单参数，返回值直接是 List<ScanResult>
                else -> {
                    val result = clazz.getMethod(methodName, stringClass)
                        .invoke(wifiService, packageName)
                        ?: return emptyList()
                    @Suppress("UNCHECKED_CAST")
                    result as? List<ScanResult> ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun watchApp(token: IBinder) {
        token.linkToDeath(
            {
                Log.d(TAG, "App 进程已死，MainService 自杀")
                Process.killProcess(Process.myPid())
            },
            0
        )
        Log.d(TAG, "已注册 App 存活监听，App pid 监控中")
    }

    override fun shutdown() {
        Log.d(TAG, "收到 shutdown 指令，MainService 退出")
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val TAG = "ToolboxMainService"
    }
}