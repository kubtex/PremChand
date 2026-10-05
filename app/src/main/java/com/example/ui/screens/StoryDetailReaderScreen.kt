package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlayerState
import com.example.model.AudioType
import com.example.model.Story
import com.example.ui.ReadingThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailReaderScreen(
    story: Story,
    isBookmarked: Boolean,
    playerState: PlayerState,
    fontSizeSp: Int,
    themeMode: ReadingThemeMode,
    onBackClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPlayAudioClick: (Boolean) -> Unit, // forceTts
    onTogglePlayPause: () -> Unit,
    onSeekBackward10: () -> Unit,
    onSeekForward30: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onThemeModeChange: (ReadingThemeMode) -> Unit,
    onSaveReadingProgress: (scrollIndex: Int, percent: Int) -> Unit,
    initialReadingPercent: Int = 0,
    snackbarHostState: SnackbarHostState? = null
) {
    BackHandler {
        onBackClick()
    }

    val isThisStoryPlaying = (playerState.currentStory?.id == story.id)
    
    val totalParagraphs = story.paragraphs.size.coerceAtLeast(1)
    val initialScrollIndex = if (initialReadingPercent in 1..99) {
        ((initialReadingPercent.toFloat() / 100f) * totalParagraphs).toInt().coerceIn(0, totalParagraphs)
    } else {
        0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    // Determine reader background and text colors based on selected reading theme
    val (readerBg, readerTextColor, readerHighlightColor) = when (themeMode) {
        ReadingThemeMode.PARCHMENT -> Triple(
            Color(0xFFFAF6EF),
            Color(0xFF211517),
            Color(0xFFFDE8EB)
        )
        ReadingThemeMode.SEPIA -> Triple(
            Color(0xFFF4ECD8),
            Color(0xFF35251E),
            Color(0xFFF6DCB4)
        )
        ReadingThemeMode.NIGHT -> Triple(
            Color(0xFF150C0E),
            Color(0xFFF7ECEE),
            Color(0xFF311D22)
        )
    }

    // Auto-scroll to active paragraph if audio is playing this story
    LaunchedEffect(playerState.currentChunkIndex, isThisStoryPlaying) {
        if (isThisStoryPlaying && playerState.currentChunkIndex in story.paragraphs.indices) {
            // Offset by 1 because header is item 0
            listState.animateScrollToItem(playerState.currentChunkIndex + 1)
        }
    }

    // Track scroll reading progress with debounce
    val readingProgressPercent by remember {
        derivedStateOf {
            val totalP = story.paragraphs.size.coerceAtLeast(1)
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            if (totalItems > 0 && lastVisible >= totalItems - 1) {
                100
            } else if (listState.firstVisibleItemIndex == 0) {
                initialReadingPercent.coerceAtLeast(5).coerceAtMost(100)
            } else {
                val currentP = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                val calc = ((currentP.toFloat() / totalP) * 100).toInt()
                calc.coerceIn(5, 99)
            }
        }
    }

    val currentProgress by rememberUpdatedState(readingProgressPercent)
    val currentIndex by rememberUpdatedState(listState.firstVisibleItemIndex)

    LaunchedEffect(readingProgressPercent) {
        if (readingProgressPercent > 0) {
            delay(800) // Debounce
            onSaveReadingProgress(currentIndex, currentProgress)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (currentProgress > 0) {
                onSaveReadingProgress(currentIndex, currentProgress)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = story.titleHindi,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएँ"
                        )
                    }
                },
                actions = {
                    // Font Size Cycle
                    IconButton(
                        onClick = {
                            val nextSize = if (fontSizeSp >= 24) 16 else fontSizeSp + 2
                            onFontSizeChange(nextSize)
                        },
                        modifier = Modifier.testTag("reader_font_size_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = "फ़ॉन्ट आकार"
                        )
                    }

                    // Reading Theme Toggle (Parchment -> Sepia -> Night)
                    IconButton(
                        onClick = {
                            val nextTheme = when (themeMode) {
                                ReadingThemeMode.PARCHMENT -> ReadingThemeMode.SEPIA
                                ReadingThemeMode.SEPIA -> ReadingThemeMode.NIGHT
                                ReadingThemeMode.NIGHT -> ReadingThemeMode.PARCHMENT
                            }
                            onThemeModeChange(nextTheme)
                        },
                        modifier = Modifier.testTag("reader_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "रंग थीम"
                        )
                    }

                    // Bookmark
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "बुकमार्क हटाएं" else "बुकमार्क करें",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = readerBg,
                    titleContentColor = readerTextColor,
                    navigationIconContentColor = readerTextColor,
                    actionIconContentColor = readerTextColor
                )
            )
        },
        containerColor = readerBg
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("story_reader_scroll"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Specified Top Audio Header UI
            item {
                StoryAudioHeaderCard(
                    story = story,
                    isThisStoryPlaying = isThisStoryPlaying,
                    playerState = playerState,
                    onPlayAudioClick = onPlayAudioClick,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekBackward10 = onSeekBackward10,
                    onSeekForward30 = onSeekForward30,
                    onPlayPrevious = onPlayPrevious,
                    onPlayNext = onPlayNext
                )
            }

            // Paragraphs of the classic story
            itemsIndexed(story.paragraphs) { index, paragraph ->
                val isCurrentParagraphPlaying = (isThisStoryPlaying && playerState.currentChunkIndex == index)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrentParagraphPlaying) readerHighlightColor else Color.Transparent
                        )
                        .padding(horizontal = if (isCurrentParagraphPlaying) 12.dp else 0.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.6f).sp,
                            color = readerTextColor
                        ),
                        modifier = Modifier.testTag("paragraph_$index")
                    )
                }
            }

            // End of Story Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "— मुंशी प्रेमचंद —",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = readerTextColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "कालजयी हिंदी साहित्य",
                        style = MaterialTheme.typography.labelSmall,
                        color = readerTextColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Implements the explicit layout requested in prompt:
 * ┌─────────────────────────────┐
 * │ ईदगाह                       │
 * │ मुंशी प्रेमचंद              │
 * │                             │
 * │ 🔊 कहानी सुनें              │
 * └─────────────────────────────┘
 * And when audio starts:
 * ▶ Playing
 * Story title
 * Progress: ██████████░░░░ 62%
 * Controls: ⏮ Previous, ⏸ Pause / ▶ Play, ⏭ Next
 * Additional: ⏪ 10 sec, ⏩ 30 sec
 */
