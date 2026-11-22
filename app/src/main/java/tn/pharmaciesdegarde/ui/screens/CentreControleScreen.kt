package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.SessionViewModel

@Composable
fun CentreControleScreen(
    onPharmaciesClick: () -> Unit = {},
    onAdministration: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            AdminHeader(title = "Centre de contrôle") {
                // Avatar « AD » : pour l'instant un clic déconnecte
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF7E57C2))
                        .clickable { viewModel.logout(); onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("AD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(9.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                }
            }
        },
        bottomBar = { AdminBottomBar(AdminTab.ACCUEIL, onAdministration = onAdministration) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ServiceCard(
                Icons.Default.LocalPharmacy,
                "Pharmacies",
                "Gérer les établissements",
                onPharmaciesClick
            )
        }
    }
}

@Composable
internal fun ServiceCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(PrimaryBlueSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = PrimaryBlue) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}