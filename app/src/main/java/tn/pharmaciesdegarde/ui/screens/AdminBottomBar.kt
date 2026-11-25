package tn.pharmaciesdegarde.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

enum class AdminTab(val label: String, val icon: ImageVector) {
    ACCUEIL("Accueil", Icons.Default.Home),
    MODERATION("Modération", Icons.Default.Warning),
    RAPPORTS("Rapports", Icons.Default.Description),

    ADMINISTRATION("Administration", Icons.Default.AdminPanelSettings),
}

@Composable
fun AdminBottomBar(
    selected: AdminTab,
    onAccueil: () -> Unit = {},
    onAdministration: () -> Unit = {}
) {
    NavigationBar(containerColor = Color.White) {
        AdminTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = {
                    if (tab != selected) when (tab) {
                        AdminTab.ACCUEIL -> onAccueil()
                        AdminTab.ADMINISTRATION -> onAdministration()
                        else -> {}   // Modération, Rapports, Profil : à brancher plus tard
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 10.sp) }
            )
        }
    }
}