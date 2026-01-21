package com.wifi.toolbox.utils

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.wifi.toolbox.structs.WifiInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

object ResourcesRunner {

    @Suppress("UNUSED_PARAMETER")
    class WebAppInterface(
        private val _ssid: String,
        private val onAdd: (String) -> Unit,
        private val onFinish: () -> Unit
    ){
        @JavascriptInterface
        fun getSsid(): String = _ssid

        @JavascriptInterface
        fun addItem(item: String) {
            onAdd(item)
        }

        @JavascriptInterface
        fun finish() {
            onFinish()
        }
    }

    suspend fun runScript(
        context: Context,
        jsContent: String,
        wifiInfo: WifiInfo,
        timeout: Long = 5000L,
        onLog: (String) -> Unit
    ): List<String> = withContext(Dispatchers.Main) {
        val resultList = mutableListOf<String>()
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                cm?.message()?.let { onLog(it) }
                return true
            }
        }

        try {
            withTimeout(timeout) {
                suspendCancellableCoroutine { continuation ->
                    val bridge = WebAppInterface(
                        _ssid = wifiInfo.ssid,
                        onAdd = { resultList.add(it) },
                        onFinish = { if (continuation.isActive) continuation.resume(Unit) }
                    )
                    webView.addJavascriptInterface(bridge, "_bridge")

                    val wrapperJs = """
                        (async function() {
                            window.task = {
                                ssid: _bridge.getSsid(),
                                addItem: function(i){ _bridge.addItem(i) },
                                finish: function(){ _bridge.finish() },
                                wait: function(ms){ return new Promise(r => setTimeout(r, ms)) }
                            };
                            try {
                                const wait = t => new Promise(r => setTimeout(r, t))
                                await (async () => {
                                    $jsContent
                                })();
                            } catch(e) {
                                console.log("脚本执行错: " + e.message);
                            } finally {
                                window.task.finish();
                            }
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(wrapperJs, null)

                    continuation.invokeOnCancellation {
                        webView.stopLoading()
                        webView.destroy()
                    }
                }
            }
        } catch (e: Exception) {
            onLog("执行异常: ${e.message}")
        } finally {
            webView.destroy()
        }
        resultList
    }

    suspend fun run(
        context: Context,
        resources: List<com.wifi.toolbox.structs.PojieResource>,
        wifiInfo: WifiInfo,
        onProgress: (Int, Int) -> Unit,
        onLog: (String) -> Unit
    ): List<String> {
        val allLists = mutableListOf<List<String>>()
        resources.forEachIndexed { index, res ->
            onProgress(index + 1, resources.size)
            onLog("执行第[${index + 1}]个：${res.name}(${res.id})")
            val items = when (res.type) {
                0 -> res.content.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                1 -> runScript(context, res.content, wifiInfo, onLog = onLog)
                else -> emptyList()
            }
            if (items.isNotEmpty()) allLists.add(items)
            onLog("第[${index + 1}]个执行完毕，共${items.size}条")
        }
        val combined = mutableListOf<String>()
        val iterators = allLists.map { it.iterator() }.toMutableList()
        while (iterators.isNotEmpty()) {
            val it = iterators.iterator()
            while (it.hasNext()) {
                val scriptIt = it.next()
                if (scriptIt.hasNext()) combined.add(scriptIt.next()) else it.remove()
            }
        }
        val result = combined.asSequence().distinct().filter { it.length >= 8 }.toList()
        onLog("全部执行完毕，共${result.size}条")
        return result
    }
}