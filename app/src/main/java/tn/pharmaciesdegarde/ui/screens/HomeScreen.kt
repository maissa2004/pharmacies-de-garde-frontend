package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            // En-tête (Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_pharmacie),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bonsoir, Mariem",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trouvez une pharmacie",
                        color = TextInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Position
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimaryBlueSoft, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Votre position", color = TextMuted, fontSize = 10.sp)
                    Text("La Marsa, Tunis", color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barre de recherche
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.White, RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Rechercher un médicament", color = TextMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Titre de section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Pharmacies de garde · aujourd'hui", color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("12 pharmacies autour de vous", color = TextMuted, fontSize = 10.sp)
                }
                Text("Filtres", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Carte (Simulation visuelle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(GreenSoft, RoundedCornerShape(18.dp)) // Fond de la carte
            ) {
                // Ici vous mettrez plus tard Google Maps Compose
                Text(
                    text = "Simulation Carte (Google Maps)",
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenPharmacy
                )
            }
        }
    }
}