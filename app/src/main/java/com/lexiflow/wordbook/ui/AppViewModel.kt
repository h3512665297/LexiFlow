package com.lexiflow.wordbook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lexiflow.wordbook.data.BookEntity
import com.lexiflow.wordbook.data.LearningRepository
import com.lexiflow.wordbook.data.UserSettings
import com.lexiflow.wordbook.data.WordEntity
import com.lexiflow.wordbook.domain.LearningStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val loading: Boolean = true,
    val error: String? = null,
    val stats: LearningStats = LearningStats(),
    val featuredWord: WordEntity? = null,
    val selectedBook: BookEntity? = null,
    val moods: Map<String, String> = emptyMap()
)

data class LibraryState(
    val books: List<BookEntity> = emptyList(),
    val bookSizes: Map<Int, Int> = emptyMap(),
    val words: List<WordEntity> = emptyList(),
    val filter: String = "search",
    val query: String = "",
    val error: String? = null,
    val hasMore: Boolean = true,
    val loadingMore: Boolean = false,
    val settings: UserSettings
)

class AppViewModel(private val repository: LearningRepository) : ViewModel() {
    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard = _dashboard.asStateFlow()

    private val _library = MutableStateFlow(LibraryState(settings = repository.settings()))
    val library = _library.asStateFlow()

    init { refresh() }

    fun randomizeFeatured() = viewModelScope.launch {
        _dashboard.value = _dashboard.value.copy(featuredWord = repository.randomFeaturedWord())
    }

    fun setMood(dayKey: String, mood: String) = viewModelScope.launch {
        if (mood.isEmpty()) {
            repository.clearMood(dayKey)
            _dashboard.value = _dashboard.value.copy(moods = _dashboard.value.moods - dayKey)
        } else {
            repository.setMood(dayKey, mood)
            _dashboard.value = _dashboard.value.copy(moods = _dashboard.value.moods + (dayKey to mood))
        }
    }

    fun refresh() = viewModelScope.launch {
        runCatching {
            _dashboard.value = DashboardState(
                loading = false,
                stats = repository.stats(),
                featuredWord = repository.featuredWord(),
                selectedBook = repository.selectedBook(),
                moods = repository.allMoods()
            )
        }.getOrElse { e ->
            _dashboard.value = _dashboard.value.copy(loading = false, error = e.message ?: "加载失败")
            return@launch
        }
        loadLibrary(_library.value.filter, "")
    }

    fun loadLibrary(filter: String, query: String = "") = viewModelScope.launch {
        runCatching {
            val list = when (filter) {
                "favorites" -> repository.favoritesPaged(PAGE_SIZE, 0)
                "mistakes" -> repository.mistakesPaged(PAGE_SIZE, 0)
                else -> if (query.isBlank()) repository.myWordsPaged(PAGE_SIZE, 0) else repository.searchPaged(query, PAGE_SIZE, 0)
            }
            _library.value = _library.value.copy(
                books = repository.books(),
                bookSizes = repository.bookSizes(),
                words = list,
                filter = filter,
                query = query,
                error = null,
                hasMore = list.size >= PAGE_SIZE,
                loadingMore = false,
                settings = repository.settings()
            )
        }.getOrElse { e ->
            _library.value = _library.value.copy(error = e.message ?: "加载失败")
        }
    }

    fun loadMore() = viewModelScope.launch {
        val state = _library.value
        if (!state.hasMore || state.loadingMore) return@launch
        _library.value = state.copy(loadingMore = true)
        runCatching {
            val offset = state.words.size
            val page = when (state.filter) {
                "favorites" -> repository.favoritesPaged(PAGE_SIZE, offset)
                "mistakes" -> repository.mistakesPaged(PAGE_SIZE, offset)
                else -> if (state.query.isBlank()) repository.myWordsPaged(PAGE_SIZE, offset) else repository.searchPaged(state.query, PAGE_SIZE, offset)
            }
            _library.value = _library.value.copy(
                words = state.words + page,
                hasMore = page.size >= PAGE_SIZE,
                loadingMore = false
            )
        }.getOrElse { e ->
            _library.value = _library.value.copy(loadingMore = false, error = e.message ?: "加载失败")
        }
    }

    fun selectBook(id: Int) = viewModelScope.launch {
        repository.selectBook(id)
        refresh()
    }

    fun toggleFavorite(id: Int) = viewModelScope.launch {
        repository.toggleFavorite(id)
        loadLibrary(_library.value.filter, _library.value.query)
    }

    fun saveSettings(settings: UserSettings) {
        repository.saveSettings(settings)
        _library.value = _library.value.copy(settings = settings)
        refresh()
    }

    suspend fun importWords(json: String) = repository.importWords(json)

    suspend fun importSimpleWords(json: String): Int {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return 0
        val root = org.json.JSONArray(json)
        var imported = 0
        for (i in 0 until root.length()) {
            val item = root.getJSONObject(i)
            val word = item.optString("word").trim()
            if (word.isEmpty()) continue
            val meaning = item.optString("meaning", "")
            if (repository.enrichAndAddWord(word, meaning, apiKey)) imported++
        }
        return imported
    }

    suspend fun exportJson() = repository.exportJson()
    fun clearData() = viewModelScope.launch {
        repository.clearLearningData()
        refresh()
    }

    fun getApiKey() = repository.getApiKey()
    fun setApiKey(key: String) = repository.setApiKey(key)

    suspend fun addWord(entry: com.lexiflow.wordbook.data.DictEntry) = repository.addWord(entry)

    suspend fun deleteWord(wordId: Int) {
        repository.deleteWord(wordId)
        refresh()
    }

    companion object {
        private const val PAGE_SIZE = 50
    }

    class Factory(private val repository: LearningRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
    }
}
