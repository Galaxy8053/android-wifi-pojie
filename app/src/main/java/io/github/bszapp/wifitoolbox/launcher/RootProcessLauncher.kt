package io.github.bszapp.wifitoolbox.launcher

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import com.rosan.app_process.AppProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlin.coroutines.coroutineContext

internal class RootProcessLauncher(private val context: Context) : AutoCloseable {

    private val processLoader = SuspendLazy<AppProcess> {
        val proc = object : AppProcess.Terminal() {
            override fun newTerminal(): List<String?> = listOf("su")
        }
        val job = currentCoroutineContext()[Job]
        runInterruptible(Dispatchers.IO) {
            if (proc.init(context)) {
                proc
            } else {
                if (job?.isCancelled == true) {
                    throw InterruptedException("init cancelled")
                }
                throw Exception("su命令执行失败，请确认设备已经root，然后在管理器授权本应用")
            }
        }
    }

    suspend fun getServiceBinder(className: String): IBinder =
        processLoader.get().serviceBinder(ComponentName(context, className))

    override fun close() = runBlocking {
        runCatching { processLoader.get().closeQuietly() }
        processLoader.clear()
    }
}