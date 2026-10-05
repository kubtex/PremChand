package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AudioLibraryScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StoryDetailReaderScreen
import com.example.ui.theme.PremchandTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PremchandTheme {
                PremchandApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremchandApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            try {
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            } catch (_: Throwable) {}
            viewModel.clearSnackbarMessage()
        }
    }

    // Full screen Reader view if active
    val activeStory = uiState.activeReaderStory
    if (activeStory != null) {
        StoryDetailReaderScreen(
            story = activeStory,
            isBookmarked = uiState.bookmarkedStoryIds.contains(activeStory.id),
            playerState = playerState,
            fontSizeSp = uiState.readerFontSizeSp,
            themeMode = uiState.readerThemeMode,
            onBackClick = { viewModel.closeStoryReader() },
            onBookmarkToggle = { viewModel.toggleBookmark(activeStory.id) },
            onPlayAudioClick = { forceTts -> viewModel.playStory(activeStory, forceTts = forceTts) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekBackward10 = { viewModel.seekBackward10() },
            onSeekForward30 = { viewModel.seekForward30() },
            onPlayPrevious = { viewModel.playPreviousStory() },
            onPlayNext = { viewModel.playNextStory() },
            onFontSizeChange = { viewModel.setReaderFontSize(it) },
            onThemeModeChange = { viewModel.setReaderThemeMode(it) },
            onSaveReadingProgress = { scrollIndex, percent ->
                viewModel.saveReadingProgress(activeStory.id, scrollIndex, percent)
            },
            initialReadingPercent = uiState.readingProgressMap[activeStory.id] ?: 0,
            snackbarHostState = snackbarHostState
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "प्रेमचंद कहानियाँ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "मुंशी प्रेमचंद — पढ़ें भी, सुनें भी",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.testTag("app_top_bar")
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Persistent Mini Player
                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onClick = { viewModel.setFullPlayerOpen(true) }
                    )

                    // Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "मुख्य पृष्ठ"
                                )
                            },
                            label = { Text("कहानियाँ") },
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        NavigationBarItem(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 1) Icons.Filled.Headphones else Icons.Outlined.Headphones,
                                    contentDescription = "ऑडियो लाइब्रेरी"
                                )
                            },
                            label = { Text("ऑडियो") },
                            modifier = Modifier.testTag("nav_tab_audio")
                        )

                        NavigationBarItem(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.bookmarkedStoryIds.isNotEmpty()) {
                                            Badge {
                                                Text(
                                                    text = "${uiState.bookmarkedStoryIds.size}",
                                                    modifier = Modifier.testTag("saved_count_badge")
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTabIndex == 2) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "सहेजे गए"
                                    )
                                }
                            },
                            label = { Text("सहेजे गए") },
                            modifier = Modifier.testTag("nav_tab_bookmarks")
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> HomeScreen(
                        uiState = uiState,
                        onStoryReadClick = { storyId -> viewModel.openStoryReader(storyId) },
                        onStoryListenClick = { story ->
                            viewModel.playStory(story)
                            viewModel.setFullPlayerOpen(true)
                        },
                        onResumeAudioClick = { storyId -> viewModel.resumeAudio(storyId) },
                        onBookmarkToggle = { storyId -> viewModel.toggleBookmark(storyId) },
                        onFilterSelect = { filter -> viewModel.setFilter(filter) },
                        onCategorySelect = { category -> viewModel.setCategory(category) },
                        onSearchQueryChange = { query -> viewModel.setSearchQuery(query) }
                    )

                    1 -> AudioLibraryScreen(
                        uiState = uiState,
                        onStoryReadClick = { storyId -> viewModel.openStoryReader(storyId) },
                        onStoryListenClick = { story ->
                            viewModel.playStory(story)
                            viewModel.setFullPlayerOpen(true)
                        },
                        onBookmarkToggle = { storyId -> viewModel.toggleBookmark(storyId) },
                        onFilterSelect = { filter -> viewModel.setFilter(filter) }
                    )

                    2 -> BookmarksScreen(
                        uiState = uiState,
                        onStoryReadClick = { storyId -> viewModel.openStoryReader(storyId) },
                        onStoryListenClick = { story ->
                            viewModel.playStory(story)
                            viewModel.setFullPlayerOpen(true)
                        },
                        onBookmarkToggle = { storyId -> viewModel.toggleBookmark(storyId) },
                        onBrowseStoriesClick = { selectedTabIndex = 0 },
                        onFilterChange = { filter -> viewModel.setSavedFilter(filter) }
                    )
                }
            }
        }
    }

    // Full Audio Player Modal Bottom Sheet
    if (uiState.isFullPlayerOpen && playerState.currentStory != null) {
        val currentPlayingStory = playerState.currentStory
        AudioPlayerDialog(
            playerState = playerState,
            isBookmarked = currentPlayingStory != null && uiState.bookmarkedStoryIds.contains(currentPlayingStory.id),
            onBookmarkToggle = {
                currentPlayingStory?.let { viewModel.toggleBookmark(it.id) }
            },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekBackward10 = { viewModel.seekBackward10() },
            onSeekForward30 = { viewModel.seekForward30() },
            onSeekToFraction = { viewModel.seekToFraction(it) },
            onPlayPrevious = { viewModel.playPreviousStory() },
            onPlayNext = { viewModel.playNextStory() },
            onSetSpeed = { viewModel.setSpeed(it) },
            onOpenReader = { storyId ->
                viewModel.setFullPlayerOpen(false)
                viewModel.openStoryReader(storyId)
            },
            onDismiss = { viewModel.setFullPlayerOpen(false) }
        )
    }
}
