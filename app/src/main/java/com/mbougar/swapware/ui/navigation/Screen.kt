package com.mbougar.swapware.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object AddAd : Screen("add_ad", "Add", Icons.Default.AddCircle)
    object Messages : Screen("messages", "Messages", Icons.Default.Email)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    object Login : Screen("login", "Login", Icons.Default.Lock)
    object AdDetail : Screen("ad_detail/{adId}", "Details", Icons.Default.List) {
        fun createRoute(adId: String) = "ad_detail/$adId"
    }
    object ChatDetail : Screen("chat_detail/{conversationId}", "Chat", Icons.Default.Email) { // TODO() Add chat icon
        fun createRoute(conversationId: String) = "chat_detail/$conversationId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Favorites,
    Screen.AddAd,
    Screen.Messages,
    Screen.Profile
)