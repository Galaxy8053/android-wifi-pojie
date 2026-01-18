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
        return dir.listFiles()?.filter { it.extension == "json" || it.extension == "js" }
            ?.mapNotNull { file ->
                get(context, file.nameWithoutExtension)
            } ?: emptyList()
    }

    fun get(context: Context, id: String): PojieResource? {
        val dir = getDir(context)
        val jsonFile = File(dir, "$id.json")
        if (jsonFile.exists()) return try {
            PojieResource.parseJSON(jsonFile.readText())
        } catch (_: Exception) {
            null
        }

        val jsFile = File(dir, "$id.js")
        if (jsFile.exists()) return try {
            PojieResource.parseScript(jsFile.readText())
        } catch (_: Exception) {
            null
        }

        return null
    }

    fun testExists(context: Context, res: PojieResource, excludeId: String?) {
        PojieResource.testId(res.id)

        val dir = getDir(context)
        val jsonFile = File(dir, "${res.id}.json")
        val jsFile = File(dir, "${res.id}.js")

        if (res.id != excludeId) {
            if (jsonFile.exists() || jsFile.exists()) {
                throw Exception("ID“${res.id}”已经存在")
            }
        }
    }

    fun save(context: Context, res: PojieResource, excludeId: String? = null) {
        val dir = getDir(context)

        testExists(context, res, excludeId)

        val ext = if (res.type == 0) "json" else "js"
        val file = File(dir, "${res.id}.$ext")
        if (res.type == 0) {
            val json = JSONObject().apply {
                put("id", res.id)
                put("name", res.name)
                put("description", res.description)
                put("type", 0)
                put("content", res.content)
                put("author", res.author)
                put("version", res.version)
            }
            file.writeText(json.toString())
        } else {
            file.writeText(res.content)
        }
    }

    fun update(context: Context, oldId: String, res: PojieResource) {
        save(context, res, excludeId = oldId)
        if (oldId != res.id) {
            delete(context, oldId)
        }
    }

    fun delete(context: Context, id: String) {
        val dir = getDir(context)
        File(dir, "$id.json").delete()
        File(dir, "$id.js").delete()
    }

    fun randomID(): String {
        val chars = "abcdef"
        val prefix = chars.random()
        val suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 7)
        return "$prefix$suffix"
    }
}