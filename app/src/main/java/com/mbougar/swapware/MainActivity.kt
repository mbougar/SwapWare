package com.mbougar.swapware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mbougar.swapware.ui.navigation.AppNavHost
import com.mbougar.swapware.ui.navigation.Screen
import com.mbougar.swapware.ui.navigation.bottomNavItems
import com.mbougar.swapware.ui.theme.SwapWareTheme
import com.mbougar.swapware.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Esta es la actividad principal, el punto de entrada de la aplicación.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Este método se llama cuando se crea la actividad.
     * Aquí es donde configuramos la vista principal de la app.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwapWareTheme {
                Content()
            }
        }
    }
}

/**
 * Este es el composable principal que organiza la navegación
 * y la barra de navegación inferior.
 */
@Composable
fun Content() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = if (authViewModel.isUserLoggedIn()) Screen.Home.route else Screen.Login.route

    val shouldShowBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    // Scaffold es como un esqueleto para la pantalla, nos da lugares para poner cosas
    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            // Marcamos como seleccionado el ítem de la pantalla actual.
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Con esta llamada evitamos apilar pantallas infinitamente.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Evita lanzar la misma pantalla si ya estamos en ella.
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
            )
        }
    }
}