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
import tn.pharmaciesdegarde.ui.screens.GardesScreen
import tn.pharmaciesdegarde.ui.screens.GardesUserScreen
import tn.pharmaciesdegarde.ui.screens.GestionServiceScreen
import tn.pharmaciesdegarde.ui.screens.HomeScreen
import tn.pharmaciesdegarde.ui.screens.LoginScreen
import tn.pharmaciesdegarde.ui.screens.PharmaciesScreen
import tn.pharmaciesdegarde.ui.screens.RegisterScreen
import tn.pharmaciesdegarde.ui.screens.WelcomeScreen
import tn.pharmaciesdegarde.viewmodel.SessionViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import tn.pharmaciesdegarde.ui.screens.PharmacieFicheScreen
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
                onGoRegister = {
                    navController.navigate("register") { popUpTo("login") { inclusive = true } }
                }            )
        }
        composable("register") {
            RegisterScreen(
                onSuccess = { role ->
                    val target = if (role.equals("ADMIN", ignoreCase = true)) "admin" else "home"
                    navController.navigate(target) { popUpTo("welcome") { inclusive = true } }
                },
                onGoLogin = {
                    navController.navigate("login") { popUpTo("register") { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(
                nom = nom,
                onPharmacieClick = { id -> navController.navigate("pharmacie/$id") },
                onSeeAll = { navController.navigate("gardes-user") },
                onLogout = {
                    session.logout()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("gardes-user") { entry ->
            // Même ViewModel que l'accueil : la région choisie est conservée
            val homeEntry = remember(entry) { navController.getBackStackEntry("home") }
            GardesUserScreen(
                onBack = { navController.popBackStack() },
                onPharmacieClick = { id -> navController.navigate("pharmacie/$id") },
                viewModel = hiltViewModel(homeEntry)
            )
        }
        composable(
            route = "pharmacie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            // Les gardes sont déjà en mémoire dans le ViewModel de l'accueil
            val homeEntry = remember(entry) { navController.getBackStackEntry("home") }
            PharmacieFicheScreen(
                pharmacieId = id,
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel(homeEntry)
            )
        }
        composable("admin") {
            CentreControleScreen(
                onPharmaciesClick = { navController.navigate("pharmacies") },
                onAdministration = { navController.navigate("gestion") { launchSingleTop = true } },
                onLogout = {
                    session.logout()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable("gestion") {
            GestionServiceScreen(
                onBack = { navController.popBackStack() },
                onPharmaciesClick = { navController.navigate("pharmacies") },
                onGardesClick = { navController.navigate("gardes") },
                onAccueil = { navController.popBackStack("admin", inclusive = false) }
            )
        }

        composable("pharmacies") {
            PharmaciesScreen(onBack = { navController.popBackStack() })
        }
        composable("gardes") {
            GardesScreen(
                onBack = { navController.popBackStack() },
                onAccueil = { navController.popBackStack("admin", inclusive = false) },
                onAdministration = { navController.popBackStack("gestion", inclusive = false) }
            )

        }

    }
}