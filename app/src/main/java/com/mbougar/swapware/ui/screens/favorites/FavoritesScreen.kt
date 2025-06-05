package com.mbougar.swapware.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.ui.screens.home.AdItem
import com.mbougar.swapware.viewmodel.FavoritesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val currentUserIdForFavorites = favoritesViewModel.uiState.collectAsState().value.currentUserId
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Favorite Ads") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                uiState.favoriteAds.isEmpty() -> {
                    Text("You have no favorite ads yet.", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.favoriteAds, key = { it.id }) { ad ->
                            AdItem(
                                ad = ad,
                                onAdClick = {
                                    navController.navigate(Screen.AdDetail.createRoute(ad.id))
                                },
                                onFavoriteClick = {
                                    viewModel.removeFromFavorites(ad.id)
                                },
                                currentUserId = currentUserIdForFavorites
                            )
                        }
                    }
                }
            }
        }
    }
}