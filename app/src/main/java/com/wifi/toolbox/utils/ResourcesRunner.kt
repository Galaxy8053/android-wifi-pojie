package com.wifi.toolbox.utils

import com.quickjs.JSObject
import com.quickjs.QuickJS
import com.wifi.toolbox.structs.WifiInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object ResourcesRunner {

    suspend fun runScript(
        jsContent: String,
        wifiInfo: WifiInfo,
        timeout: Long = 5000L,
        onLog: (String) -> Unit
    ): List<String> = withContext(Dispatchers.Default) {
        val resultList = mutableListOf<String>()
        val quickJS = QuickJS.createRuntimeWithEventQueue()
        val jsContext = quickJS.createContext()

        try {
            withTimeout(timeout) {
                val taskObj = JSObject(jsContext)
                taskObj.set("ssid", wifiInfo.ssid)

                taskObj.registerJavaMethod({ _, args ->
                    val msg = args.getString(0)
                    onLog(msg)
                }, "log")

                taskObj.registerJavaMethod({ _, args ->
                    val item = args.getString(0)
                    resultList.add(item)
                }, "addItem")

                jsContext.set("task", taskObj)

                jsContext.executeVoidScript(jsContent, null)
            }
        } finally {
            jsContext.close()
            quickJS.close()
        }

        resultList
    }

    suspend fun run(
        resources: List<com.wifi.toolbox.structs.PojieResource>,
        wifiInfo: WifiInfo,
        onProgress: (Int, Int) -> Unit,
        onLog: (String) -> Unit
    ): List<String> {
        val allLists = mutableListOf<List<String>>()

        resources.forEachIndexed { index, res ->
            onProgress(index + 1, resources.size)
            val items = when (res.type) {
                0 -> res.content.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                1 -> try {
                    runScript(res.content, wifiInfo, onLog = onLog)
                } catch (e: Exception) {
                    onLog("脚本执行失败: ${res.id}, ${e.message}")
                    emptyList()
                }
                else -> emptyList()
            }
            if (items.isNotEmpty()) {
                allLists.add(items)
            }
        }

        val combined = mutableListOf<String>()
        val iterators = allLists.map { it.iterator() }.toMutableList()

        while (iterators.isNotEmpty()) {
            val it = iterators.iterator()
            while (it.hasNext()) {
                val scriptIt = it.next()
                if (scriptIt.hasNext()) {
                    combined.add(scriptIt.next())
                } else {
                    it.remove()
                }
            }
        }

        return combined.asSequence()
            .distinct()
            .filter { it.length >= 8 }
            .toList()
    }
}