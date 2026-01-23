package com.wifi.toolbox.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PojieHistoryItem(
    val ssid: String,
    val passwords: List<String>,
    val progress: Int,
    val successfulPassword: String? = null
)

class PojieHistoryManager(context: Context) {
    private val _historyFlow = MutableStateFlow<List<PojieHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<PojieHistoryItem>> = _historyFlow

    private val historyFile = File(context.filesDir, "pojiehistory.json")

    init {
        loadHistory()
    }

    fun loadHistory() {
        if (!historyFile.exists()) return
        try {
            val content = historyFile.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<PojieHistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pwdArray = obj.getJSONArray("passwords")
                val pwds = List(pwdArray.length()) { pwdArray.getString(it) }
                list.add(
                    PojieHistoryItem(
                        obj.getString("ssid"),
                        pwds,
                        obj.getInt("progress"),
                        if (obj.has("successfulPassword") && !obj.isNull("successfulPassword"))
                            obj.getString("successfulPassword") else null
                    )
                )
            }
            _historyFlow.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveHistory(list: List<PojieHistoryItem>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("ssid", item.ssid)
            obj.put("progress", item.progress)
            obj.put("successfulPassword", item.successfulPassword ?: JSONObject.NULL)
            val pwdArray = JSONArray()
            item.passwords.forEach { pwdArray.put(it) }
            obj.put("passwords", pwdArray)
            jsonArray.put(obj)
        }
        historyFile.writeText(jsonArray.toString())
        _historyFlow.value = list
    }

    fun addOrUpdateHistory(item: PojieHistoryItem) {
        val currentList = _historyFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.ssid == item.ssid }
        if (index != -1) {
            currentList[index] = item
        } else {
            currentList.add(0, item)
        }
        saveHistory(currentList)
    }

    fun deleteHistory(ssid: String) {
        val currentList = _historyFlow.value.filter { it.ssid != ssid }
        saveHistory(currentList)
    }
}