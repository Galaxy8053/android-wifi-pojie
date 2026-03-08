package io.github.bszapp.wifitoolbox.launcher

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendLazy<T>(private val initializer: suspend () -> T) {
    private var cached: T? = null
    private val mutex = Mutex()
    suspend fun get(): T {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: initializer().also { cached = it }
        }
    }

    fun clear() { cached = null }
}

fun AutoCloseable.closeQuietly() = runCatching { close() }