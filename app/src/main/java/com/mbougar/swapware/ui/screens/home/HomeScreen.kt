package com.mbougar.swapware.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.viewmodel.HomeUiState
import com.mbougar.swapware.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketplace Home") },
                actions = {
                    FilterMenu(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.filterByCategory(it) }
                    )
                }
            )
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onAdClick = { adId ->
                navController.navigate(Screen.AdDetail.createRoute(adId))
            },
            onFavoriteClick = { adId ->
                viewModel.toggleFavorite(adId)
            },
            onRefresh = { viewModel.refresh() } // TODO hacer que se actualiza al arrastrar la pantalla desde la parte superior?
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onAdClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.ads.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                // TODO Quiza un  Button("Retry") { onRefresh() } ??
            }
            uiState.ads.isEmpty() -> {
                Text("No ads found.", modifier = Modifier.align(Alignment.Center))
                // TODO Quiza un  Button("Retry") { onRefresh() } ??
            }
            else -> {
                // Si decido usar gesto de refresh seria aqui tambien
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.ads, key = { it.id }) { ad ->
                        AdItem(
                            ad = ad,
                            onAdClick = { onAdClick(ad.id) },
                            onFavoriteClick = { onFavoriteClick(ad.id) }
                        )
                    }
                }
            }
        }
        if (uiState.isLoading && uiState.ads.isNotEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        }
    }
}

@Composable
fun AdItem(
    ad: Ad,
    onAdClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: añadir imagen

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(ad.title, style = MaterialTheme.typography.titleMedium)
                Text(ad.category, style = MaterialTheme.typography.bodySmall)
                Text("€${ad.price}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text("Seller: ${ad.sellerEmail}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (ad.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (ad.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    }
}

@Composable
fun FilterMenu(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Electronics", "Furniture", "Vehicles", "Clothing", "Other")

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = "Filter") // Todo() change icon to filter icon
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Categories") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}