@Composable
private fun StoryAudioHeaderCard(
    story: Story,
    isThisStoryPlaying: Boolean,
    playerState: PlayerState,
    onPlayAudioClick: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBackward10: () -> Unit,
    onSeekForward30: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("story_audio_header_box"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Story Title & Author
            Text(
                text = story.titleHindi,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${story.titleEnglish} • ${story.estimatedReadingMinutes} मिनट • ${story.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!isThisStoryPlaying) {
                // Unified Clean Action: 🔊 कहानी सुनें (Hindi TTS)
                Button(
                    onClick = { onPlayAudioClick(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_audio_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🔊 कहानी सुनें (ऑडियो कहानी)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // When Audio Starts:
                // ▶ Playing
                // Story Title
                // Progress: ██████████░░░░ 62%
                // Controls: ⏮ Previous, ⏸ Pause / ▶ Play, ⏭ Next
                // Additional: ⏪ 10 sec, ⏩ 30 sec
                val progressFraction = if (playerState.durationMs > 0) {
                    (playerState.positionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val percent = (progressFraction * 100).toInt()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (playerState.isPlaying) "▶ Playing" else "⏸ Paused",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " • 🔊 ऑडियो कहानी",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "प्रगति (Progress): $percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (playerState.audioType == AudioType.TTS && playerState.totalChunks > 0) {
                        Text(
                            text = "अनुच्छेद ${playerState.currentChunkIndex + 1}/${playerState.totalChunks}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Controls: ⏪ 10s, ⏮ Previous, ⏸ Pause / ▶ Play, ⏭ Next, ⏩ 10s
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⏪ 10s (Matching round icon with 10)
                    IconButton(
                        onClick = onSeekBackward10,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("in_story_seek_back_10")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "१० सेकंड पीछे",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // ⏮ Previous
                    IconButton(
                        onClick = onPlayPrevious,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("in_story_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "पिछली कहानी",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // ⏸ Pause / ▶ Play
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("in_story_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "रोकें" else "चलाएँ",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // ⏭ Next
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("in_story_next")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "अगली कहानी",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // ⏩ 10s (Matching round icon with 10)
                    IconButton(
                        onClick = onSeekForward30,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("in_story_seek_forward_30")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "१० सेकंड आगे",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Warning if TTS unavailable
            if (!playerState.isHindiTtsAvailable && playerState.ttsWarningMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playerState.ttsWarningMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
