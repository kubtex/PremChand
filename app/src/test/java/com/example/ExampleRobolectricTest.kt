package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PremchandStoryCatalog
import com.example.model.AudioType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("प्रेमचंद कहानियाँ", appName)
    }

    @Test
    fun `story catalog contains classic stories with offline audio`() {
        val stories = PremchandStoryCatalog.stories
        assertTrue(stories.isNotEmpty())

        val eidgah = PremchandStoryCatalog.getStoryById("eidgah")
        assertNotNull(eidgah)
        assertEquals("ईदगाह", eidgah?.titleHindi)
        assertEquals(AudioType.TTS, eidgah?.audioType)
        assertTrue(eidgah?.hasAudio == true)

        val poosKiRaat = PremchandStoryCatalog.getStoryById("poos_ki_raat")
        assertNotNull(poosKiRaat)
        assertEquals(AudioType.TTS, poosKiRaat?.audioType)

        val ttsStories = PremchandStoryCatalog.getTtsStories()
        assertTrue(ttsStories.isNotEmpty())
    }

    @Test
    fun `story catalog loads all stories from premchand website assets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PremchandStoryCatalog.init(context)
        val allStories = PremchandStoryCatalog.stories
        assertTrue(allStories.size >= 148)

        val grihaDaah = PremchandStoryCatalog.getStoryById("griha_daah")
        assertNotNull(grihaDaah)
        assertEquals("गृह दाह", grihaDaah?.titleHindi)
        assertTrue(grihaDaah?.paragraphs?.isNotEmpty() == true)

        val mantra2 = PremchandStoryCatalog.getStoryById("mantra_2")
        assertNotNull(mantra2)
        assertEquals("मंत्र", mantra2?.titleHindi)
    }

    @Test
    fun `room database bookmark persistence test`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.db.AppDatabase.getDatabase(context)
        val dao = db.storyDao()
        val repo = com.example.data.repository.StoryRepository(dao)

        repo.toggleBookmark("eidgah", true)
        val isBookmarkedFirst = dao.isStoryBookmarked("eidgah").first()
        assertTrue(isBookmarkedFirst)

        val reading = dao.getReadingProgress("eidgah")
        assertTrue(reading?.isBookmarked == true)

        repo.toggleBookmark("eidgah", false)
        val isBookmarkedAfter = dao.isStoryBookmarked("eidgah").first()
        assertFalse(isBookmarkedAfter)

        val updatedReading = dao.getReadingProgress("eidgah")
        assertTrue(updatedReading?.isBookmarked == false)
    }
}
