package com.lexiflow.wordbook.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

data class DictEntry(
    val word: String,
    val phonetic: String,
    val meaning: String,
    val meanings: List<DictMeaning>,
    val example: String,
    val exampleTranslation: String,
    val synonyms: String,
    val wordRoot: String
)

data class DictMeaning(
    val partOfSpeech: String,
    val definitions: List<DictDefinition>
)

data class DictDefinition(
    val definition: String,
    val example: String = ""
)

data class ExampleResult(
    val example: String,
    val translation: String
)

sealed class DictResult {
    data class Found(val entry: DictEntry) : DictResult()
    data object NotFound : DictResult()
    data class Error(val message: String) : DictResult()
}

object DictionaryClient {
    private const val API_URL = "https://api.deepseek.com/chat/completions"

    suspend fun lookup(word: String, apiKey: String): DictResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext DictResult.Error("请先在设置中配置 DeepSeek API Key")
        try {
            val systemPrompt = """你是一个英汉双语词典。对用户输入的英文单词，返回 JSON（不要 markdown 标记）：
{
  "word": "单词本身",
  "phonetic": "音标",
  "meaning": "v. 中文释义1, 中文释义2; n. 中文释义3",
  "posList": [{"pos": "v.", "defs": ["中文释义1", "中文释义2"]}, {"pos": "n.", "defs": ["中文释义3"]}],
  "example": "一个英文例句",
  "exampleTranslation": "例句的中文翻译",
  "synonyms": "同义词, 以逗号分隔",
  "wordRoot": "词根/词源简述"
}
规则：meaning 字段格式为 "词性缩写. 中文释义; 词性缩写. 中文释义"。只输出 JSON。"""

            val body = JSONObject().apply {
                put("model", "deepseek-chat")
                put("temperature", 0.3)
                put("max_tokens", 1024)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", word.trim().lowercase())
                    })
                })
            }

            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                outputStream.bufferedWriter().use { it.write(body.toString()) }
            }

            if (conn.responseCode != 200) {
                val err = runCatching { conn.errorStream.bufferedReader().use { it.readText() } }.getOrDefault("")
                return@withContext DictResult.Error("API 错误 ${conn.responseCode}: $err")
            }

            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val response = JSONObject(raw)
            val content = response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val json = JSONObject(content)
            val meanings = buildList {
                val arr = json.optJSONArray("posList") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val defs = buildList {
                        val defArr = m.optJSONArray("defs") ?: JSONArray()
                        for (j in 0 until defArr.length()) {
                            add(DictDefinition(definition = defArr.getString(j)))
                        }
                    }
                    add(DictMeaning(partOfSpeech = m.getString("pos"), definitions = defs))
                }
            }

            DictResult.Found(
                DictEntry(
                    word = json.optString("word", word),
                    phonetic = json.optString("phonetic", ""),
                    meaning = json.optString("meaning", ""),
                    meanings = meanings,
                    example = json.optString("example", ""),
                    exampleTranslation = json.optString("exampleTranslation", ""),
                    synonyms = json.optString("synonyms", ""),
                    wordRoot = json.optString("wordRoot", "")
                )
            )
        } catch (e: UnknownHostException) {
            DictResult.Error("网络连接失败，请检查网络")
        } catch (e: Exception) {
            DictResult.Error("查询失败：${e.message}")
        }
    }

    suspend fun generateExample(word: String, meaning: String, apiKey: String): ExampleResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val body = JSONObject().apply {
                put("model", "deepseek-chat")
                put("temperature", 0.5)
                put("max_tokens", 256)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "为英文单词生成一个日常例句。返回 JSON：{\"example\":\"英文例句\",\"translation\":\"中文翻译\"}。只输出 JSON，不要 markdown。")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "$word ($meaning)")
                    })
                })
            }
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                outputStream.bufferedWriter().use { it.write(body.toString()) }
            }
            if (conn.responseCode != 200) return@withContext null
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val content = JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
            val json = JSONObject(content)
            ExampleResult(json.optString("example", ""), json.optString("translation", ""))
        } catch (_: Exception) { null }
    }
}
