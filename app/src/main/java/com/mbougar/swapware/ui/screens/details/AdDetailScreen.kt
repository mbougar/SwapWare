package com.mbougar.swapware.ui.screens.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.viewmodel.AdDetailUiState
import com.mbougar.swapware.viewmodel.AdDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdDetailScreen(
    adId: String,
    navController: NavController,
    viewModel: AdDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.navigateToConversationId) {
        uiState.navigateToConversationId?.let { conversationId ->
            val ad = uiState.ad
            if (ad != null) {
                val otherUserDisplayName = ad.sellerDisplayName.ifEmpty {
                    ad.sellerEmail.takeIf { it.isNotEmpty() } ?: "Unknown Seller"
                }

                navController.navigate(
                    Screen.ChatDetail.createRoute(
                        conversationId = conversationId,
                        otherUserDisplayName = otherUserDisplayName,
                        adTitle = ad.title
                    )
                )
                viewModel.navigationOrErrorHandled()
            }
        }
    }

    LaunchedEffect(uiState.conversationError) {
        uiState.conversationError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar("Error: $error")
            }
            viewModel.navigationOrErrorHandled()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.ad?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.ad?.let { ad ->
                        if (!uiState.isOwnAd) {
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (ad.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (ad.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AdDetailContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onContactSeller = {
                viewModel.initiateConversation()
            },
            isContactingSeller = uiState.isInitiatingConversation
        )
    }
}

@Composable
fun AdDetailContent(
    modifier: Modifier = Modifier,
    uiState: AdDetailUiState,
    onContactSeller: () -> Unit,
    isContactingSeller: Boolean
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            uiState.ad == null -> {
                Text(
                    "Ad not found.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                val ad = uiState.ad
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (ad.imageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(ad.imageUrl)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = ad.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp), // TODO decidir tamaño
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(ad.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("€${ad.price}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Category: ${ad.category}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Description", style = MaterialTheme.typography.titleMedium)
                    Text(ad.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Seller: ${ad.sellerDisplayName.ifEmpty { ad.sellerEmail }}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    ad.sellerLocation?.let { location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "Seller Location",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(location, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onContactSeller,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isContactingSeller && ad.sellerId != com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ) {
                        if (isContactingSeller) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        }
                        Text("Contact Seller")
                    }

                    Text(text = "${ad.isSold}")
                }
            }
        }
    }
}