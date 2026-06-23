package com.lexiflow.wordbook.domain

import com.lexiflow.wordbook.data.WordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    private val word = WordEntity(1, "test", "/test/", "测试", "A test.", "一个测试。")

    @Test
    fun goodFirstAnswerSchedulesAtLeastOneDay() {
        val result = ReviewScheduler.schedule(word, RecallRating.GOOD, 1_000L)
        assertTrue(result.nextReviewAt - 1_000L >= 86_400_000L)
        assertEquals(1, result.reviewCount)
    }

    @Test
    fun forgettingAReviewedWordIncrementsLapses() {
        val reviewed = word.copy(reviewCount = 3, stability = 5.0)
        val result = ReviewScheduler.schedule(reviewed, RecallRating.AGAIN, 1_000L)
        assertEquals(1, result.lapseCount)
        assertTrue(result.stability < reviewed.stability)
    }

    @Test
    fun easyProducesLongerIntervalThanHard() {
        val reviewed = word.copy(reviewCount = 2, stability = 3.0)
        val hard = ReviewScheduler.schedule(reviewed, RecallRating.HARD, 1_000L)
        val easy = ReviewScheduler.schedule(reviewed, RecallRating.EASY, 1_000L)
        assertTrue(easy.nextReviewAt > hard.nextReviewAt)
    }

    @Test
    fun higherDesiredRetentionSchedulesSooner() {
        val reviewed = word.copy(reviewCount = 3, stability = 8.0)
        val standard = ReviewScheduler.schedule(reviewed, RecallRating.GOOD, 1_000L, 0.90)
        val cautious = ReviewScheduler.schedule(reviewed, RecallRating.GOOD, 1_000L, 0.95)
        assertTrue(cautious.nextReviewAt < standard.nextReviewAt)
    }

    @Test
    fun difficultyStaysInBounds() {
        listOf(RecallRating.AGAIN, RecallRating.HARD, RecallRating.GOOD, RecallRating.EASY).forEach { rating ->
            repeat(20) {
                val w = word.copy(difficulty = 5.0, reviewCount = it + 1, stability = 3.0)
                val result = ReviewScheduler.schedule(w, rating, 1_000L)
                assertTrue("difficulty ${result.difficulty} out of bounds", result.difficulty in 1.0..10.0)
            }
        }
    }

    @Test
    fun firstReviewDoesNotIncrementLapsesEvenOnAgain() {
        val result = ReviewScheduler.schedule(word, RecallRating.AGAIN, 1_000L)
        assertEquals(0, result.lapseCount)
    }

    @Test
    fun firstReviewAgainSchedulesShortInterval() {
        val result = ReviewScheduler.schedule(word, RecallRating.AGAIN, 1_000L)
        assertTrue(result.nextReviewAt - 1_000L < 15 * 60_000L)
    }

    @Test
    fun stabilityRemainsPositive() {
        val reviewed = word.copy(reviewCount = 5, stability = 0.02, difficulty = 9.0)
        val result = ReviewScheduler.schedule(reviewed, RecallRating.AGAIN, 1_000L)
        assertTrue(result.stability > 0)
    }

    @Test
    fun consecutiveEasyBuildsLongIntervals() {
        var w = word
        repeat(5) {
            w = ReviewScheduler.schedule(w, RecallRating.EASY, 1_000L)
        }
        assertTrue(w.stability > 100)
    }

    @Test
    fun retentionAtBoundariesWorks() {
        val reviewed = word.copy(reviewCount = 2, stability = 4.0)
        val lowRetention = ReviewScheduler.schedule(reviewed, RecallRating.GOOD, 1_000L, 0.80)
        val highRetention = ReviewScheduler.schedule(reviewed, RecallRating.GOOD, 1_000L, 0.97)
        assertTrue(lowRetention.nextReviewAt > 0)
        assertTrue(highRetention.nextReviewAt > 0)
    }
}
