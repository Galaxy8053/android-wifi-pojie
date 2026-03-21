package io.github.bszapp.wifitoolbox.services.mainservice

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.os.Process
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import android.os.WorkSource
import android.util.Log
import androidx.annotation.Keep
import io.github.bszapp.wifitoolbox.contract.wifilist.WifiConfigPatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Keep
@SuppressLint("PrivateApi")
@Suppress("DEPRECATION")
class MainService : IMainService.Stub() {
    override fun isAlive() = true
    override fun getUid(): Int = Process.myUid()
    override fun getUidStr(): String =
        Runtime.getRuntime().exec("id").inputStream.bufferedReader().readText().trim()
    override fun getPid(): Int = Process.myPid()

    private val sdk = Build.VERSION.SDK_INT

    private val watcherBound = AtomicBoolean(false)

    init {
        killOlderInstances()

        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.schedule({
            if (!watcherBound.get()) {
                Log.w(TAG, "10 秒内未绑定 App 监听器，自杀")
                Process.killProcess(Process.myPid())
            }
            scheduler.shutdown()
        }, 10, TimeUnit.SECONDS)
    }

    /**
     * 扫描 /proc，找到与当前进程同名、且 PID < myPid 的所有旧实例，逐一 kill。
     * 依赖 /proc/<pid>/cmdline，不需要额外权限（同 UID 可读，root/system 全部可读）。
     */
    private fun killOlderInstances() {
        val myPid = Process.myPid()

        // 读取自身进程名（cmdline 以 \0 分隔参数，取第一段即进程名）
        val myProcessName = try {
            java.io.File("/proc/self/cmdline")
                .readBytes()
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)
                .trim()
        } catch (e: Exception) {
            Log.e(TAG, "读取自身进程名失败: ${e.message}")
            return
        }

        if (myProcessName.isEmpty()) {
            Log.w(TAG, "自身进程名为空，跳过旧实例清理")
            return
        }

        Log.d(TAG, "自身进程名=$myProcessName  pid=$myPid，开始扫描旧实例…")

        val procDir = java.io.File("/proc")
        val entries = procDir.listFiles() ?: run {
            Log.w(TAG, "/proc 不可读，跳过旧实例清理")
            return
        }

        for (pidDir in entries) {
            val pid = pidDir.name.toIntOrNull() ?: continue   // 只处理纯数字目录
            if (pid >= myPid) continue                         // 只杀比自己旧（PID 更小）的

            try {
                val processName = java.io.File(pidDir, "cmdline")
                    .readBytes()
                    .takeWhile { it != 0.toByte() }
                    .toByteArray()
                    .toString(Charsets.UTF_8)
                    .trim()

                if (processName == myProcessName) {
                    Log.w(TAG, "发现旧实例 pid=$pid (< $myPid)，执行 kill")
                    Process.killProcess(pid)
                }
            } catch (_: Exception) {
                // 进程已消失或无权读取，忽略
            }
        }

