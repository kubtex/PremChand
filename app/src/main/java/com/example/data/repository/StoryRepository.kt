package com.example.data.repository

import com.example.data.PremchandStoryCatalog
import com.example.data.db.AudioProgressEntity
import com.example.data.db.ReadingProgressEntity
import com.example.data.db.StoryDao
import com.example.model.Story
import kotlinx.coroutines.flow.Flow

class StoryRepository(
    private val storyDao: StoryDao,
    context: android.content.Context? = null
) {

    init {
        context?.let { PremchandStoryCatalog.init(it) }
    }

    val allStories: List<Story>
        get() = PremchandStoryCatalog.stories

    val audioProgressList: Flow<List<AudioProgressEntity>> = storyDao.getAllAudioProgress()

    val readingProgressList: Flow<List<ReadingProgressEntity>> = storyDao.getAllReadingProgress()

    val bookmarksList: Flow<List<com.example.data.db.BookmarkEntity>> = storyDao.getAllBookmarks()

    fun getStoryById(id: String): Story? = PremchandStoryCatalog.getStoryById(id)

    suspend fun getAudioProgress(storyId: String): AudioProgressEntity? {
        return storyDao.getAudioProgress(storyId)
    }

    suspend fun saveAudioProgress(
        storyId: String,
        positionMs: Long,
        durationMs: Long,
        speed: Float = 1.0f,
        isCompleted: Boolean = false
    ) {
        val entity = AudioProgressEntity(
            storyId = storyId,
            positionMs = positionMs,
            durationMs = durationMs,
            lastPlayedTimestamp = System.currentTimeMillis(),
            isCompleted = isCompleted,
            playbackSpeed = speed
        )
        storyDao.saveAudioProgress(entity)
    }

    suspend fun getReadingProgress(storyId: String): ReadingProgressEntity? {
        return storyDao.getReadingProgress(storyId)
    }

    suspend fun saveReadingProgress(
        storyId: String,
        scrollIndex: Int,
        scrollOffset: Int,
        percentCompleted: Int
    ) {
        val existing = storyDao.getReadingProgress(storyId)
        val entity = ReadingProgressEntity(
            storyId = storyId,
            scrollIndex = scrollIndex,
            scrollOffset = scrollOffset,
            percentCompleted = percentCompleted,
            lastReadTimestamp = System.currentTimeMillis(),
            isBookmarked = existing?.isBookmarked ?: false
        )
        storyDao.saveReadingProgress(entity)
    }

    suspend fun toggleBookmark(storyId: String, isBookmarked: Boolean) {
        try {
            if (isBookmarked) {
                storyDao.addBookmark(com.example.data.db.BookmarkEntity(storyId = storyId, bookmarkedAt = System.currentTimeMillis()))
            } else {
                storyDao.removeBookmark(storyId)
            }
            val existing = storyDao.getReadingProgress(storyId)
            if (existing != null) {
                storyDao.updateBookmark(storyId, isBookmarked)
            } else {
                storyDao.saveReadingProgress(
                    ReadingProgressEntity(
                        storyId = storyId,
                        isBookmarked = isBookmarked
                    )
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("StoryRepository", "Error in toggleBookmark", t)
        }
    }
}
