package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.pharmaciesdegarde.ui.theme.*

enum class AuthTab { LOGIN, REGISTER }
enum class AuthRole { UTILISATEUR, PHARMACIEN }

// Bande « Connexion | Inscription »
@Composable
fun AuthTabs(selected: AuthTab, onLogin: () -> Unit, onRegister: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFFE6EDF3), RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        TabItem("Connexion", selected == AuthTab.LOGIN, Modifier.weight(1f), onLogin)
        TabItem("Inscription", selected == AuthTab.REGISTER, Modifier.weight(1f), onRegister)
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(enabled = !selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) PrimaryBlue else TextMuted
        )
    }
}

// Choix « Utilisateur | Pharmacien »
@Composable
fun RoleSelector(selected: AuthRole, onSelect: (AuthRole) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RoleChip(
            "Utilisateur", Icons.Default.Person,
            selected == AuthRole.UTILISATEUR, Modifier.weight(1f)
        ) { onSelect(AuthRole.UTILISATEUR) }
        RoleChip(
            "Pharmacien", Icons.Default.LocalPharmacy,
            selected == AuthRole.PHARMACIEN, Modifier.weight(1f)
        ) { onSelect(AuthRole.PHARMACIEN) }
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) GreenSoft else Color.White)
            .border(1.dp, if (selected) GreenPharmacy else BorderLine, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = if (selected) GreenPharmacy else TextMuted
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) GreenPharmacy else TextInk
        )
    }
}