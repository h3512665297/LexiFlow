package com.lexiflow.wordbook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lexiflow.wordbook.data.LearningRepository
import com.lexiflow.wordbook.domain.RecallRating
import com.lexiflow.wordbook.domain.StudyMode
import com.lexiflow.wordbook.domain.StudyState
import com.lexiflow.wordbook.domain.TrainingCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(private val repository: LearningRepository) : ViewModel() {
    private val _state = MutableStateFlow<StudyState>(StudyState.Loading)
    val state = _state.asStateFlow()
    private val _mode = MutableStateFlow(StudyMode.FLASHCARD)
    val mode = _mode.asStateFlow()
    private var shownAt = System.currentTimeMillis()
    private var freeReview = false
    private var extraLearning = false
    private var cardsInRound = 0

    init { loadNext() }

    fun setMode(mode: StudyMode) {
        _mode.value = mode
        freeReview = false
        extraLearning = false
        cardsInRound = 0
        loadNext()
    }

    fun loadNext() {
        _state.value = StudyState.Loading
        viewModelScope.launch {
            if (cardsInRound > 0) {
                cardsInRound--
                if (cardsInRound <= 0) {
                    freeReview = false
                    extraLearning = false
                    _state.value = StudyState.Completed(repository.stats().nextReviewAt)
                    return@launch
                }
            }
            shownAt = System.currentTimeMillis()
            val supplier: suspend (StudyMode) -> TrainingCard? = when {
                freeReview -> repository::nextFreeCard
                extraLearning -> { mode -> repository.nextCard(mode, ignoreNewLimit = true) }
                else -> repository::nextCard
            }
            _state.value = supplier(_mode.value)
                ?.let {
                    if (cardsInRound == 0) cardsInRound = repository.settings().dailyGoal
                    StudyState.Ready(it)
                }
                ?: StudyState.Completed(repository.stats().nextReviewAt)
        }
    }

    fun startFreeReview() {
        freeReview = true
        extraLearning = false
        cardsInRound = repository.settings().newLimit
        loadNext()
    }

    fun startExtraLearning() {
        freeReview = false
        extraLearning = true
        cardsInRound = repository.settings().newLimit
        loadNext()
    }

    fun answer(rating: RecallRating) {
        val ready = _state.value as? StudyState.Ready ?: return
        if (ready.answering) return
        _state.value = ready.copy(answering = true)
        viewModelScope.launch {
            repository.answer(ready.card, rating, System.currentTimeMillis() - shownAt)
            loadNext()
        }
    }

    class Factory(private val repository: LearningRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(repository) as T
    }
}
