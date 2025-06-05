package com.mbougar.swapware.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mbougar.swapware.R
import com.mbougar.swapware.data.local.PoblacionLocation
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.HardwareCategory
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.viewmodel.HomeUiState
import com.mbougar.swapware.viewmodel.HomeViewModel
import kotlin.math.roundToInt

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
                        currentUiState = uiState,
                        onCategorySelected = { viewModel.filterByCategory(it) },
                        onLocationQueryChanged = { viewModel.onFilterLocationSearchQueryChanged(it) },
                        onLocationSelected = { viewModel.setDistanceFilterLocation(it) },
                        onDistanceSelected = { viewModel.setFilterDistanceKm(it) },
                        onClearDistanceFilter = {
                            viewModel.setDistanceFilterLocation(null)
                            viewModel.setFilterDistanceKm(null)
                            viewModel.onFilterLocationSearchQueryChanged("")
                        }
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
            onRefresh = { viewModel.refresh() }
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
    val homeViewModel: HomeViewModel = hiltViewModel()
    val currentUserIdForHome = homeViewModel.uiState.collectAsState().value.currentUserId

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
            }
            uiState.ads.isEmpty() -> {
                Text("No ads found.", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.ads, key = { it.id }) { ad ->
                        AdItem(
                            ad = ad,
                            onAdClick = { onAdClick(ad.id) },
                            onFavoriteClick = { onFavoriteClick(ad.id) },
                            currentUserId = currentUserIdForHome
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
@Composable
fun AdItem(
    ad: Ad,
    onAdClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    currentUserId: String? = null
) {
    val isOwnAd = ad.sellerId == currentUserId && currentUserId != null

    Card(
        onClick = onAdClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
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
                        ad.sellerLocation?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!isOwnAd) {
                        IconButton(
                            onClick = onFavoriteClick,
                        ) {
                            Icon(
                                imageVector = if (ad.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (ad.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your Ad",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (ad.isSold) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (ad.imageUrl != null) 8.dp else 0.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = if (ad.imageUrl != null) 0.dp else 8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "SOLD",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMenu(
    currentUiState: HomeUiState,
    onCategorySelected: (String?) -> Unit,
    onLocationQueryChanged: (String) -> Unit,
    onLocationSelected: (PoblacionLocation) -> Unit,
    onDistanceSelected: (Float?) -> Unit,
    onClearDistanceFilter: () -> Unit
) {
    var mainFilterExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var locationSearchDropdownVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val distanceOptions = listOf(5f, 10f, 25f, 50f, 100f)

    Box {
        IconButton(onClick = { mainFilterExpanded = true }) {
            Icon(Icons.Default.FilterAlt, contentDescription = "Filter products")
        }
        DropdownMenu(
            expanded = mainFilterExpanded,
            onDismissRequest = { mainFilterExpanded = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).fillMaxHeight()
        ) {
            Text(
                "Filter Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            )

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentUiState.selectedCategory ?: "All Categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("All Categories") }, onClick = { onCategorySelected(null); categoryDropdownExpanded = false })
                        HardwareCategory.entries.forEach { category ->
                            DropdownMenuItem(text = { Text(category.displayName) }, onClick = { onCategorySelected(category.displayName); categoryDropdownExpanded = false })
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))

            Text("Distance from", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = locationSearchDropdownVisible && currentUiState.locationSuggestions.isNotEmpty() && currentUiState.locationSearchQuery.isNotBlank(),
                    onExpandedChange = {
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentUiState.locationSearchQuery,
                        onValueChange = { query ->
                            onLocationQueryChanged(query)
                            locationSearchDropdownVisible = query.isNotBlank()
                        },
                        label = { Text("Your Location (Población)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    locationSearchDropdownVisible = currentUiState.locationSearchQuery.isNotBlank()
                                } else {
                                    if (!focusState.isFocused) {
                                        locationSearchDropdownVisible = false
                                    }
                                }
                            },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    if (currentUiState.locationSuggestions.isNotEmpty() && currentUiState.locationSearchQuery.isNotBlank()) {
                        ExposedDropdownMenu(
                            expanded = locationSearchDropdownVisible,
                            onDismissRequest = {
                                locationSearchDropdownVisible = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentUiState.locationSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion.getDisplayName()) },
                                    onClick = {
                                        onLocationSelected(suggestion)
                                        locationSearchDropdownVisible = false
                                        focusManager.clearFocus()
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
            }

            if (currentUiState.userPoblacionForFilter != null) {
                Text(
                    "Max Distance: ${currentUiState.filterDistanceKm?.roundToInt()?.toString() ?: "Any"} km",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentUiState.filterDistanceKm ?: 0f,
                    onValueChange = { newValue -> onDistanceSelected(if (newValue == 0f) null else newValue) },
                    valueRange = 0f..100f,
                    steps = ((100f - 5f) / 5f).toInt() - 1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    distanceOptions.forEach { dist -> TextButton(onClick = { onDistanceSelected(dist) }) { Text("${dist.toInt()}km") } }
                    TextButton(onClick = { onDistanceSelected(null) }) { Text("Any") }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))

            if (currentUiState.userPoblacionForFilter != null || currentUiState.filterDistanceKm != null || currentUiState.selectedCategory != null) {
                DropdownMenuItem(text = { Text("Clear All Filters") }, onClick = { onCategorySelected(null); onClearDistanceFilter() })
            }
            DropdownMenuItem(text = { Text("Done", fontWeight = FontWeight.Bold) }, onClick = { mainFilterExpanded = false })
        }
    }
}