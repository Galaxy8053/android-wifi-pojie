package io.github.bszapp.wifitoolbox.services.mainservice

import android.content.Context
import androidx.annotation.Keep

@Keep
class MainService(private val context: Context) : IMainService.Stub() {

    override fun isAlive() = true

    override fun doPrivilegedThing(): String {
        return "running as uid=${android.os.Process.myUid()}"
    }
}