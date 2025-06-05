package com.mbougar.swapware.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    data object AddAd : Screen("add_ad", "Add", Icons.Default.AddCircle)
    data object Messages : Screen("messages", "Messages", Icons.AutoMirrored.Filled.Chat)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)

    data object Login : Screen("login", "Login", Icons.Default.Lock)
    data object Register : Screen("register", "Register", Icons.Default.PersonAdd)
    data object AdDetail : Screen("ad_detail/{adId}", "Details", Icons.AutoMirrored.Filled.List) {
        fun createRoute(adId: String) = "ad_detail/$adId"
    }
    data object ChatDetail : Screen(
        route = "chat_detail/{conversationId}/{otherUserDisplayName}/{adTitle}",
        title = "Chat",
        icon =  Icons.AutoMirrored.Filled.Chat
    ) {
        fun createRoute(conversationId: String, otherUserDisplayName: String, adTitle: String): String {
            val encodedDisplayName = URLEncoder.encode(otherUserDisplayName, "UTF-8")
            val encodedTitle = URLEncoder.encode(adTitle, "UTF-8")
            return "chat_detail/$conversationId/$encodedDisplayName/$encodedTitle"
        }
    }

    data object MyAds : Screen("my_ads", "My Ads", Icons.Filled.Storefront)
    data object TermsOfService : Screen("terms_of_service", "Terms of Service", Icons.Filled.Article)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Favorites,
    Screen.AddAd,
    Screen.Messages,
    Screen.Profile
)
