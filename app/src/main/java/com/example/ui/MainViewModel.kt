package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackManager
import com.example.audio.PlayerState
import com.example.data.PremchandStoryCatalog
import com.example.data.db.AppDatabase
import com.example.data.db.AudioProgressEntity
import com.example.data.db.ReadingProgressEntity
import com.example.data.repository.StoryRepository
import com.example.model.AudioType
import com.example.model.Story
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AudioFilter(val label: String) {
    ALL("सभी"),
    BOTH("📖 + 🎧 दोनों"),
    AUDIO_AVAILABLE("🎧 ऑडियो उपलब्ध"),
    READ_ONLY("📖 केवल पढ़ें")
}

enum class ReadingThemeMode(val title: String) {
    PARCHMENT("कागज़"),
    SEPIA("बादामी"),
    NIGHT("रात्रि")
}

data class ContinueListeningItem(
    val story: Story,
    val progressEntity: AudioProgressEntity,
    val percentCompleted: Int
)

data class ContinueReadingItem(
    val story: Story,
    val progressEntity: ReadingProgressEntity
)

enum class SavedFilter(val label: String) {
    ALL("सभी सहेजे गए"),
    AUDIO_ONLY("🎧 ऑडियो सहित"),
    READ_ONLY("📖 पढ़ने हेतु")
}

