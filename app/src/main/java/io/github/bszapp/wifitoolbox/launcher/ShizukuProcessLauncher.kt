package io.github.bszapp.wifitoolbox.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import com.rosan.app_process.AppProcess
import io.github.bszapp.wifitoolbox.launcher.SuspendLazy
import io.github.bszapp.wifitoolbox.launcher.closeQuietly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method

internal class ShizukuProcessLauncher(private val context: Context) : AutoCloseable {

    fun checkPermission() {
        if (!Shizuku.pingBinder()) throw Exception("Shizuku 未运行，请先启动 Shizuku")
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
            throw Exception("未获得 Shizuku 授权")
    }

    private fun shizukuNewProcess(
        cmd: Array<String>, env: Array<String>?, dir: String?
    ): ShizukuRemoteProcess {
        val method: Method = Shizuku::class.java
            .getDeclaredMethod("newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).also { it.isAccessible = true }
        return method.invoke(null, cmd, env, dir) as ShizukuRemoteProcess
    }

    private val processLoader = SuspendLazy<AppProcess> {
        val proc = object : AppProcess.Terminal() {
            override fun newTerminal(): List<String?> = listOf("sh")
            override fun innerProcess(params: ProcessParams): Process {
                val cmd = params.cmdList.toTypedArray()
                val env = params.env
                    ?.mapNotNull { (k, v) -> if (v != null) "$k=$v" else null }
                    ?.toTypedArray()
                return shizukuNewProcess(cmd, env, params.directory)
            }
        }
        withContext(Dispatchers.IO) {
            if (proc.init(context)) proc
            else throw Exception("Shizuku 进程启动失败")
        }
    }

    suspend fun getServiceBinder(className: String): IBinder =
        processLoader.get().serviceBinder(ComponentName(context, className))

    override fun close() = runBlocking {
        runCatching { processLoader.get().closeQuietly() }
        processLoader.clear()
    }
}