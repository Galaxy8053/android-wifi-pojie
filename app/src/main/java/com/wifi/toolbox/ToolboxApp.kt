package com.wifi.toolbox

import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.wifi.toolbox.app.*
import com.wifi.toolbox.services.AIDLService
import com.wifi.toolbox.utils.ActivityStack
import com.wifi.toolbox.utils.PojieHistoryManager
import com.wifi.toolbox.utils.SettingsManager
import kotlinx.coroutines.*

class ToolboxApp : Application() {

    companion object {
        lateinit var instance: ToolboxApp
            private set
    }

    val TAG="ToolboxApp"

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
    lateinit var settings: SettingsManager
    lateinit var pojieHistory: PojieHistoryManager

    val pojieConfig get() = pojieTask.pojieConfig
    val logState get() = pojieTask.logState
    val alertDialogState get() = ui.alertFlow
    val snackbarState get() = ui.snackFlow
    val runningPojieTasks get() = pojieTask.TaskRunList
    val finishedPojieTasksTip get() = pojieTask.TaskEndTip

    override fun onCreate() {
        super.onCreate()
        instance = this

        appCrash = AppCrash(this)
        shizuku = AppShizuku(appScope)
        ui = AppUI(appScope)
        pojieTask = AppPojieTask(this)
        settings = SettingsManager(this)
        pojieHistory = PojieHistoryManager(this)

        appCrash.StartCatch()
        shizuku.init()


        Log.d(TAG, "Application onCreate: Binding Root Service...")
        val intent = Intent(this, AIDLService::class.java)
        RootService.bind(intent, conn)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = ActivityStack.register(activity)
            override fun onActivityPaused(activity: Activity) = ActivityStack.unregister()
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        shizuku.removeListener()
        appScope.cancel()
    }


    private var ipc: IToolboxService? = null

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "AIDL Service Connected (App Level)")
            ipc = IToolboxService.Stub.asInterface(service)
            try {
                Log.d(TAG, "Executing: Press Power Key")
                ipc?.pressPowerKey()
            } catch (e: RemoteException) {
                Log.e(TAG, "Remote call failed", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "AIDL Service Disconnected")
            ipc = null
        }
    }

    fun alert(title: String, text: String) = ui.alert(title, text)
}