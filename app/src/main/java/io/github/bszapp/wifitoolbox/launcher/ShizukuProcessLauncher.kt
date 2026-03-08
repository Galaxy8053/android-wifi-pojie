package io.github.bszapp.wifitoolbox.launcher

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.rosan.app_process.AppProcess
import io.github.bszapp.wifitoolbox.services.mainservice.MainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method
import kotlin.coroutines.resume

internal class ShizukuProcessLauncher(private val context: Context) : AutoCloseable {
    suspend fun ensurePermission() {
        if (!Shizuku.pingBinder())
            throw Exception("Shizuku未运行，请安装并启动Shizuku服务")

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return

        if (Shizuku.shouldShowRequestPermissionRationale())
            throw Exception("Shizuku授权已被永久拒绝，请手动授权")

        // 请求权限
        val granted = suspendCancellableCoroutine { cont ->
            val requestCode = 1001

            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(reqCode: Int, grantResult: Int) {
                    if (reqCode == requestCode) {
                        Shizuku.removeRequestPermissionResultListener(this)
                        cont.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            cont.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
            }

            Shizuku.requestPermission(requestCode)
        }

        if (!granted)
            throw Exception("Shizuku授权被拒绝")
    }

    // SHIZUKU
    private var directArgs: Shizuku.UserServiceArgs? = null
    private var directConnection: ServiceConnection? = null

    suspend fun getDirectServiceBinder(): IBinder {
        ensurePermission()

        val args = Shizuku.UserServiceArgs(
            ComponentName(context, MainService::class.java)
        )
            .processNameSuffix("mainservice")
            .daemon(false)
        directArgs = args

        return withContext(Dispatchers.IO) {
            callbackFlow {
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        trySend(service!!)
                        channel.close()
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}
                }.also { directConnection = it }

                runCatching {
                    Shizuku.bindUserService(args, connection)
                }.onFailure { close(it) }

                awaitClose()
            }.first()
        }
    }

    // SHIZUKU_TERMINAL
    private fun shizukuNewProcess(
        cmd: Array<String>, env: Array<String>?, dir: String?
    ): ShizukuRemoteProcess {
        val method: Method = Shizuku::class.java
            .getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).also { it.isAccessible = true }
        return method.invoke(null, cmd, env, dir) as ShizukuRemoteProcess
    }

    private val terminalProcessLoader = SuspendLazy<AppProcess> {
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
            else throw Exception("Shizuku Terminal进程启动失败")
        }
    }

    suspend fun getTerminalServiceBinder(className: String): IBinder {
        ensurePermission()
        return terminalProcessLoader.get().serviceBinder(ComponentName(context, className))
    }

    // 关闭服务
    override fun close() = runBlocking {
        runCatching {
            directArgs?.let { args ->
                directConnection?.let { conn ->
                    Shizuku.unbindUserService(args, conn, false)
                }
            }
        }
        directArgs = null
        directConnection = null

        runCatching { terminalProcessLoader.get().closeQuietly() }
        terminalProcessLoader.clear()
    }
}