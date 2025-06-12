package com.mbougar.swapware.ui.screens.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.viewmodel.MessagesUiState
import com.mbougar.swapware.viewmodel.MessagesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Messages") })
        }
    ) { paddingValues ->
        MessagesContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            navController = navController,
            onRefresh = { viewModel.refresh() }
        )
    }
}

@Composable
fun MessagesContent(
    modifier: Modifier = Modifier,
    uiState: MessagesUiState,
    navController: NavController,
    onRefresh: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.conversations.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) { Text("Retry") }
                }
            }
            uiState.conversations.isEmpty() -> {
                Text("You have no messages yet.", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.conversations, key = { it.id }) { conversation ->
                        val otherParticipantDisplayName = remember(conversation.participantIds, conversation.participantDisplayNames, uiState.currentUserId) {
                            val otherIdIndex = conversation.participantIds.indexOfFirst { id -> id != uiState.currentUserId }
                            if (otherIdIndex != -1 && otherIdIndex < conversation.participantDisplayNames.size) {
                                conversation.participantDisplayNames[otherIdIndex]
                            } else {
                                "Unknown User"
                            }
                        }

                        ConversationItem(
                            conversation = conversation,
                            otherParticipantDisplayName = otherParticipantDisplayName,
                            onClick = {
                                navController.navigate(
                                    Screen.ChatDetail.createRoute(
                                        conversationId = conversation.id,
                                        otherUserDisplayName = otherParticipantDisplayName,
                                        adTitle = conversation.adTitle
                                    )
                                )
                            }
                        )
                        Divider()
                    }
                }
            }
        }
        if (uiState.isLoading && uiState.conversations.isNotEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    otherParticipantDisplayName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = otherParticipantDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(conversation.lastMessageTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ad: ${conversation.adTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.lastMessageSnippet ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (conversation.lastMessageSnippet != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    // Dependiendo de l tiempo pasado mostramos el tiempo de forma diferente (como whatsap)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}