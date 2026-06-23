package com.lexiflow.wordbook.data

import androidx.room.withTransaction
import com.lexiflow.wordbook.domain.CardKind
import com.lexiflow.wordbook.domain.DailyActivity
import com.lexiflow.wordbook.domain.DailyMood
import com.lexiflow.wordbook.domain.LearningStats
import com.lexiflow.wordbook.domain.MemoryHealth
import com.lexiflow.wordbook.domain.RecallRating
import com.lexiflow.wordbook.domain.ReviewScheduler
import com.lexiflow.wordbook.domain.StudyCard
import com.lexiflow.wordbook.domain.StudyMode
import com.lexiflow.wordbook.domain.TrainingCard
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LearningRepository(
    context: android.content.Context,
    private val database: AppDatabase,
    private val preferences: UserPreferences
) {
    private val words = database.wordDao()
    private val events = database.studyEventDao()
    private val books = database.bookDao()
    private val moods = database.moodDao()
    private val assetImporter = AssetWordbookImporter(context.applicationContext, database)
    private val initializeMutex = Mutex()
    @Volatile private var initialized = false

    suspend fun initialize() {
        if (initialized) return
        initializeMutex.withLock {
            if (initialized) return
            val imported = runCatching { assetImporter.importIfNeeded() }
            if (imported.isFailure) android.util.Log.e("LearningRepo", "词库导入失败", imported.exceptionOrNull())
            if (words.count() == 0) {
                if (books.count() == 0) books.insertAll(SeedWords.books)
                words.insertAll(SeedWords.all)
                words.insertBookWords(SeedWords.all.mapIndexed { index, word ->
                    BookWordCrossRef(101, word.id, index + 1)
                })
            }
            initialized = true
        }
    }

    fun settings() = preferences.get()
    fun saveSettings(settings: UserSettings) = preferences.update(settings)
    fun getApiKey() = preferences.getApiKey()
    fun setApiKey(key: String) = preferences.setApiKey(key)

    suspend fun nextCard(mode: StudyMode, ignoreNewLimit: Boolean = false): TrainingCard? {
        initialize()
        val settings = settings()
        val selectedBook = selectedBook()
        val today = dayKey()
        val now = System.currentTimeMillis()
        val raw = words.nextDue(selectedBook.id, now)?.let { StudyCard(it, CardKind.REVIEW) }
            ?: if (ignoreNewLimit || events.newToday(today) < settings.newLimit) {
                words.nextNew(selectedBook.id)?.let { StudyCard(it, CardKind.NEW) }
            } else null
        raw ?: return null
        val options = if (mode == StudyMode.FLASHCARD) {
            val distractors = words.randomWords(selectedBook.id, 8)
                .map { it.meaning }
                .filter { it != raw.word.meaning }
                .distinct()
                .take(3)
            (distractors + raw.word.meaning).shuffled()
        } else emptyList()
        return TrainingCard(raw, mode, options)
    }

    suspend fun nextFreeCard(mode: StudyMode): TrainingCard? {
        initialize()
        val bookId = selectedBook().id
        val raw = words.randomWords(bookId, 1).firstOrNull()?.let { StudyCard(it, CardKind.REVIEW) }
            ?: return null
        val options = if (mode == StudyMode.FLASHCARD) {
            val distractors = words.randomWords(bookId, 8)
                .map { it.meaning }
                .filter { it != raw.word.meaning }
                .distinct()
                .take(3)
            (distractors + raw.word.meaning).shuffled()
        } else emptyList()
        return TrainingCard(raw, mode, options)
    }

    suspend fun answer(card: TrainingCard, rating: RecallRating, durationMs: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            words.update(ReviewScheduler.schedule(card.studyCard.word, rating, now, settings().desiredRetention / 100.0))
            events.insert(
                StudyEventEntity(
                    wordId = card.studyCard.word.id,
                    rating = rating.value,
                    isNew = card.studyCard.kind == CardKind.NEW,
                    occurredAt = now,
                    dayKey = dayKey(now),
                    durationMs = durationMs,
                    mode = card.mode.name
                )
            )
        }
    }

    suspend fun stats(): LearningStats {
        initialize()
        val today = dayKey()
        val now = System.currentTimeMillis()
        val settings = settings()
        val selected = selectedBook()
        val newToday = events.newToday(today)
        val reviewedToday = events.reviewsToday(today)
        val completed = newToday + reviewedToday
        val remainingNew = minOf(settings.newLimit - newToday, words.unseenInBook(selected.id)).coerceAtLeast(0)
        val availableReviews = words.dueCountInBook(selected.id, now)
        val effectiveGoal = minOf(settings.dailyGoal, completed + remainingNew + availableReviews).coerceAtLeast(completed)
        val since = now - 7L * 24 * 60 * 60 * 1000
        val learned = words.learnedInBook(selected.id)
        val solid = words.stabilityAtLeastInBook(selected.id, 30.0)
        val stable = words.stabilityAtLeastInBook(selected.id, 21.0) - solid
        val growing = words.stabilityAtLeastInBook(selected.id, 7.0) - solid - stable
        val health = MemoryHealth(learned).also {
            it._solid = solid
            it._stable = stable.coerceAtLeast(0)
            it._growing = growing.coerceAtLeast(0)
            it._newLearned = (learned - solid - stable.coerceAtLeast(0) - growing.coerceAtLeast(0)).coerceAtLeast(0)
        }
        return LearningStats(
            learned = learned,
            mastered = words.masteredInBook(selected.id),
            newToday = newToday,
            reviewedToday = reviewedToday,
            correctToday = events.correctToday(today),
            streak = calculateStreak(events.activeDays()),
            total = words.totalInBook(selected.id),
            dailyGoal = effectiveGoal,
            nextReviewAt = words.nextFutureReview(now),
            durationTodayMs = events.durationToday(today),
            weeklyActivity = events.activitySince(since).map { DailyActivity(it.dayKey, it.count) },
            memoryHealth = health
        )
    }

    suspend fun featuredWord(): WordEntity {
        initialize()
        val bookId = selectedBook().id
        return words.nextDue(bookId, System.currentTimeMillis())
            ?: words.nextNew(bookId)
            ?: requireNotNull(words.first())
    }

    suspend fun randomFeaturedWord(): WordEntity {
        initialize()
        return words.randomWords(selectedBook().id, 1).firstOrNull()
            ?: words.randomWords(101, 1).firstOrNull()
            ?: requireNotNull(words.first())
    }

    suspend fun books() = books.all()
    suspend fun bookSizes() = words.allBookSizes().associate { it.bookId to it.count }
    suspend fun selectedBook() = books.selected() ?: SeedWords.books.first()
    suspend fun selectBook(id: Int) = books.select(id)
    suspend fun search(query: String) = words.search(query.trim())
    suspend fun searchPaged(query: String, limit: Int, offset: Int) = words.searchPaged(query.trim(), limit, offset)
    suspend fun favorites() = words.favorites()
    suspend fun favoritesPaged(limit: Int, offset: Int) = words.favoritesPaged(limit, offset)
    suspend fun mistakes() = words.mistakes()
    suspend fun mistakesPaged(limit: Int, offset: Int) = words.mistakesPaged(limit, offset)
    suspend fun myWordsPaged(limit: Int, offset: Int) = words.myWordsPaged(limit, offset)
    suspend fun toggleFavorite(id: Int) = words.toggleFavorite(id)

    suspend fun importWords(json: String): Int {
        initialize()
        val root = JSONArray(json)
        val startId = words.maxId() + 1
        val imported = buildList {
            repeat(root.length()) { index ->
                val item = root.getJSONObject(index)
                val text = item.optString("word").trim()
                if (text.isNotEmpty()) add(
                    WordEntity(
                        id = startId + index,
                        text = text,
                        phonetic = item.optString("phonetic"),
                        meaning = item.optString("meaning"),
                        example = item.optString("example"),
                        exampleTranslation = item.optString("translation"),
                        bookId = item.optInt("bookId", selectedBook().id),
                        wordRoot = item.optString("root"),
                        collocation = item.optString("collocation"),
                        synonyms = item.optString("synonyms")
                    )
                )
            }
        }
        words.insertAll(imported)
        val initialPosition = words.bookSize(selectedBook().id)
        words.insertBookWords(imported.mapIndexed { index, word ->
            BookWordCrossRef(word.bookId, word.id, initialPosition + index + 1)
        })
        return imported.size
    }

    suspend fun updateExample(wordId: Int, example: String, translation: String) =
        words.updateExample(wordId, example, translation)

    suspend fun deleteWord(wordId: Int) {
        words.deleteBookWordRef(wordId)
        words.deleteWord(wordId)
    }

    suspend fun addWord(entry: DictEntry): Int {
        initialize()
        val bookId = 200
        val existing = words.exactWord(entry.word)

        if (existing != null) {
            words.moveToMyWords(existing.id)
            runCatching { words.insertBookWords(listOf(BookWordCrossRef(bookId, existing.id, words.bookSize(bookId) + 1))) }
            return existing.id
        }

        val id = words.maxId() + 1
        val entity = WordEntity(
            id = id,
            text = entry.word,
            phonetic = entry.phonetic,
            meaning = entry.meaning,
            example = entry.example,
            exampleTranslation = entry.exampleTranslation,
            bookId = bookId,
            synonyms = entry.synonyms,
            wordRoot = entry.wordRoot
        )
        words.insertAll(listOf(entity))
        val position = words.bookSize(bookId)
        words.insertBookWords(listOf(BookWordCrossRef(bookId, id, position + 1)))
        return id
    }

    suspend fun enrichAndAddWord(word: String, providedMeaning: String, apiKey: String): Boolean {
        initialize()
        val result = DictionaryClient.lookup(word, apiKey)
        if (result is DictResult.Found) {
            val entry = if (providedMeaning.isNotBlank()) result.entry.copy(meaning = providedMeaning) else result.entry
            addWord(entry)
            return true
        }
        if (providedMeaning.isNotBlank()) {
            addWord(DictEntry(word = word, meaning = providedMeaning, phonetic = "", example = "", exampleTranslation = "", synonyms = "", wordRoot = "", meanings = emptyList()))
            return true
        }
        return false
    }

    suspend fun exportJson(): String {
        val result = JSONObject()
        result.put("exportedAt", System.currentTimeMillis())
        result.put("settings", JSONObject().apply {
            val value = settings()
            put("dailyGoal", value.dailyGoal)
            put("newLimit", value.newLimit)
            put("autoPlay", value.autoPlay)
            put("reminderEnabled", value.reminderEnabled)
            put("reminderHour", value.reminderHour)
            put("darkMode", value.darkMode)
            put("desiredRetention", value.desiredRetention)
        })
        result.put("favorites", JSONArray(favorites().map { it.text }))
        result.put("mistakes", JSONArray(mistakes().map { JSONObject().put("word", it.text).put("lapses", it.lapseCount) }))
        result.put("words", JSONArray(words.allWords().map {
            JSONObject()
                .put("word", it.text)
                .put("bookId", it.bookId)
                .put("favorite", it.isFavorite)
                .put("reviews", it.reviewCount)
                .put("lapses", it.lapseCount)
                .put("stability", it.stability)
                .put("nextReviewAt", it.nextReviewAt)
        }))
        result.put("studyEvents", JSONArray(events.allEvents().map {
            JSONObject()
                .put("wordId", it.wordId)
                .put("rating", it.rating)
                .put("isNew", it.isNew)
                .put("occurredAt", it.occurredAt)
                .put("durationMs", it.durationMs)
                .put("mode", it.mode)
        }))
        return result.toString(2)
    }

    suspend fun clearLearningData() {
        database.withTransaction {
            events.deleteAll()
            words.resetProgress()
        }
    }

    suspend fun getMood(dayKey: String) = moods.getByDay(dayKey)?.let { DailyMood(it.dayKey, it.mood) }
    suspend fun setMood(dayKey: String, mood: String) = moods.upsert(MoodEntity(dayKey, mood))
    suspend fun clearMood(dayKey: String) = moods.deleteByDay(dayKey)
    suspend fun allMoods(): Map<String, String> = moods.getAll().associate { it.dayKey to it.mood }

    private fun calculateStreak(activeDays: List<String>): Int {
        if (activeDays.isEmpty()) return 0
        val active = activeDays.toSet()
        val cursor = Calendar.getInstance()
        if (dayKey(cursor.timeInMillis) !in active) cursor.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (dayKey(cursor.timeInMillis) in active) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun dayKey(time: Long = System.currentTimeMillis()) =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
}
