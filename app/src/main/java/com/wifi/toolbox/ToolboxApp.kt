package com.wifi.toolbox

import android.app.*
import com.wifi.toolbox.app.*
import com.wifi.toolbox.utils.ActivityStack
import kotlinx.coroutines.*

class ToolboxApp : Application() {

    data class AlertDialogData(val title: String, val text: String)
    data class SnackbarData(
        val message: String,
        val actionLabel: String? = null,
        val onActionClick: (() -> Unit)? = null
    )

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var appCrash: AppCrash
    lateinit var shizuku: AppShizuku
    lateinit var ui: AppUI
    lateinit var pojieTask: AppPojieTask

    val pojieConfig get() = pojieTask.pojieConfig
    val logState get() = pojieTask.logState
    val alertDialogState get() = ui.alertFlow
    val snackbarState get() = ui.snackFlow
    val runningPojieTasks get() = pojieTask.TaskRunList
    val finishedPojieTasksTip get() = pojieTask.TaskEndTip

    override fun onCreate() {
        super.onCreate()

        appCrash = AppCrash(this)
        shizuku = AppShizuku(appScope)
        ui = AppUI(appScope)
        pojieTask = AppPojieTask(this)

        appCrash.StartCatch()
        shizuku.init()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = ActivityStack.register(activity)
            override fun onActivityPaused(activity: Activity) = ActivityStack.unregister()
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: android.os.Bundle?
            ) {
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: android.os.Bundle
            ) {
            }

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        shizuku.removeListener()
        appScope.cancel()
    }

    /**
     * 显示弹窗
     * @param title 标题
     * @param text 内容
     * @return none
     */
    fun alert(title: String, text: String) = ui.alert(title, text)
}