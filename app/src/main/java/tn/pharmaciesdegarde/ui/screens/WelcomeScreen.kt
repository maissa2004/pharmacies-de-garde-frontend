package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.ui.theme.*

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onHaveAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE3F1F8), Color.White, GreenSoft)
                )
            )
    ) {
        // Cercles décoratifs en arrière-plan
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-40).dp)
                .border(1.dp, BorderLine, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-110).dp, y = 120.dp)
                .border(1.dp, BorderLine, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Logo dans un cercle blanc
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .shadow(16.dp, CircleShape, ambientColor = PrimaryBlue, spotColor = PrimaryBlue)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_pharmacie),
                    contentDescription = "Logo Pharmacies de garde",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.height(36.dp))

            Text(
                text = "VOTRE SANTÉ, À PORTÉE DE MAIN",
                color = GreenPharmacy,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Pharmacies de garde",
                color = TextInk,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Trouvez rapidement une pharmacie ouverte et vérifiez la disponibilité de vos médicaments partout en Tunisie.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureChip(Icons.Outlined.NearMe, "Proche")
                FeatureChip(Icons.Outlined.VerifiedUser, "Fiable")
                FeatureChip(Icons.Outlined.Schedule, "24h/24")
            }

            Spacer(Modifier.weight(1f))

            // Bouton dégradé vert -> bleu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = GreenPharmacy, spotColor = PrimaryBlue)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(GreenPharmacy, PrimaryBlue)))
                    .clickable(onClick = onStart)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Commencer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "J'ai déjà un compte",
                color = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHaveAccount)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, BorderLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextInk, modifier = Modifier.size(14.dp))
        Text(label, color = TextInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(onStart = {}, onHaveAccount = {})
}