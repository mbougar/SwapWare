package com.mbougar.swapware.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.viewmodel.ChatDetailViewModel
import com.mbougar.swapware.viewmodel.ChatListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    otherUserEmail: String,
    adTitle: String,
    navController: NavController,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.chatListItems.size) {
        if (uiState.chatListItems.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.chatListItems.lastIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherUserEmail, style = MaterialTheme.typography.titleMedium)
                        Text(adTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    MessageList(
                        chatItems = uiState.chatListItems,
                        currentUserId = uiState.currentUserId ?: "",
                        listState = listState
                    )
                }
            }

            MessageInput(
                inputText = uiState.inputText,
                onTextChange = viewModel::onInputTextChanged,
                onSendClick = {
                    viewModel.sendMessage()
                    keyboardController?.hide()
                },
                isSending = uiState.isSending,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
fun MessageList(
    chatItems: List<ChatListItem>,
    currentUserId: String,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    ) {
        items(chatItems, key = { item ->
            when (item) {
                is ChatListItem.MessageItem -> "msg_${item.message.id}"
                is ChatListItem.DateSeparatorItem -> "date_${item.date}"
            }
        }) { chatItem ->
            when (chatItem) {
                is ChatListItem.MessageItem -> {
                    MessageBubble(
                        message = chatItem.message,
                        isCurrentUser = chatItem.message.senderId == currentUserId,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is ChatListItem.DateSeparatorItem -> {
                    DateSeparator(date = chatItem.date)
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isCurrentUser) 16.dp else 0.dp,
        bottomEnd = if (isCurrentUser) 0.dp else 16.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                start = if (isCurrentUser) 48.dp else 0.dp,
                end = if (isCurrentUser) 0.dp else 48.dp
            )
    ) {
        Column(
            modifier = Modifier.align(alignment)
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(bubbleShape),
                color = backgroundColor,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            message.timestamp?.let {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(tonalElevation = 3.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (inputText.isNotBlank() && !isSending) onSendClick() }
                ),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}