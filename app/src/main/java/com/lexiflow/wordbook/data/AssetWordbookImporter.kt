package com.lexiflow.wordbook.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetWordbookImporter(
    private val context: Context,
    private val database: AppDatabase
) {
    private val state = context.getSharedPreferences("wordbook_assets", Context.MODE_PRIVATE)

    suspend fun importIfNeeded() = withContext(Dispatchers.IO) {
        val currentVersion = state.getInt(KEY_VERSION, 0)
        val wordDao = database.wordDao()
        val currentCount = wordDao.count()
        if (currentVersion >= VERSION && currentCount > 50) return@withContext

        if (currentVersion >= VERSION && currentCount <= 50) {
            Log.w(TAG, "检测到版本标志为 $currentVersion 但词数仅 $currentCount，强制重新导入")
            state.edit().putInt(KEY_VERSION, 0).apply()
        }

        val bookDao = database.bookDao()
        database.withTransaction {
            wordDao.deleteBookWords()
            wordDao.deleteAll()
            bookDao.deleteAll()
            bookDao.insertAll(SeedWords.books)
        }

        val words = ArrayList<WordEntity>(BATCH_SIZE)
        val refs = ArrayList<BookWordCrossRef>(BATCH_SIZE * 3)
        var totalImported = 0
        var skipped = 0

        try {
            context.assets.open(ASSET_NAME).use { raw ->
                raw.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        try {
                            val item = JSONObject(line)
                            val text = item.getString("word")
                            val id = item.getInt("id")
                            words += WordEntity(
                                id = id,
                                text = text,
                                phonetic = item.optString("phonetic"),
                                meaning = item.optString("meaning"),
                                example = "",
                                exampleTranslation = "",
                                bookId = item.getJSONArray("memberships").getJSONObject(0).getInt("bookId"),
                                wordRoot = item.optString("definition")
                            )
                            totalImported++
                            val memberships = item.getJSONArray("memberships")
                            repeat(memberships.length()) { index ->
                                val membership = memberships.getJSONObject(index)
                                refs += BookWordCrossRef(
                                    bookId = membership.getInt("bookId"),
                                    wordId = id,
                                    position = membership.getInt("position")
                                )
                            }
                            if (words.size >= BATCH_SIZE) {
                                database.withTransaction {
                                    wordDao.insertAll(words)
                                    wordDao.insertBookWords(refs)
                                }
                                words.clear()
                                refs.clear()
                            }
                        } catch (e: JSONException) {
                            skipped++
                        }
                    }
                }
            }
            if (words.isNotEmpty()) {
                database.withTransaction {
                    wordDao.insertAll(words)
                    wordDao.insertBookWords(refs)
                }
            }
            Log.i(TAG, "词库导入完成：$totalImported 词，跳过 $skipped 行")
            state.edit().putInt(KEY_VERSION, VERSION).apply()
        } catch (e: IOException) {
            Log.e(TAG, "读取词库资源失败", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "WordbookImporter"
        private const val ASSET_NAME = "ecdict_wordbooks.ndjson"
        private const val KEY_VERSION = "version"
        private const val VERSION = 2
        private const val BATCH_SIZE = 500
    }
}
