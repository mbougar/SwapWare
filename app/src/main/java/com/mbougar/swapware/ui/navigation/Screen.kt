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
    object ChatDetail : Screen(
        route = "chat_detail/{conversationId}/{otherUserEmail}/{adTitle}",
        title = "Chat",
        icon = Icons.Default.Email //TODO cambiar a icono chat
    ) {
        fun createRoute(conversationId: String, otherUserEmail: String, adTitle: String): String {
            // Encode es necesario para poder afrontar caracteres especiales
            val encodedEmail = java.net.URLEncoder.encode(otherUserEmail, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(adTitle, "UTF-8")
            return "chat_detail/$conversationId/$encodedEmail/$encodedTitle"
        }
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Favorites,
    Screen.AddAd,
    Screen.Messages,
    Screen.Profile
)