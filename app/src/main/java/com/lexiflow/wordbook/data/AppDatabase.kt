package com.lexiflow.wordbook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WordEntity::class, StudyEventEntity::class, BookEntity::class, BookWordCrossRef::class, MoodEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun studyEventDao(): StudyEventDao
    abstract fun bookDao(): BookDao
    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lexiflow.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN bookId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE words ADD COLUMN wordRoot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN collocation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN synonyms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_events ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_events ADD COLUMN mode TEXT NOT NULL DEFAULT 'FLASHCARD'")
                db.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, isSelected INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_words_bookId ON words(bookId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_words_text ON words(text)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS book_words (bookId INTEGER NOT NULL, wordId INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(bookId, wordId))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_words_wordId ON book_words(wordId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_words_bookId_position ON book_words(bookId, position)")
                db.execSQL("INSERT OR IGNORE INTO book_words(bookId, wordId, position) SELECT bookId, id, id FROM words")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS moods (dayKey TEXT NOT NULL, mood TEXT NOT NULL, PRIMARY KEY(dayKey))")
            }
        }
    }
}