data class MainUiState(
    val allStories: List<Story> = emptyList(),
    val filteredStories: List<Story> = emptyList(),
    val featuredStory: Story? = null,
    val popularAudioStories: List<Story> = emptyList(),
    val continueListeningList: List<ContinueListeningItem> = emptyList(),
    val continueReadingList: List<ContinueReadingItem> = emptyList(),
    val bookmarkedStoryIds: Set<String> = emptySet(),
    val selectedCategory: String? = null,
    val selectedFilter: AudioFilter = AudioFilter.ALL,
    val savedFilter: SavedFilter = SavedFilter.ALL,
    val searchQuery: String = "",
    val activeReaderStory: Story? = null,
    val readerFontSizeSp: Int = 18,
    val readerThemeMode: ReadingThemeMode = ReadingThemeMode.PARCHMENT,
    val isFullPlayerOpen: Boolean = false,
    val snackbarMessage: String? = null,
    val readingProgressMap: Map<String, Int> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = StoryRepository(database.storyDao(), application)
    val playbackManager = AudioPlaybackManager.getInstance(application).apply {
        setRepository(repository)
    }

    val playerState: StateFlow<PlayerState> = playbackManager.playerState

    private val _uiState = MutableStateFlow(
        MainUiState(
            allStories = repository.allStories,
            filteredStories = repository.allStories,
            featuredStory = repository.allStories.find { it.id == "eidgah" } ?: repository.allStories.firstOrNull(),
            popularAudioStories = repository.allStories.filter { it.isFeatured }
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            combine(
                repository.audioProgressList,
                repository.readingProgressList,
                repository.bookmarksList
            ) { audioList, readingList, bookmarksList ->
                Triple(audioList, readingList, bookmarksList)
            }.catch { e ->
                android.util.Log.e("MainViewModel", "Error in database observation flow", e)
            }.collect { (audioList, readingList, bookmarksList) ->
                try {
                    val allStories = repository.allStories

                    // Continue listening items (stories with audio progress not finished or recently played)
                    val continueListening = audioList.mapNotNull { progress ->
                        val story = allStories.find { it.id == progress.storyId }
                        if (story != null) {
                            val duration = if (progress.durationMs > 0) progress.durationMs else story.audioDurationMs
                            val percent = if (duration > 0) {
                                ((progress.positionMs.toFloat() / duration) * 100).toInt().coerceIn(1, 100)
                            } else {
                                0
                            }
                            ContinueListeningItem(story, progress, percent)
                        } else null
                    }

                    // Continue reading items
                    val continueReading = readingList.mapNotNull { progress ->
                        val story = allStories.find { it.id == progress.storyId }
                        if (story != null && progress.percentCompleted > 0) {
                            ContinueReadingItem(story, progress)
                        } else null
                    }

                    // Dedicated Room bookmarks
                    val bookmarked = bookmarksList.map { it.storyId }.toSet()

                    // Reading progress mapping by storyId
                    val progressMap = readingList.associate { it.storyId to it.percentCompleted.coerceIn(0, 100) }

                    _uiState.update { current ->
                        current.copy(
                            continueListeningList = continueListening,
                            continueReadingList = continueReading,
                            bookmarkedStoryIds = bookmarked,
                            readingProgressMap = progressMap
                        )
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("MainViewModel", "Error updating progress state", t)
                }
            }
        }
    }

    fun setFilter(filter: AudioFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilters()
    }

    fun setCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        var list = current.allStories

        // Category filter
        if (!current.selectedCategory.isNullOrBlank()) {
            list = list.filter { it.category == current.selectedCategory }
        }

        // Audio mode filter
        list = when (current.selectedFilter) {
            AudioFilter.ALL -> list
            AudioFilter.BOTH -> list.filter { it.hasAudio }
            AudioFilter.AUDIO_AVAILABLE -> list.filter { it.hasAudio }
            AudioFilter.READ_ONLY -> list
        }

        // Search filter
        if (current.searchQuery.isNotBlank()) {
            val q = current.searchQuery.trim().lowercase()
            list = list.filter {
                it.titleHindi.lowercase().contains(q) ||
                        it.titleEnglish.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }

        _uiState.update { it.copy(filteredStories = list) }
    }

    fun playStory(story: Story, forceTts: Boolean = true) {
        viewModelScope.launch {
            try {
                val fullStory = if (story.paragraphs.isEmpty()) {
                    repository.getStoryById(story.id) ?: story
                } else {
                    story
                }
                val progress = try {
                    repository.getAudioProgress(fullStory.id)
                } catch (_: Throwable) {
                    null
                }
                val resumePos = if (progress != null && !progress.isCompleted) progress.positionMs else 0L
                playbackManager.playStory(fullStory, forceTts = true, resumePositionMs = resumePos)
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Error in playStory", t)
            }
        }
    }

    fun resumeAudio(storyId: String) {
        viewModelScope.launch {
            try {
                val story = repository.getStoryById(storyId) ?: return@launch
                val progress = try {
                    repository.getAudioProgress(storyId)
                } catch (_: Throwable) {
                    null
                }
                val resumePos = progress?.positionMs ?: 0L
                playbackManager.playStory(story, resumePositionMs = resumePos)
                _uiState.update { it.copy(isFullPlayerOpen = true) }
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Error in resumeAudio", t)
            }
        }
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekForward30() {
        playbackManager.seekForward30()
    }

    fun seekBackward10() {
        playbackManager.seekBackward10()
    }

    fun seekToFraction(fraction: Float) {
        playbackManager.seekToFraction(fraction)
    }

    fun setSpeed(speed: Float) {
        playbackManager.setSpeed(speed)
    }

    fun playNextStory() {
        playbackManager.playNextStory()
    }

    fun playPreviousStory() {
        playbackManager.playPreviousStory()
    }

    fun setFullPlayerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFullPlayerOpen = isOpen) }
    }

    fun openStoryReader(storyId: String) {
        val story = repository.getStoryById(storyId)
        val currentProgress = _uiState.value.readingProgressMap[storyId] ?: 0
        if (currentProgress == 0) {
            saveReadingProgress(storyId, 0, 5)
        }
        _uiState.update { it.copy(activeReaderStory = story) }
    }

    fun closeStoryReader() {
        _uiState.update { it.copy(activeReaderStory = null) }
    }

    private var saveReadingProgressJob: Job? = null

    fun saveReadingProgress(storyId: String, scrollIndex: Int, percent: Int) {
        val safePercent = percent.coerceIn(0, 100)

        // Immediate in-memory UI state update
        _uiState.update { current ->
            val updatedMap = current.readingProgressMap + (storyId to safePercent)
            val story = repository.getStoryById(storyId)
            val existingIndex = current.continueReadingList.indexOfFirst { it.story.id == storyId }
            val updatedContinueList = if (story != null && safePercent > 0) {
                val entity = com.example.data.db.ReadingProgressEntity(
                    storyId = storyId,
                    scrollIndex = scrollIndex,
                    scrollOffset = 0,
                    percentCompleted = safePercent,
                    lastReadTimestamp = System.currentTimeMillis()
                )
                val newItem = ContinueReadingItem(story, entity)
                if (existingIndex >= 0) {
                    current.continueReadingList.toMutableList().apply { set(existingIndex, newItem) }
                } else {
                    listOf(newItem) + current.continueReadingList
                }
            } else {
                current.continueReadingList
            }

            current.copy(
                readingProgressMap = updatedMap,
                continueReadingList = updatedContinueList
            )
        }

        saveReadingProgressJob?.cancel()
        saveReadingProgressJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveReadingProgress(storyId, scrollIndex, 0, safePercent)
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Error saving reading progress", t)
            }
        }
    }

    fun toggleBookmark(storyId: String) {
        val isCurrentBookmarked = _uiState.value.bookmarkedStoryIds.contains(storyId)
        val newStatus = !isCurrentBookmarked
        val storyTitle = repository.getStoryById(storyId)?.titleHindi ?: "कहानी"
        val message = if (newStatus) {
            "‘$storyTitle’ सहेजे गए अनुभाग में जोड़ी गई (Saved)"
        } else {
            "‘$storyTitle’ सहेजे गए अनुभाग से हटाई गई"
        }

        // Optimistic UI state update
        _uiState.update { current ->
            val updatedBookmarks = if (newStatus) {
                current.bookmarkedStoryIds + storyId
            } else {
                current.bookmarkedStoryIds - storyId
            }
            current.copy(
                bookmarkedStoryIds = updatedBookmarks,
                snackbarMessage = message
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.toggleBookmark(storyId, newStatus)
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Error saving bookmark to DB", t)
            }
        }
    }

    fun setSavedFilter(filter: SavedFilter) {
        _uiState.update { it.copy(savedFilter = filter) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setReaderFontSize(size: Int) {
        _uiState.update { it.copy(readerFontSizeSp = size.coerceIn(14, 28)) }
    }

    fun setReaderThemeMode(mode: ReadingThemeMode) {
        _uiState.update { it.copy(readerThemeMode = mode) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