        Log.d(TAG, "旧实例扫描完成")
    }

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

    //以下是业务功能

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

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun getSavedWifiList(): ByteArray {
        val list = getSavedWifiListInternal()
        val parcel = Parcel.obtain()
        return try {
            parcel.writeTypedList(list)
            parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun getSavedWifiListInternal(): List<WifiConfiguration> {
        return try {
            val wifiService = getWifiService()
            val clazz = wifiService::class.java
            val methodName = "getPrivilegedConfiguredNetworks"
            val stringClass = String::class.java

            val raw = when {
                sdk >= 33 -> {
                    val user = when (getUid()) { 0 -> "root"; 1000 -> "system"; else -> "shell" }
                    val attrSource = AttributionSource::class.java
                        .getConstructor(Int::class.java, String::class.java, String::class.java,
                            Set::class.java, AttributionSource::class.java)
                        .newInstance(getUid(), packageName, packageName, null, null)
                    val bundle = Bundle().apply {
                        putParcelable("EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE", attrSource as Parcelable)
                    }
                    clazz.getMethod(methodName, stringClass, stringClass, Bundle::class.java)
                        .invoke(wifiService, user, packageName, bundle)
                }

                sdk >= 30 -> {
                    clazz.getMethod(methodName, stringClass, stringClass)
                        .invoke(wifiService, packageName, null)
                }

                sdk == 29 -> return getSavedWifiListFallback()

                else -> {
                    try {
                        clazz.getMethod(methodName, stringClass).invoke(wifiService, packageName)
                    } catch (_: NoSuchMethodException) {
                        try {
                            clazz.getMethod(methodName).invoke(wifiService)
                        } catch (_: NoSuchMethodException) {
                            clazz.getMethod("getConfiguredNetworks").invoke(wifiService)
                        }
                    }
                }
            } ?: return getSavedWifiListFallback()

            val list = when (raw) {
                is List<*> -> raw as List<WifiConfiguration>
                else -> raw.javaClass.getMethod("getList").invoke(raw) as List<WifiConfiguration>
            }

            list.distinctBy { it.networkId }
        } catch (e: Exception) {
            Log.e(TAG, "getSavedWifiList() failed: ${e.message}")
            getSavedWifiListFallback()
        }
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun getSavedWifiListFallback(): List<WifiConfiguration> {
        return try {
            val wifiService = getWifiService()
            val clazz = wifiService::class.java

            val raw = when {
                sdk >= 30 -> clazz.getMethod("getConfiguredNetworks", String::class.java, String::class.java)
                    .invoke(wifiService, packageName, null)
                sdk >= 28 -> clazz.getMethod("getConfiguredNetworks", String::class.java)
                    .invoke(wifiService, packageName)
                else -> clazz.getMethod("getConfiguredNetworks").invoke(wifiService)
            } ?: return emptyList()

            val list = when (raw) {
                is List<*> -> raw as List<WifiConfiguration>
                else -> raw.javaClass.getMethod("getList").invoke(raw) as List<WifiConfiguration>
            }
            list.distinctBy { it.networkId }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun updateWifiConfig(networkId: Int, patchBytes: ByteArray): Boolean {
        return try {
            // 反序列化：和 WifiListController.refreshSavedWifiList() 还原 WifiConfiguration 的方式完全一样
            val parcel = Parcel.obtain()
            val patch = try {
                parcel.unmarshall(patchBytes, 0, patchBytes.size)
                parcel.setDataPosition(0)
                parcel.readParcelable<WifiConfigPatch>(WifiConfigPatch::class.java.classLoader)!!
            } finally {
                parcel.recycle()
            }

            val wifiService = getWifiService()
            val clazz = wifiService::class.java

            patch.enabled?.let { enable ->
                if (enable) {
                    if (sdk >= 29)
                        clazz.getMethod("enableNetwork", Int::class.java, Boolean::class.java, String::class.java)
                            .invoke(wifiService, networkId, false, packageName)
                    else
                        clazz.getMethod("enableNetwork", Int::class.java, Boolean::class.java)
                            .invoke(wifiService, networkId, false)
                } else {
                    if (sdk >= 29)
                        clazz.getMethod("disableNetwork", Int::class.java, String::class.java)
                            .invoke(wifiService, networkId, packageName)
                    else
                        clazz.getMethod("disableNetwork", Int::class.java)
                            .invoke(wifiService, networkId)
                }
            }

            patch.autoJoin?.let { autoJoin ->
                if (sdk >= 30) {
                    clazz.getMethod("allowAutojoin", Int::class.java, Boolean::class.java)
                        .invoke(wifiService, networkId, autoJoin)
                } else {
                    val config = getSavedWifiListInternal()
                        .firstOrNull { it.networkId == networkId } ?: return@let
                    WifiConfiguration::class.java.getField("allowAutojoin").setBoolean(config, autoJoin)
                    clazz.getMethod("updateNetwork", WifiConfiguration::class.java)
                        .invoke(wifiService, config)
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "updateWifiConfig failed: ${e.message}", e)
            false
        }
    }

    //===============

    override fun watchApp(token: IBinder) {
        watcherBound.set(true)
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