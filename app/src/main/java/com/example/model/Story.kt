package com.example.model

enum class AudioType(val labelHindi: String, val iconText: String) {
    TTS("ऑडियो वाचन", "🔊")
}

data class Story(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val description: String,
    val category: String,
    val estimatedReadingMinutes: Int,
    val audioAsset: String? = null,
    val audioType: AudioType = AudioType.TTS,
    val hasAudio: Boolean = true,
    val isFeatured: Boolean = false,
    val audioDurationMs: Long = 0L,
    val paragraphs: List<String> = emptyList(),
    val contentHindi: String = ""
) {
    val fullContent: String
        get() = if (contentHindi.isNotBlank()) contentHindi else paragraphs.joinToString("\n\n")
}
