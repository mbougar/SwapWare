package com.mbougar.swapware.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mbougar.swapware.R
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.viewmodel.ProfileUiState
import com.mbougar.swapware.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onProfilePictureSelected(it) }
    }

    LaunchedEffect(uiState.pictureUploadError) {
        uiState.pictureUploadError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar("Error: $error")
            }
            viewModel.clearPictureUploadError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Profile") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 16.dp)
        ) {
            ProfileHeader(
                uiState = uiState,
                onImageClick = { imagePickerLauncher.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSection(
                viewModel = viewModel,
                navController = navController,
                onLogoutConfirmed = onLogout
            )
        }

        if (uiState.isUploadingPicture) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (uiState.showDeleteAccountDialog) {
            DeleteAccountConfirmationDialog(
                onConfirm = {
                    viewModel.deleteAccount()
                },
                onDismiss = { viewModel.onShowDeleteAccountDialog(false) }
            )
        }
    }
}

@Composable
fun ProfileHeader(uiState: ProfileUiState, onImageClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(uiState.profilePictureUrl ?: R.drawable.ic_placeholder_profile)
                        .error(R.drawable.ic_placeholder_profile)
                        .placeholder(R.drawable.ic_placeholder_profile)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit Profile Picture",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(4.dp)
                    .clickable(onClick = onImageClick),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = uiState.userDisplayName ?: "User Name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (uiState.userRating != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", uiState.userRating),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No rating yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    viewModel: ProfileViewModel,
    navController: NavController,
    onLogoutConfirmed: () -> Unit
) {
    Column {
        SettingsItem(
            icon = Icons.Filled.Storefront,
            title = "My Ads",
            onClick = { navController.navigate(Screen.MyAds.route) }
        )
        SettingsItem(
            icon = Icons.Filled.Article,
            title = "Terms of Service",
            onClick = { navController.navigate(Screen.TermsOfService.route) }
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Logout",
            onClick = {
                viewModel.logout()
                onLogoutConfirmed()
            }
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsItem(
            icon = Icons.Filled.DeleteForever,
            title = "Delete Account",
            contentColor = MaterialTheme.colorScheme.error,
            onClick = { viewModel.onShowDeleteAccountDialog(true) }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    contentColor: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Go to $title",
            tint = contentColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = { Text("Are you sure you want to delete your account? This action is irreversible and all your data will be lost.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}