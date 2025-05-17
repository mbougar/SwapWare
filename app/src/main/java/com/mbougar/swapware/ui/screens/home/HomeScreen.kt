package com.mbougar.swapware.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mbougar.swapware.R
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.HardwareCategory
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
                title = { Text("SwapWare") },
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

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdItem(
    ad: Ad,
    onAdClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onAdClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            if (ad.imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(ad.imageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_placeholder_image)
                            .error(R.drawable.ic_error_image)
                            .build()
                    ),
                    contentDescription = ad.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ImageNotSupported,
                        contentDescription = "No image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ad.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "€${String.format("%.2f", ad.price)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        ad.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (ad.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (ad.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
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

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
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
            HardwareCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category.displayName)
                        expanded = false
                    }
                )
            }
        }
    }
}