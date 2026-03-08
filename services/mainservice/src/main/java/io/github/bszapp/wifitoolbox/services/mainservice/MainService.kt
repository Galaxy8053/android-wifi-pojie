package io.github.bszapp.wifitoolbox.services.mainservice

import androidx.annotation.Keep

@Keep
class MainService : IMainService.Stub() {
    override fun isAlive() = true
    override fun doPrivilegedThing(): String = "running as uid=${android.os.Process.myUid()}"
    override fun getUid(): Int = android.os.Process.myUid()
}