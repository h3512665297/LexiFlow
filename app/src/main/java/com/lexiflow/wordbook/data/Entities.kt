package com.lexiflow.wordbook.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index("nextReviewAt"), Index("reviewCount"), Index("bookId"), Index("text", unique = true)]
)
data class WordEntity(
    @PrimaryKey val id: Int,
    val text: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
    val exampleTranslation: String,
    val bookId: Int = 1,
    val wordRoot: String = "",
    val collocation: String = "",
    val synonyms: String = "",
    val isFavorite: Boolean = false,
    val stability: Double = 0.0,
    val difficulty: Double = 5.0,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastReviewedAt: Long = 0L
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val isSelected: Boolean = false
)

@Entity(
    tableName = "book_words",
    primaryKeys = ["bookId", "wordId"],
    indices = [Index("wordId"), Index(value = ["bookId", "position"])]
)
data class BookWordCrossRef(
    val bookId: Int,
    val wordId: Int,
    val position: Int
)

@Entity(
    tableName = "study_events",
    indices = [Index("dayKey"), Index("wordId")]
)
data class StudyEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Int,
    val rating: Int,
    val isNew: Boolean,
    val occurredAt: Long,
    val dayKey: String,
    val durationMs: Long = 0L,
    val mode: String = "FLASHCARD"
)

@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey val dayKey: String,
    val mood: String
)
