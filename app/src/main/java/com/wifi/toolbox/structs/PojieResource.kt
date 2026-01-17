package com.wifi.toolbox.structs

data class PojieResource(
    val id: String,
    val name: String?,
    val description: String?,
    val type: Int,
    val content: String,
    val author: String?,
    val version: String?
) {
    companion object {
        private val ID_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9._-]+$")
        private val HEADER_REGEX = Regex("""// ==ToolboxScript==([\s\S]*?)// ==/ToolboxScript==""")
        private val PROPERTY_REGEX = Regex("""@(\w+)\s+(.*)""")

        fun parseScript(scriptContent: String): PojieResource {
            val matchResult = HEADER_REGEX.find(scriptContent) ?: throw Exception("未找到脚本声明头")
            val headerContent = matchResult.groupValues[1]

            val properties = mutableMapOf<String, String?>()
            headerContent.lines().forEach { line ->
                PROPERTY_REGEX.find(line)?.let {
                    properties[it.groupValues[1].trim()] = it.groupValues[2].trim()
                }
            }

            val id = properties["id"] ?: throw Exception("脚本声明中未找到id属性")

            if (!ID_REGEX.matches(id)) {
                throw Exception("id必须匹配/^[a-zA-Z][a-zA-Z0-9._-]+\$/")
            }

            return PojieResource(
                id = id,
                name = properties["name"],
                description = properties["description"],
                type = 1,
                content = scriptContent,
                author = properties["author"],
                version = properties["version"]
            )
        }

        fun parseJSON(jsonString: String): PojieResource {
            val json = org.json.JSONObject(jsonString)
            val id = json.optString("id") ?: throw Exception("信息缺少id")

            if (!ID_REGEX.matches(id)) {
                throw Exception("id必须匹配/^[a-zA-Z][a-zA-Z0-9._-]+\$/")
            }

            return PojieResource(
                id = id,
                name = if (json.isNull("name")) null else json.optString("name"),
                description = if (json.isNull("description")) null else json.optString("description"),
                type = 0,
                content = json.optString("content") ?: throw Exception("信息缺少content"),
                author = if (json.isNull("author")) null else json.optString("author"),
                version = if (json.isNull("version")) null else json.optString("version")
            )
        }
    }
}