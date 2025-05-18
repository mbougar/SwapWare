package com.mbougar.swapware.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object AddAd : Screen("add_ad", "Add", Icons.Default.AddCircle)
    object Messages : Screen("messages", "Messages", Icons.AutoMirrored.Filled.Chat)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Register", Icons.Default.PersonAdd)
    object AdDetail : Screen("ad_detail/{adId}", "Details", Icons.AutoMirrored.Filled.List) {
        fun createRoute(adId: String) = "ad_detail/$adId"
    }
    object ChatDetail : Screen(
        route = "chat_detail/{conversationId}/{otherUserEmail}/{adTitle}",
        title = "Chat",
        icon =  Icons.AutoMirrored.Filled.Chat
    ) {
        fun createRoute(conversationId: String, otherUserEmail: String, adTitle: String): String {
            val encodedEmail = URLEncoder.encode(otherUserEmail, "UTF-8")
            val encodedTitle = URLEncoder.encode(adTitle, "UTF-8")
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
