package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.pharmaciesdegarde.ui.theme.*

@Composable
fun GestionServiceScreen(
    onBack: () -> Unit,
    onPharmaciesClick: () -> Unit,
    onGardesClick: () -> Unit,
    onAccueil: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = { AdminHeader(title = "Gestion du service", onBack = onBack) },
        bottomBar = { AdminBottomBar(AdminTab.ADMINISTRATION, onAccueil = onAccueil) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Bandeau d'information
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlueSoft, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Administration mobile", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Toutes les opérations sont optimisées pour smartphone",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceCard(
                    Icons.Default.LocalPharmacy, "Pharmacies",
                    "Ajouter, modifier ou supprimer", onPharmaciesClick
                )
                ServiceCard(
                    Icons.Default.CalendarToday, "Plannings de garde",
                    "Gérer les gardes par région", onGardesClick
                )
            }
        }
    }
}