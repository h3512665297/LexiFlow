package com.lexiflow.wordbook.domain

import com.lexiflow.wordbook.data.WordEntity
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.ln

object ReviewScheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE

    fun schedule(word: WordEntity, rating: RecallRating, now: Long, desiredRetention: Double = 0.9): WordEntity {
        val firstReview = word.reviewCount == 0
        val newDifficulty = when (rating) {
            RecallRating.AGAIN -> word.difficulty + 0.8
            RecallRating.HARD -> word.difficulty + 0.25
            RecallRating.GOOD -> word.difficulty - 0.15
            RecallRating.EASY -> word.difficulty - 0.45
        }.coerceIn(1.0, 10.0)

        val newStability = when {
            firstReview && rating == RecallRating.AGAIN -> 0.01
            firstReview && rating == RecallRating.HARD -> 0.25
            firstReview && rating == RecallRating.GOOD -> 1.0
            firstReview -> 3.0
            rating == RecallRating.AGAIN -> max(0.01, word.stability * 0.2)
            rating == RecallRating.HARD -> max(0.15, word.stability * 1.2)
            rating == RecallRating.GOOD -> max(1.0, word.stability * (1.8 + (10 - newDifficulty) * 0.05))
            else -> max(3.0, word.stability * (2.8 + (10 - newDifficulty) * 0.08))
        }

        val retentionFactor = (ln(desiredRetention.coerceIn(0.8, 0.97)) / ln(0.9)).coerceIn(0.3, 2.2)
        val interval = when (rating) {
            RecallRating.AGAIN -> 5 * MINUTE
            RecallRating.HARD -> max(10 * MINUTE, (newStability * DAY * 0.6 * retentionFactor).toLong())
            RecallRating.GOOD -> max(DAY, (newStability * DAY * retentionFactor).toLong())
            RecallRating.EASY -> max(3 * DAY, (newStability.pow(1.05) * DAY * retentionFactor).toLong())
        }

        return word.copy(
            stability = newStability,
            difficulty = newDifficulty,
            reviewCount = word.reviewCount + 1,
            lapseCount = word.lapseCount + if (rating == RecallRating.AGAIN) 1 else 0,
            nextReviewAt = now + interval,
            lastReviewedAt = now
        )
    }
}
