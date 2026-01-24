package com.wifi.toolbox.utils

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.wifi.toolbox.IRootService

class AppRootService : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return object : IRootService.Stub() {
            override fun call(serviceName: String, methodName: String, args: Bundle): Bundle {
                val result = Bundle()
                try {
                    val smClass = Class.forName("android.os.ServiceManager")
                    val binder = smClass.getMethod("getService", String::class.java).invoke(null, serviceName) as IBinder

                    val stubClass = Class.forName("android.net.wifi.IWifiManager\$Stub")
                    val iwm = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder) ?: throw Exception("Failed to get IWifiManager")

                    val method = iwm.javaClass.methods.find { it.name == methodName }

                    val out = if (method?.parameterCount == 0) {
                        method.invoke(iwm)
                    } else {
                        method?.invoke(iwm, "com.android.shell")
                    }

                    if (out is List<*>) {
                        result.putParcelableArrayList("data", ArrayList(out.filterIsInstance<android.os.Parcelable>()))
                    } else if (out != null) {
                        val list = try {
                            out.javaClass.getMethod("getList").invoke(out) as List<*>
                        } catch (e: Exception) {
                            null
                        }
                        if (list != null) {
                            result.putParcelableArrayList("data", ArrayList(list.filterIsInstance<android.os.Parcelable>()))
                        }
                    }
                } catch (e: Exception) {
                    result.putString("error", e.toString())
                }
                return result
            }
        }
    }
}