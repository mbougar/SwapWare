package com.mbougar.swapware.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Add Modifier import
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mbougar.swapware.ui.screens.addad.AddAdScreen
import com.mbougar.swapware.ui.screens.auth.LoginScreen
import com.mbougar.swapware.ui.screens.details.AdDetailScreen
import com.mbougar.swapware.ui.screens.favorites.FavoritesScreen
import com.mbougar.swapware.ui.screens.home.HomeScreen
import com.mbougar.swapware.ui.screens.messages.MessagesScreen
import com.mbougar.swapware.ui.screens.profile.ProfileScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(navController = navController)
        }
        composable(Screen.AddAd.route) {
            AddAdScreen(navController = navController)
        }
        composable(Screen.Messages.route) {
            MessagesScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Screen.AdDetail.route,
            arguments = listOf(navArgument("adId") { type = NavType.StringType })
        ) { backStackEntry ->
            val adId = backStackEntry.arguments?.getString("adId")
            requireNotNull(adId) { "Ad ID is required" }
            AdDetailScreen(adId = adId, navController = navController)
        }
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            requireNotNull(conversationId) { "Conversation ID is required for Chat Detail" }

            // --- Placeholder: Implement ChatDetailScreen later ---
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chat Detail Screen for ID: $conversationId (Not Implemented)")
            }
            // TODO cambiar por ChatDetail
        }
        // TODO añadir settings?
    }
}