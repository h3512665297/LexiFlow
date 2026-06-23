package com.lexiflow.wordbook.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AppDatabaseMigrationTest {
    private val testDbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1to2() {
        val db = helper.createDatabase(testDbName, 1).apply {
            execSQL(
                "INSERT INTO words(id, text, phonetic, meaning, example, exampleTranslation, " +
                    "stability, difficulty, reviewCount, lapseCount, nextReviewAt, lastReviewedAt) " +
                    "VALUES (1, 'hello', '/həˈloʊ/', '你好', 'Hello world.', '你好世界。', 1.0, 5.0, 2, 0, 1000, 500)"
            )
            execSQL(
                "INSERT INTO study_events(wordId, rating, isNew, occurredAt, dayKey) " +
                    "VALUES (1, 3, 1, 1000, '2025-01-01')"
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)
        migrated.query("SELECT * FROM words").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("bookId")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("wordRoot")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("collocation")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("synonyms")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")))
        }
        migrated.query("SELECT * FROM study_events").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("durationMs")))
            assertEquals("FLASHCARD", cursor.getString(cursor.getColumnIndexOrThrow("mode")))
        }
        migrated.query("SELECT COUNT(*) FROM books").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2to3() {
        val db = helper.createDatabase(testDbName, 2).apply {
            execSQL(
                "INSERT INTO words(id, text, phonetic, meaning, example, exampleTranslation, " +
                    "bookId, wordRoot, collocation, synonyms, isFavorite, " +
                    "stability, difficulty, reviewCount, lapseCount, nextReviewAt, lastReviewedAt) " +
                    "VALUES (10, 'test', '/t/', '测试', 'A test.', '一个测试。', 101, 'root', 'coll', 'syn', 1, 3.0, 6.0, 5, 1, 2000, 1500)"
            )
            execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, isSelected INTEGER NOT NULL, PRIMARY KEY(id))")
            execSQL("INSERT INTO books(id, name, description, isSelected) VALUES (101, 'Test', 'A test book', 1)")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(testDbName, 3, true, AppDatabase.MIGRATION_2_3)
        migrated.query("SELECT * FROM book_words").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(101, cursor.getInt(cursor.getColumnIndexOrThrow("bookId")))
            assertEquals(10, cursor.getInt(cursor.getColumnIndexOrThrow("wordId")))
        }
        migrated.query("SELECT * FROM words").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("test", cursor.getString(cursor.getColumnIndexOrThrow("text")))
        }
    }
}
