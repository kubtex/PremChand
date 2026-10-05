package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM audio_progress ORDER BY lastPlayedTimestamp DESC")
    fun getAllAudioProgress(): Flow<List<AudioProgressEntity>>

    @Query("SELECT * FROM audio_progress WHERE storyId = :storyId")
    suspend fun getAudioProgress(storyId: String): AudioProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAudioProgress(progress: AudioProgressEntity)

    @Query("SELECT * FROM reading_progress ORDER BY lastReadTimestamp DESC")
    fun getAllReadingProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE storyId = :storyId")
    suspend fun getReadingProgress(storyId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)

    @Query("UPDATE reading_progress SET isBookmarked = :isBookmarked WHERE storyId = :storyId")
    suspend fun updateBookmark(storyId: String, isBookmarked: Boolean)

    @Query("SELECT * FROM bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE storyId = :storyId)")
    fun isStoryBookmarked(storyId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE storyId = :storyId")
    suspend fun removeBookmark(storyId: String)
}
