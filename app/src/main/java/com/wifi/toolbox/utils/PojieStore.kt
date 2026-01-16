package com.wifi.toolbox.utils

import android.content.Context
import com.wifi.toolbox.structs.PojieResource
import org.json.JSONObject
import java.io.File
import java.util.UUID

object PojieStore {
    private const val DIR_NAME = "pojieres"

    private fun getDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAll(context: Context): List<PojieResource> {
        val dir = getDir(context)
        return dir.listFiles()?.filter { it.extension == "json" }?.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText())
                PojieResource(
                    id = file.nameWithoutExtension,
                    name = json.optString("name"),
                    description = json.optString("description"),
                    type = json.optInt("type"),
                    content = json.optString("content")
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    fun save(context: Context, res: PojieResource) {
        val file = File(getDir(context), "${res.id}.json")
        val json = JSONObject().apply {
            put("name", res.name)
            put("description", res.description)
            put("type", res.type)
            put("content", res.content)
        }
        file.writeText(json.toString())
    }

    fun delete(context: Context, id: String) {
        File(getDir(context), "$id.json").delete()
    }

    fun exists(context: Context, id: String): Boolean {
        return File(getDir(context), "$id.json").exists()
    }

    fun generateId(): String {
        val chars = "abcdef"
        val prefix = chars.random()
        val suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 7)
        return "$prefix$suffix"
    }
}