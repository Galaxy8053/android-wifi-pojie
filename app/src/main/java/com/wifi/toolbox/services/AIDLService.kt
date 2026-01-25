package com.wifi.toolbox.services

import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.wifi.toolbox.IToolboxService
import java.util.UUID

class AIDLService : RootService() {
    private val TAG = "AIDLService"
    private val uuid = UUID.randomUUID().toString()

    inner class ToolboxIPC : IToolboxService.Stub() {
        override fun getUid(): Int {
            return Process.myUid()
        }

        override fun pressPowerKey() {
            try {
                val binder = Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String::class.java).invoke(null, "input") as IBinder

                val input = Class.forName("android.hardware.input.IInputManager\$Stub")
                    .getMethod("asInterface", IBinder::class.java).invoke(null, binder)

                val inject = input.javaClass.getMethod(
                    "injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType
                )

                val now = android.os.SystemClock.uptimeMillis()
                val injectMode = 2

                inject.invoke(input, android.view.KeyEvent(now, now,
                    android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_POWER, 0), injectMode)
                inject.invoke(input, android.view.KeyEvent(now, now,
                    android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_POWER, 0), injectMode)

            } catch (e: Exception) {
                Log.e(TAG, "注入按键失败", e)
            }
        }
    }

    override fun onCreate() {
        Log.d(TAG, "ToolboxIPC: onCreate, $uuid")
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "ToolboxIPC: onRebind")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "ToolboxIPC: onBind")
        return ToolboxIPC()
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "ToolboxIPC: onUnbind")
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "ToolboxIPC: onDestroy")
    }
}