package com.wifi.toolbox.services

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.wifi.toolbox.MyApplication
import com.wifi.toolbox.services.pojie.*
import com.wifi.toolbox.structs.PojieSettings
import kotlinx.coroutines.*

class PojieService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var sharedPreferences: SharedPreferences
    private var _pojieSettings: PojieSettings? = null

    private val pojieSettings: PojieSettings
        get() = _pojieSettings ?: PojieSettings.from(sharedPreferences).also { _pojieSettings = it }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
        _pojieSettings = PojieSettings.from(prefs)
    }

    private lateinit var connectionWorker: ConnectWorker
    private lateinit var taskDispatcher: PojieTaskManager

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings_pojie", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener)
        connectionWorker = ConnectWorker(this, serviceScope)
        taskDispatcher = PojieTaskManager(this, serviceScope, connectionWorker)
    }

    /**
     * 打印日志到全局状态中
     * @param log 日志内容字符串
     */
    fun log(log: String,allowEdit: Boolean=false) {
        (applicationContext as MyApplication).logState.addLog(log,allowEdit)
    }

    /**
     * 停止所有正在运行的破解任务并取消当前的协程作业
     */
    fun stopAllTasks() {
        val app = applicationContext as MyApplication
        app.runningPojieTasks.clear()
        taskDispatcher.cancelCurrentJob()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as MyApplication
        try {
            PojieNotificationHelper.createNotificationChannel(this)
            val notification = PojieNotificationHelper.buildForegroundNotification(this)
            startForeground(1, notification)

            if (!taskDispatcher.isWorking()) {
                app.logState.clear()
                log(PojieNotificationHelper.getAsciiArt())

                connectionWorker.initLogServices(pojieSettings)
                taskDispatcher.startLoop(pojieSettings)
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
    fun stop() {
        log("[运行结束]")
        connectionWorker.closeServices()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }
}