package tn.pharmaciesdegarde.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tn.pharmaciesdegarde.ui.screens.CentreControleScreen
import tn.pharmaciesdegarde.ui.screens.HomeScreen
import tn.pharmaciesdegarde.ui.screens.LoginScreen
import tn.pharmaciesdegarde.ui.screens.PharmaciesScreen
import tn.pharmaciesdegarde.ui.screens.WelcomeScreen
import tn.pharmaciesdegarde.viewmodel.SessionViewModel

@Composable
fun AppNavHost(session: SessionViewModel = hiltViewModel()) {
    val destination by session.startDestination.collectAsStateWithLifecycle()
    val nom by session.nom.collectAsStateWithLifecycle()

    var start by remember { mutableStateOf<String?>(null) }
    if (start  == null) start = destination // attend la lecture du DataStore
    val startDestination = start ?: return

    val navController = rememberNavController()
    NavHost(navController, startDestination = startDestination) {
        composable("welcome") {
            WelcomeScreen(
                onStart = { navController.navigate("register") },
                onHaveAccount = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(
                onSuccess = { role ->
                    val target = if (role.equals("ADMIN", ignoreCase = true)) "admin" else "home"
                    navController.navigate(target) { popUpTo("welcome") { inclusive = true } }
                },
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

        composable("admin") {
            CentreControleScreen(
                onPharmaciesClick = { navController.navigate("pharmacies") },
                onLogout = {
                    session.logout()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("pharmacies") {
            PharmaciesScreen(onBack = { navController.popBackStack() })
        }
    }
}