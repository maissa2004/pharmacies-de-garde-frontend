package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

@Composable
fun AdminHeader(
    title: String,
    subtitle: String = "Espace administrateur",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)      // fond blanc, y compris derrière la barre d'état
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextInk)
                }
                Spacer(Modifier.width(8.dp))
            }
            Image(
                painter = painterResource(id = R.drawable.logo_pharmacie),
                contentDescription = "Logo",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subtitle, color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(title, color = TextInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            actions()
        }
        HorizontalDivider(color = Color(0xFFE5EAF2))   // trait de séparation avec le contenu
    }
}