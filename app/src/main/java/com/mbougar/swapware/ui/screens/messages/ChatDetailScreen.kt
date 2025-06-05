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
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
    otherUserDisplayName: String,
    adTitle: String,
    navController: NavController,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val ad = uiState.adDetails
    val conversation = uiState.conversationDetails
    val currentUserId = uiState.currentUserId

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
                        Text(otherUserDisplayName, style = MaterialTheme.typography.titleMedium)
                        Text(adTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ad != null && currentUserId == ad.sellerId &&
                        conversation != null && !conversation.adIsSoldInThisConversation && !ad.isSold) {
                        IconButton(onClick = { viewModel.markAdAsSoldToOtherUser() }) {
                            Icon(Icons.Filled.Sell, contentDescription = "Mark as Sold")
                        }
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
                if (uiState.isLoading && uiState.chatListItems.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    MessageList(
                        chatItems = uiState.chatListItems,
                        currentUserId = uiState.currentUserId ?: "",
                        listState = listState
                    )
                }
            }

            if (ad != null && currentUserId != null && conversation != null && conversation.adIsSoldInThisConversation) {
                val otherUserIdInChat = conversation.participantIds.find { it != currentUserId }

                if (otherUserIdInChat != null) {
                    val sellerId = ad.sellerId
                    val buyerId = conversation.adSoldToParticipantId

                    val userToRateId: String?
                    val canRate: Boolean

                    if (currentUserId == sellerId && buyerId == otherUserIdInChat && !conversation.sellerRatedBuyerForAd) {
                        userToRateId = buyerId
                        canRate = true
                    } else if (currentUserId == buyerId && sellerId == otherUserIdInChat && !conversation.buyerRatedSellerForAd) {
                        userToRateId = sellerId
                        canRate = true
                    } else {
                        userToRateId = null
                        canRate = false
                    }

                    if (canRate && userToRateId != null) {
                        val otherUserToRateDisplayName = if(userToRateId == uiState.otherUserDisplayName){
                            conversation.participantDisplayNames.getOrNull(conversation.participantIds.indexOf(userToRateId)) ?: "User"
                        } else {
                            "User"
                        }

                        RateUserBar(
                            userNameToRate = otherUserToRateDisplayName,
                            onClickRate = { viewModel.onAttemptToRateUser(userToRateId) }
                        )
                    } else if (conversation.sellerRatedBuyerForAd && conversation.buyerRatedSellerForAd) {
                        Text("Transaction complete. Both users rated.",
                            modifier = Modifier.padding(8.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(8.dp),
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall
                        )
                    }
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

    if (uiState.showRatingDialogForUser != null) {
        RatingDialog(
            userNameToRate = uiState.otherUserDisplayName,
            onDismiss = { viewModel.onDismissRatingDialog() },
            onSubmitRating = { ratingValue -> viewModel.submitRating(ratingValue) },
            isSubmitting = uiState.ratingSubmissionInProgress
        )
    }
}

@Composable
fun RateUserBar(userNameToRate: String, onClickRate: () -> Unit) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Rate $userNameToRate for this transaction:", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClickRate, shape = MaterialTheme.shapes.small) {
                Icon(Icons.Filled.Stars, contentDescription = "Rate", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Rate")
            }
        }
    }
}

@Composable
fun RatingDialog(
    userNameToRate: String,
    onDismiss: () -> Unit,
    onSubmitRating: (Int) -> Unit,
    isSubmitting: Boolean
) {
    var selectedRating by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate $userNameToRate") },
        text = {
            Column {
                Text("Select a rating (1-5 stars):")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedRating = star }) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "Star $star",
                                tint = if (star <= selectedRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedRating > 0) onSubmitRating(selectedRating) },
                enabled = selectedRating > 0 && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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