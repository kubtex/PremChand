package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.PremchandStoryCatalog
import com.example.data.repository.StoryRepository
import com.example.model.AudioType
import com.example.model.Story
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class PlayerState(
    val currentStory: Story? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val audioType: AudioType? = AudioType.TTS,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val activeParagraphText: String = "",
    val isHindiTtsAvailable: Boolean = true,
    val ttsWarningMessage: String? = null
)

class AudioPlaybackManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var repository: StoryRepository? = null

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isHindiAvailable = true

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        setupTts()
    }

    fun setRepository(repo: StoryRepository) {
        this.repository = repo
    }

    private fun setupTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        val hindiLocale = Locale.forLanguageTag("hi-IN")
                        val langResult = tts?.setLanguage(hindiLocale)
                        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            val generalHindi = Locale("hi")
                            val fallbackResult = tts?.setLanguage(generalHindi)
                            isHindiAvailable = fallbackResult != TextToSpeech.LANG_MISSING_DATA &&
                                    fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED
                        } else {
                            isHindiAvailable = true
                        }
                        isTtsInitialized = true

                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _playerState.update { it.copy(isPlaying = true) }
                            }

                            override fun onDone(utteranceId: String?) {
                                scope.launch {
                                    val state = _playerState.value
                                    val story = state.currentStory
                                    if (story != null) {
                                        val nextChunk = state.currentChunkIndex + 1
                                        val chunks = if (story.paragraphs.isNotEmpty()) story.paragraphs else listOf(story.description)
                                        if (nextChunk < chunks.size) {
                                            playTtsChunk(story, nextChunk)
                                        } else {
                                            _playerState.update {
                                                it.copy(isPlaying = false, currentChunkIndex = chunks.size - 1)
                                            }
                                            saveCurrentProgress(isCompleted = true)
                                        }
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _playerState.update { it.copy(isPlaying = false) }
                            }
                        })

                        _playerState.update {
                            it.copy(
                                isHindiTtsAvailable = isHindiAvailable,
                                ttsWarningMessage = if (!isHindiAvailable) "हिंदी आवाज़ उपलब्ध नहीं है।" else null
                            )
                        }
                    } else {
                        isHindiAvailable = false
                        isTtsInitialized = false
                    }
                } catch (t: Throwable) {
                    Log.e("AudioPlaybackManager", "Error inside TTS callback", t)
                    isHindiAvailable = false
                    isTtsInitialized = false
                }
            }
        } catch (t: Throwable) {
            Log.e("AudioPlaybackManager", "Error initializing TTS", t)
            isHindiAvailable = false
            isTtsInitialized = false
        }
    }

    fun playStory(story: Story, forceTts: Boolean = true, resumePositionMs: Long? = null) {
        try {
            stopInternal()
            playTtsAudio(story, resumeChunkIndex = (resumePositionMs?.toInt() ?: 0))
        } catch (t: Throwable) {
            Log.e("AudioPlaybackManager", "Error in playStory", t)
        }
    }

    private fun playTtsAudio(story: Story, resumeChunkIndex: Int = 0) {
        try {
            val paragraphs = if (story.paragraphs.isNotEmpty()) {
                story.paragraphs
            } else {
                PremchandStoryCatalog.getParagraphsForStory(story.id, appContext).ifEmpty {
                    listOf(story.description)
                }
            }

            val fullStory = story.copy(paragraphs = paragraphs)
            val startIndex = resumeChunkIndex.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))

            _playerState.update {
                it.copy(
                    currentStory = fullStory,
                    audioType = AudioType.TTS,
                    currentChunkIndex = startIndex,
                    totalChunks = paragraphs.size,
                    durationMs = (paragraphs.size * 10000L),
                    positionMs = (startIndex * 10000L),
                    activeParagraphText = paragraphs.getOrNull(startIndex) ?: "",
                    isPlaying = true,
                    ttsWarningMessage = null
                )
            }

            playTtsChunk(fullStory, startIndex)
        } catch (t: Throwable) {
            Log.e("AudioPlaybackManager", "Error in playTtsAudio", t)
        }
    }

    private fun playTtsChunk(story: Story, chunkIndex: Int) {
        try {
            val chunks = story.paragraphs.ifEmpty { listOf(story.description) }
            if (chunkIndex in chunks.indices) {
                val rawText = chunks[chunkIndex]
                val text = if (rawText.length > 3500) rawText.take(3500) else rawText
                val speed = _playerState.value.playbackSpeed
                if (tts == null) {
                    setupTts()
                }
                try {
                    tts?.setSpeechRate(speed)
                } catch (_: Throwable) {}

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "premchand_${story.id}_$chunkIndex")
                }
                _playerState.update {
                    it.copy(
                        currentChunkIndex = chunkIndex,
                        activeParagraphText = text,
                        positionMs = (chunkIndex * 10000L),
                        durationMs = (chunks.size * 10000L),
                        isPlaying = true
                    )
                }
                try {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "premchand_${story.id}_$chunkIndex")
                } catch (t: Throwable) {
                    Log.e("AudioPlaybackManager", "Error calling tts.speak", t)
                }
            }
        } catch (t: Throwable) {
            Log.e("AudioPlaybackManager", "Error in playTtsChunk", t)
        }
    }

    fun togglePlayPause() {
        val state = _playerState.value
        val story = state.currentStory ?: return

        if (state.isPlaying) {
            try {
                tts?.stop()
            } catch (_: Throwable) {}
            _playerState.update { it.copy(isPlaying = false) }
            saveCurrentProgress()
        } else {
            playTtsChunk(story, state.currentChunkIndex)
        }
    }

    fun seekForward30() {
        val state = _playerState.value
        val story = state.currentStory ?: return
        if (state.totalChunks > 0) {
            val nextIndex = (state.currentChunkIndex + 1).coerceAtMost(state.totalChunks - 1)
            playTtsChunk(story, nextIndex)
        }
    }

    fun seekBackward10() {
        val state = _playerState.value
        val story = state.currentStory ?: return
        if (state.totalChunks > 0) {
            val prevIndex = (state.currentChunkIndex - 1).coerceAtLeast(0)
            playTtsChunk(story, prevIndex)
        }
    }

    fun seekToFraction(fraction: Float) {
        val state = _playerState.value
        val story = state.currentStory ?: return
        if (state.totalChunks > 0) {
            val targetIndex = (fraction * (state.totalChunks - 1)).toInt().coerceIn(0, state.totalChunks - 1)
            playTtsChunk(story, targetIndex)
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.update { it.copy(playbackSpeed = speed) }
        try {
            tts?.setSpeechRate(speed)
        } catch (_: Throwable) {}
    }

    fun playNextStory() {
        val current = _playerState.value.currentStory ?: return
        val allStories = PremchandStoryCatalog.stories
        val currentIndex = allStories.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < allStories.size - 1) {
            playStory(allStories[currentIndex + 1])
        }
    }

    fun playPreviousStory() {
        val current = _playerState.value.currentStory ?: return
        val allStories = PremchandStoryCatalog.stories
        val currentIndex = allStories.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playStory(allStories[currentIndex - 1])
        }
    }

    private fun stopInternal() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
        _playerState.update { it.copy(isPlaying = false) }
    }

    private fun saveCurrentProgress(isCompleted: Boolean = false) {
        val state = _playerState.value
        val story = state.currentStory ?: return
        val repo = repository ?: return

        scope.launch(Dispatchers.IO) {
            val position = state.currentChunkIndex.toLong()
            repo.saveAudioProgress(
                storyId = story.id,
                positionMs = position,
                durationMs = state.durationMs,
                speed = state.playbackSpeed,
                isCompleted = isCompleted
            )
        }
    }

    fun release() {
        saveCurrentProgress()
        stopInternal()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlaybackManager? = null

        fun getInstance(context: Context): AudioPlaybackManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlaybackManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
