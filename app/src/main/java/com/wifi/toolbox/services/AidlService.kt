package com.wifi.toolbox.services

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.wifi.toolbox.IToolboxService
import com.wifi.toolbox.utils.ShizukuUtil
import com.wifi.toolbox.utils.ShizukuUtil.PACKAGE_NAME
import com.wifi.toolbox.utils.ShizukuUtil.getWifiMethod
import rikka.shizuku.SystemServiceHelper
import java.util.UUID

class AidlService : RootService() {
    private val TAG = "ToolboxApp-Root"
    private val uuid = UUID.randomUUID().toString()
    private var expectedUid = -1

    inner class ToolboxIPC : IToolboxService.Stub() {
        private fun checkCaller() {
            val callingUid = getCallingUid()
            if (expectedUid != -1 && callingUid != expectedUid) {
                Log.w(TAG, "权限校验失败：$callingUid != $expectedUid")
                throw Exception("Permission Denied")
            }
        }

        override fun getUid(): Int {
            checkCaller()
            return Process.myUid()
        }

        override fun pressPowerKey() {
            checkCaller()
            try {
                val binder = Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String::class.java).invoke(null, "input") as IBinder

                val input = Class.forName("android.hardware.input.IInputManager\$Stub")
                    .getMethod("asInterface", IBinder::class.java).invoke(null, binder)

                val inject = input.javaClass.getMethod(
                    "injectInputEvent",
                    android.view.InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )

                val now = android.os.SystemClock.uptimeMillis()
                val injectMode = 2

                inject.invoke(
                    input, android.view.KeyEvent(
                        now, now,
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_POWER, 0
                    ), injectMode
                )
                inject.invoke(
                    input, android.view.KeyEvent(
                        now, now,
                        android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_POWER, 0
                    ), injectMode
                )

            } catch (e: Exception) {
                Log.e(TAG, "注入按键失败", e)
            }
        }

        fun getWifiService(): Any {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val binder = serviceManager.getMethod("getService", String::class.java)
                .invoke(null, "wifi") as IBinder

            val wifiStub = Class.forName("android.net.wifi.IWifiManager\$Stub")
            val wifiService = wifiStub.getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)
            return wifiService
        }

        override fun setWifiEnabled(enabled: Boolean) {
            checkCaller()
            val wifiService = getWifiService()
            val method = getWifiMethod(wifiService, "setWifiEnabled")

            when (method.parameterTypes.size) {
                3 -> method.invoke(wifiService, PACKAGE_NAME, enabled, Bundle())
                2 -> method.invoke(wifiService, PACKAGE_NAME, enabled)
                1 -> method.invoke(wifiService, enabled)
            }
        }

        override fun forgetNetwork(netId: Int) {
            checkCaller()
            val wifiService = getWifiService()

            val sdk = Build.VERSION.SDK_INT
            if (sdk <= 29) {
                val removeMethod = getWifiMethod(wifiService, "removeNetwork")
                when (removeMethod.parameterTypes.size) {
                    2 -> removeMethod.invoke(wifiService, netId, PACKAGE_NAME)
                    1 -> removeMethod.invoke(wifiService, netId)
                }
            } else {
                val forgetMethod = getWifiMethod(wifiService, "forget")
                when (forgetMethod.parameterTypes.size) {
                    4 -> forgetMethod.invoke(wifiService, netId, null, null, 0)
                    2 -> forgetMethod.invoke(wifiService, netId, null)
                }
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
        expectedUid = intent.getIntExtra("caller_uid", -1)
        Log.d(TAG, "ToolboxIPC: onBind, UID: $expectedUid")
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