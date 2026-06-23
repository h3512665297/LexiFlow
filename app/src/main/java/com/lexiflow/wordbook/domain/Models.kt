package com.lexiflow.wordbook.domain

import com.lexiflow.wordbook.data.WordEntity

enum class RecallRating(val value: Int) {
    AGAIN(1), HARD(2), GOOD(3), EASY(4)
}

enum class CardKind { NEW, REVIEW }

data class StudyCard(val word: WordEntity, val kind: CardKind)

enum class StudyMode {
    FLASHCARD, LISTENING, SPELLING, CLOZE
}

data class TrainingCard(
    val studyCard: StudyCard,
    val mode: StudyMode,
    val options: List<String> = emptyList()
)

data class DailyActivity(val dayKey: String, val count: Int)
data class DailyMood(val dayKey: String, val mood: String)

data class MemoryTier(val label: String, val count: Int, val tier: Int)

data class MemoryHealth(val learned: Int = 0) {
    val tiers: List<MemoryTier> get() {
        if (learned == 0) return emptyList()
        var running = 0
        return listOf(
            MemoryTier("稳固掌握", _solid, 0),
            MemoryTier("稳定记忆", _stable, 1),
            MemoryTier("成长中", _growing, 2),
            MemoryTier("新近掌握", _newLearned, 3)
        ).filter { it.count > 0 }
    }

    var _solid: Int = 0
    var _stable: Int = 0
    var _growing: Int = 0
    var _newLearned: Int = 0
}

data class LearningStats(
    val learned: Int = 0,
    val mastered: Int = 0,
    val newToday: Int = 0,
    val reviewedToday: Int = 0,
    val correctToday: Int = 0,
    val streak: Int = 0,
    val total: Int = 0,
    val dailyGoal: Int = 20,
    val nextReviewAt: Long? = null,
    val durationTodayMs: Long = 0,
    val weeklyActivity: List<DailyActivity> = emptyList(),
    val memoryHealth: MemoryHealth = MemoryHealth()
) {
    val completedToday get() = newToday + reviewedToday
    val accuracy get() = if (completedToday == 0) 0 else correctToday * 100 / completedToday
}

sealed interface StudyState {
    data object Loading : StudyState
    data class Ready(val card: TrainingCard, val answering: Boolean = false) : StudyState
    data class Completed(val nextReviewAt: Long?) : StudyState
}

object SpellingTolerance {
    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            for (k in prev.indices) prev[k] = curr[k]
        }
        return prev[b.length]
    }

    fun matches(input: String, target: String): Boolean {
        val a = input.trim().lowercase()
        val b = target.trim().lowercase()
        if (a.isEmpty()) return false
        if (a == b) return true
        val maxErrors = when {
            b.length <= 4 -> 0
            b.length <= 7 -> 1
            else -> 2
        }
        return levenshtein(a, b) <= maxErrors
    }
}
