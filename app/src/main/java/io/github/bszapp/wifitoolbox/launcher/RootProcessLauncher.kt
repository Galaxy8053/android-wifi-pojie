package io.github.bszapp.wifitoolbox.launcher

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import com.rosan.app_process.AppProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class RootProcessLauncher(private val context: Context) : AutoCloseable {

    private val processLoader = SuspendLazy<AppProcess> {
        val proc = object : AppProcess.Terminal() {
            override fun newTerminal(): List<String?> = listOf("su")
        }
        withContext(Dispatchers.IO) {
            if (proc.init(context)) proc
            else throw Exception("Root 授权失败，请确认已安装 Magisk 或 KernelSU")
        }
    }

    suspend fun getServiceBinder(className: String): IBinder =
        processLoader.get().serviceBinder(ComponentName(context, className))

    override fun close() = runBlocking {
        runCatching { processLoader.get().closeQuietly() }
        processLoader.clear()
    }
}