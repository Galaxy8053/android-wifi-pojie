package com.wifi.toolbox.services.pojie

import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.wifi.toolbox.MyApplication
import com.wifi.toolbox.services.*
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.utils.*
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ConnectWorker(
    private val service: PojieService,
    private val scope: CoroutineScope
) {
    private var readLogMode = 0
    private var logcatService: WifiLogcatService? = null
    private var broadcastService: WifiBroadcastService? = null
    private var connectWifiApi29Callback: ConnectivityManager.NetworkCallback? = null

    /**
     * 初始化日志收集服务
     * @param settings 配置信息
     */
    fun initLogServices(settings: PojieSettings) {
        readLogMode = settings.readLogMode
        when (readLogMode) {
            0 -> throw Exception("读取网络日志实现为空，请先去设置中选择")
            1 -> {
                logcatService = WifiLogcatService(settings)
                service.log("Logcat服务已启动")
            }

            2 -> {
                broadcastService = WifiBroadcastService(service)
                service.log("广播监听服务已启动")
            }
        }
    }

    /**
     * 关闭所有服务并释放资源
     */
    fun closeServices() {
        logcatService?.close()
        broadcastService?.close()
    }

    /**
     * 执行具体的任务逻辑，包括调用系统 API 连接 WiFi 及其超时处理
     * @param app 全局 Application 实例
     * @param task 包含 SSID 和密码的任务信息
     * @param settings 设置信息
     * @return 任务执行结果状态码
     */
    suspend fun performTaskLogic(
        app: MyApplication,
        task: SinglePojieTask,
        settings: PojieSettings
    ): Int {
        val startTime = System.currentTimeMillis()
        val connectMode = settings.connectMode
        when (connectMode) {
            0 -> throw Exception("连接wifi实现为空，请先去设置中选择")
            1 -> ShizukuUtil.connectToWifi(task.ssid, task.password)
            2 -> {
                val netId = ApiUtil.connectToWifiApi28(service, task.ssid, task.password)
                if (netId == -1) throw Exception("请求发送失败，请先手动忘记此网络")
            }

            3 -> { /*这里的逻辑被移动到waitForTaskResult中了*/
            }

            else -> throw Exception("前面的区域，以后再来探索吧(connectMode=${settings.connectMode})")
        }

        return withTimeoutOrNull(timeMillis = app.pojieConfig.maxTryTime.toLong()) {
            suspendCancellableCoroutine<Int> { continuation ->

                val collectJob = scope.launch {
                    waitForTaskResult(app, connectMode, task, continuation, startTime)
                }

                continuation.invokeOnCancellation {
                    collectJob.cancel()
                }
            }
        } ?: SinglePojieTask.RESULT_TIMEOUT
    }

    suspend fun CoroutineScope.waitForTaskResult(
        app: MyApplication,
        connectMode: Int,
        task: SinglePojieTask,
        continuation: CancellableContinuation<Int>,
        startTime: Long
    ) {
        try {
            if (connectMode == 3) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    connectWifiApi29Callback =
                        connectToWifiApi29(task.ssid, task.password) { success ->
                            if (continuation.isActive) {
                                if (success) {
                                    continuation.resume(SinglePojieTask.RESULT_SUCCESS)
                                    cancel()
                                } else {
                                    continuation.resume(SinglePojieTask.RESULT_FAILED)
                                    cancel()
                                }
                            }
                        }
                } else throw Exception("系统版本过低，无法使用[连接到设备]连接wifi(sdk<29&connectMode=3)")
            }

            waitWifiStatus(app, task, startTime, connectMode, continuation)
        } catch (e: Exception) {
            if (e !is CancellationException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    /**
     * 处理网络日志收集，根据不同的读取模式分发日志处理
     * @param app 全局 Application 实例
     * @param task 当前执行的子任务
     * @param startTime 任务开始时间戳
     * @param connectMode 当前的连接模式
     * @param continuation 协程挂起回调
     */
    private suspend fun CoroutineScope.waitWifiStatus(
        app: MyApplication,
        task: SinglePojieTask,
        startTime: Long,
        connectMode: Int,
        continuation: CancellableContinuation<Int>
    ) {
        when (readLogMode) {
            1 -> {
                logcatService?.logFlow?.collect { data ->
                    checkLogData(data, app, task, startTime, connectMode, continuation)
                }
            }

            2 -> {
                if (app.pojieConfig.failureFlag == 2) {
                    throw Exception("广播监听模式不支持按握手次数判定，请在改为“握手超时”或切换为Logcat模式")
                }
                broadcastService?.setTargetSsid(task.ssid)
                broadcastService?.logFlow?.collect { data ->
                    checkLogData(data, app, task, startTime, connectMode, continuation)
                }
            }
        }
    }

    /**
     * 校验并处理单条日志数据，判定连接结果
     * @param data 捕获的 WiFi 日志数据
     * @param app 全局 Application 实例
     * @param task 当前子任务信息
     * @param startTime 任务启动时间
     * @param connectMode 连接模式
     * @param continuation 协程回调
     */
    private fun CoroutineScope.checkLogData(
        data: WifiLogData,
        app: MyApplication,
        task: SinglePojieTask,
        startTime: Long,
        connectMode: Int,
        continuation: CancellableContinuation<Int>
    ) {
        if (!continuation.isActive) return

        val isMatch =
            connectMode == 3 || (data.ssid == task.ssid && data.eventStartTime >= startTime)
        if (!isMatch) {
            if (readLogMode == 1) service.log("W: 信息不匹配，实际${data.ssid}，应为${task.ssid}")
            return
        }

        when (data.event) {
            WifiLogData.EVENT_WIFI_CONNECTED -> {
                if (connectMode != 3) {
                    continuation.resume(SinglePojieTask.RESULT_SUCCESS)
                    cancel()
                }
            }

            WifiLogData.EVENT_CONNECT_FAILED -> {
                if (readLogMode == 2 || System.currentTimeMillis() - data.eventStartTime > 2000) {
                    continuation.resume(SinglePojieTask.RESULT_FAILED)
                    cancel()
                }
            }

            WifiLogData.EVENT_HANDSHAKE -> {
                if (checkHandshakeFailure(data, app.pojieConfig)) {
                    continuation.resume(SinglePojieTask.RESULT_FAILED)
                    cancel()
                }
            }

            WifiLogData.EVENT_CONNECT_ERROR -> {
                continuation.resume(SinglePojieTask.RESULT_ERROR_TRANSIENT)
                cancel()
            }
        }
    }

    /**
     * 根据配置校验握手过程是否已触发失败条件
     * @param data 捕获的日志数据
     * @param config 破解配置项
     * @return 是否判定为失败
     */
    private fun checkHandshakeFailure(data: WifiLogData, config: PojieConfig): Boolean {
        return when (config.failureFlag) {
            1 -> data.handshakeUseTime > config.timeout
            2 -> data.handshakeCount > config.maxHandshakeCount
            else -> false
        }
    }

    /**
     * 适配 Android 10+ 的 WiFi 连接 API
     * @param ssid WiFi 名称
     * @param pass WiFi 密码
     * @param callback 连接结果回调
     * @return 网络回调实例
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun connectToWifiApi29(
        ssid: String, pass: String, callback: (Boolean) -> Unit
    ): ConnectivityManager.NetworkCallback? {
        val foregroundActivity = ActivityStack.get()

        return if (foregroundActivity != null) {
            withContext(Dispatchers.Main) {
                ApiUtil.connectToWifiApi29(foregroundActivity, ssid, pass, callback)
            }
        } else {
            ApiUtil.connectToWifiApi29(service, ssid, pass, callback)
        }
    }

    /**
     * 清理连接过程中占用的系统资源，包括取消 API 29 回调或断开当前 Wi-Fi 连接
     * @param settings 设置信息
     */
    fun cleanConnection(settings: PojieSettings) {
        if (connectWifiApi29Callback != null) {
            ApiUtil.cancelWifiRequest(service, connectWifiApi29Callback!!)
            connectWifiApi29Callback = null
        } else {
            when (settings.enableMode) {
                1 -> ShizukuUtil.disconnectWifi()
                2 -> ApiUtil.disconnectWifi(service)
            }
        }
    }

    /**
     * 根据名称移除网络配置
     * @param settings 设置信息
     */
    fun forgetNetwork(settings: PojieSettings, ssid: String): Boolean {
        val netId = when (settings.connectMode) {
            1 -> ShizukuUtil.getNetIdBySsid(ssid)
            2 -> ApiUtil.getNetIdBySsid(service, ssid)
            else -> -1
        }
        if (netId == -1) return false
        return when (settings.connectMode) {
            1 -> {
                ShizukuUtil.forgetNetwork(netId)
                true
            }

            2 -> ApiUtil.forgetNetwork(service, netId)
            else -> false
        }
    }

    /**
     * 获取格式化的当前系统时间字符串
     * @return 格式为 [HH:mm:ss] 的字符串
     */
    fun getLogTime(): String {
        val df = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return "[${df.format(java.util.Date())}]"
    }
}