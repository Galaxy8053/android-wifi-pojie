package com.wifi.toolbox.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.wifi.toolbox.IToolboxService
import com.wifi.toolbox.services.AidlService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppAidl(private val context: android.content.Context) {

    val TAG = "AppAidl"

    var ipc by mutableStateOf<IToolboxService?>(null)

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "AIDL Service Connected")
            ipc = IToolboxService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "AIDL Service Disconnected")
            ipc = null
        }
    }

    fun startAIDLServiceRoot() {
        Log.d(TAG, "启动AIDLService")
        val intent = Intent(context, AidlService::class.java)
        RootService.bind(intent, conn)
    }
}