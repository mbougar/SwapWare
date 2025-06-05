package com.mbougar.swapware.ui.screens.myads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.ui.screens.home.AdItem
import com.mbougar.swapware.viewmodel.MyAdsUiState
import com.mbougar.swapware.viewmodel.MyAdsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAdsScreen(
    navController: NavController,
    viewModel: MyAdsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Posted Ads") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        MyAdsContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            navController = navController,
            onFavoriteClick = { adId -> viewModel.toggleFavorite(adId, uiState.currentUserId) }
        )
    }
}

@Composable
fun MyAdsContent(
    modifier: Modifier = Modifier,
    uiState: MyAdsUiState,
    navController: NavController,
    onFavoriteClick: (String) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            uiState.ads.isEmpty() -> {
                Text("You haven't posted any ads yet.", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.ads, key = { it.id }) { ad ->
                        AdItem(
                            ad = ad,
                            onAdClick = {
                                navController.navigate(Screen.AdDetail.createRoute(ad.id))
                            },
                            onFavoriteClick = { onFavoriteClick(ad.id) },
                            currentUserId = uiState.currentUserId
                        )
                    }
                }
            }
        }
    }
}