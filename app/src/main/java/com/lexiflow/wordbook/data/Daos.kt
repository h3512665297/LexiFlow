package com.lexiflow.wordbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction

@Dao
interface WordDao {
    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(id), 0) FROM words")
    suspend fun maxId(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookWords(refs: List<BookWordCrossRef>)

    @Update
    suspend fun update(word: WordEntity)

    @Query("UPDATE words SET example = :example, exampleTranslation = :translation WHERE id = :wordId")
    suspend fun updateExample(wordId: Int, example: String, translation: String)

    @Query("SELECT w.* FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount > 0 AND w.nextReviewAt <= :now ORDER BY w.nextReviewAt ASC LIMIT 1")
    suspend fun nextDue(bookId: Int, now: Long): WordEntity?

    @Query("SELECT w.* FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount = 0 ORDER BY bw.position ASC LIMIT 1")
    suspend fun nextNew(bookId: Int): WordEntity?

    @Query("SELECT * FROM words ORDER BY id ASC LIMIT 1")
    suspend fun first(): WordEntity?

    @Query("SELECT w.* FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomWords(bookId: Int, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE text LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY isFavorite DESC, lapseCount DESC, text LIMIT 100")
    suspend fun search(query: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE LOWER(text) = LOWER(:word) LIMIT 1")
    suspend fun exactWord(word: String): WordEntity?

    @Query("SELECT * FROM words WHERE isFavorite = 1 ORDER BY text")
    suspend fun favorites(): List<WordEntity>

    @Query("SELECT * FROM words WHERE isFavorite = 1 ORDER BY text LIMIT :limit OFFSET :offset")
    suspend fun favoritesPaged(limit: Int, offset: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE lapseCount > 0 ORDER BY lapseCount DESC, text")
    suspend fun mistakes(): List<WordEntity>

    @Query("SELECT * FROM words WHERE lapseCount > 0 ORDER BY lapseCount DESC, text LIMIT :limit OFFSET :offset")
    suspend fun mistakesPaged(limit: Int, offset: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE bookId = 200 ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun myWordsPaged(limit: Int, offset: Int): List<WordEntity>

    @Query("UPDATE words SET bookId = 200 WHERE id = :wordId")
    suspend fun moveToMyWords(wordId: Int)

    @Query("SELECT * FROM words WHERE text LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY isFavorite DESC, lapseCount DESC, text LIMIT :limit OFFSET :offset")
    suspend fun searchPaged(query: String, limit: Int, offset: Int): List<WordEntity>

    @Query("UPDATE words SET isFavorite = NOT isFavorite WHERE id = :wordId")
    suspend fun toggleFavorite(wordId: Int)

    @Query("UPDATE words SET stability = 0, difficulty = 5, reviewCount = 0, lapseCount = 0, nextReviewAt = 0, lastReviewedAt = 0")
    suspend fun resetProgress()

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteWord(wordId: Int)

    @Query("DELETE FROM book_words WHERE wordId = :wordId")
    suspend fun deleteBookWordRef(wordId: Int)

    @Query("DELETE FROM book_words")
    suspend fun deleteBookWords()

    @Query("DELETE FROM words")
    suspend fun deleteAll()

    @Query("SELECT * FROM words ORDER BY id")
    suspend fun allWords(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun total(): Int

    @Query("SELECT COUNT(*) FROM book_words WHERE bookId = :bookId")
    suspend fun totalInBook(bookId: Int): Int

    @Query("SELECT COUNT(*) FROM words WHERE reviewCount > 0")
    suspend fun learned(): Int

    @Query("SELECT COUNT(*) FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount > 0")
    suspend fun learnedInBook(bookId: Int): Int

    @Query("SELECT COUNT(*) FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount > 0 AND w.stability >= :minStability")
    suspend fun stabilityAtLeastInBook(bookId: Int, minStability: Double): Int

    @Query("SELECT COUNT(*) FROM words WHERE reviewCount = 0")
    suspend fun unseen(): Int

    @Query("SELECT COUNT(*) FROM words WHERE reviewCount > 0 AND nextReviewAt <= :now")
    suspend fun dueCount(now: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE stability >= 21")
    suspend fun mastered(): Int

    @Query("SELECT COUNT(*) FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.stability >= 21")
    suspend fun masteredInBook(bookId: Int): Int

    @Query("SELECT MIN(nextReviewAt) FROM words WHERE reviewCount > 0 AND nextReviewAt > :now")
    suspend fun nextFutureReview(now: Long): Long?

    @Query("SELECT COUNT(*) FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount = 0")
    suspend fun unseenInBook(bookId: Int): Int

    @Query("SELECT COUNT(*) FROM words w INNER JOIN book_words bw ON bw.wordId = w.id WHERE bw.bookId = :bookId AND w.reviewCount > 0 AND w.nextReviewAt <= :now")
    suspend fun dueCountInBook(bookId: Int, now: Long): Int

    @Query("SELECT COUNT(*) FROM book_words WHERE bookId = :bookId")
    suspend fun bookSize(bookId: Int): Int

    @Query("SELECT bw.bookId AS bookId, COUNT(*) AS count FROM book_words bw GROUP BY bw.bookId")
    suspend fun allBookSizes(): List<BookWordCount>
}

@Dao
interface StudyEventDao {
    @Insert
    suspend fun insert(event: StudyEventEntity)

    @Query("SELECT COUNT(*) FROM study_events WHERE dayKey = :day")
    suspend fun totalToday(day: String): Int

    @Query("SELECT COUNT(*) FROM study_events WHERE dayKey = :day AND isNew = 1")
    suspend fun newToday(day: String): Int

    @Query("SELECT COUNT(*) FROM study_events WHERE dayKey = :day AND isNew = 0")
    suspend fun reviewsToday(day: String): Int

    @Query("SELECT COUNT(*) FROM study_events WHERE dayKey = :day AND rating >= 3")
    suspend fun correctToday(day: String): Int

    @Query("SELECT DISTINCT dayKey FROM study_events ORDER BY dayKey DESC")
    suspend fun activeDays(): List<String>

    @Query("SELECT dayKey, COUNT(*) AS count FROM study_events WHERE occurredAt >= :since GROUP BY dayKey ORDER BY dayKey")
    suspend fun activitySince(since: Long): List<DailyActivityRow>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM study_events WHERE dayKey = :day")
    suspend fun durationToday(day: String): Long

    @Query("DELETE FROM study_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM study_events ORDER BY occurredAt")
    suspend fun allEvents(): List<StudyEventEntity>
}

data class DailyActivityRow(val dayKey: String, val count: Int)
data class BookWordCount(val bookId: Int, val count: Int)

@Dao
interface MoodDao {
    @Query("SELECT * FROM moods WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getByDay(dayKey: String): MoodEntity?

    @Query("SELECT * FROM moods")
    suspend fun getAll(): List<MoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mood: MoodEntity)

    @Query("DELETE FROM moods WHERE dayKey = :dayKey")
    suspend fun deleteByDay(dayKey: String)
}

@Dao
interface BookDao {
    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Query("SELECT * FROM books ORDER BY id")
    suspend fun all(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isSelected = 1 LIMIT 1")
    suspend fun selected(): BookEntity?

    @Query("UPDATE books SET isSelected = CASE WHEN id = :bookId THEN 1 ELSE 0 END")
    suspend fun select(bookId: Int)

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}
