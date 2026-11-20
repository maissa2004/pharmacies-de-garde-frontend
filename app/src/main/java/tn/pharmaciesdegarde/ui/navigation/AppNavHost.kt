package tn.pharmaciesdegarde.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tn.pharmaciesdegarde.ui.screens.HomeScreen
import tn.pharmaciesdegarde.ui.screens.LoginScreen
import tn.pharmaciesdegarde.ui.screens.WelcomeScreen
import tn.pharmaciesdegarde.viewmodel.SessionViewModel

@Composable
fun AppNavHost(session: SessionViewModel = hiltViewModel()) {
    val loggedIn by session.loggedIn.collectAsStateWithLifecycle()
    val nom by session.nom.collectAsStateWithLifecycle()
    if (loggedIn == null) return // attend la lecture du DataStore

    val navController = rememberNavController()
    NavHost(navController, startDestination = if (loggedIn == true) "home" else "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onStart = { navController.navigate("register") },
                onHaveAccount = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(
                onSuccess = { navController.navigate("home") { popUpTo("welcome") { inclusive = true } } },
                onGoRegister = { navController.navigate("register") }
            )
        }
        composable("register") { /* RegisterScreen, même onSuccess */ }
        composable("home") {
            HomeScreen(
                nom = nom,
                onLogout = {
                    session.logout()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}