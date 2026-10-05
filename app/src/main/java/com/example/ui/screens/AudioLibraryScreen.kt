package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioType
import com.example.model.Story
import com.example.ui.AudioFilter
import com.example.ui.MainUiState
import com.example.ui.components.StoryCard

@Composable
fun AudioLibraryScreen(
    uiState: MainUiState,
    onStoryReadClick: (String) -> Unit,
    onStoryListenClick: (Story) -> Unit,
    onBookmarkToggle: (String) -> Unit,
    onFilterSelect: (AudioFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_library_screen"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Info Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔊", fontSize = 28.sp)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Column {
                            Text(
                                text = "ऑडियो वाचन लाइब्रेरी",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "१००% ऑफ़लाइन सुनने की सुविधा",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "मुंशी प्रेमचंद की समस्त कहानियों का आनंद लें। स्पष्ट हिंदी आवाज़ में किसी भी कहानी को हैंड्स-फ़्री कभी भी सुनें।",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "🎧 समस्त ऑडियो कहानियाँ (${uiState.filteredStories.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Story List
        items(uiState.filteredStories, key = { it.id }) { story ->
            StoryCard(
                story = story,
                isBookmarked = uiState.bookmarkedStoryIds.contains(story.id),
                readingProgressPercent = uiState.readingProgressMap[story.id] ?: 0,
                onReadClick = { onStoryReadClick(story.id) },
                onListenClick = { onStoryListenClick(story) },
                onBookmarkToggle = { onBookmarkToggle(story.id) }
            )
        }
    }
}
