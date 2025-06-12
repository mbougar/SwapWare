package com.mbougar.swapware.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mbougar.swapware.ui.screens.addad.AddAdScreen
import com.mbougar.swapware.ui.screens.auth.LoginScreen
import com.mbougar.swapware.ui.screens.auth.RegisterScreen
import com.mbougar.swapware.ui.screens.details.AdDetailScreen
import com.mbougar.swapware.ui.screens.favorites.FavoritesScreen
import com.mbougar.swapware.ui.screens.home.HomeScreen
import com.mbougar.swapware.ui.screens.messages.ChatDetailScreen
import com.mbougar.swapware.ui.screens.messages.MessagesScreen
import com.mbougar.swapware.ui.screens.myads.MyAdsScreen
import com.mbougar.swapware.ui.screens.profile.ProfileScreen
import com.mbougar.swapware.ui.screens.tos.TermsOfServiceScreen
import com.mbougar.swapware.ui.screens.userprofile.UserProfileScreen
import java.net.URLDecoder

/**
 * Composable que define el grafo de navegación de la aplicación.
 * Decide qué pantalla mostrar según la ruta actual.
 * @param navController El controlador para navegar entre pantallas.
 * @param startDestination La ruta de la pantalla inicial.
 * @param modifier Modificador de Compose.
 */
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
                        popUpTo(Screen.Register.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
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
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUserDisplayName") { type = NavType.StringType },
                navArgument("adTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            val encodedDisplayName = backStackEntry.arguments?.getString("otherUserDisplayName")
            val encodedTitle = backStackEntry.arguments?.getString("adTitle")

            requireNotNull(conversationId) { "Conversation ID is required" }
            requireNotNull(encodedDisplayName) { "Other user display name is required" }
            requireNotNull(encodedTitle) { "Ad title is required" }

            val otherUserDisplayName = remember(encodedDisplayName) { URLDecoder.decode(encodedDisplayName, "UTF-8") }
            val adTitle = remember(encodedTitle) { URLDecoder.decode(encodedTitle, "UTF-8") }

            ChatDetailScreen(
                conversationId = conversationId,
                otherUserDisplayName = otherUserDisplayName,
                adTitle = adTitle,
                navController = navController
            )
        }
        composable(Screen.MyAds.route) {
            MyAdsScreen(navController = navController)
        }
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(navController = navController)
        }
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            requireNotNull(userId) { "User ID is required for UserProfileScreen" }
            UserProfileScreen(navController = navController, userId = userId)
        }
    }
}