@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.GardesViewModel

private val FICHE_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val FICHE_HOUR = DateTimeFormatter.ofPattern("HH:mm")

// « Aujourd'hui 19:00 », « Demain 08:00 », « Vendredi 14 février 20:00 »
private fun friendly(dt: LocalDateTime, today: LocalDate): String {
    val hour = dt.format(FICHE_HOUR)
    return when (dt.toLocalDate()) {
        today -> "Aujourd'hui $hour"
        today.plusDays(1) -> "Demain $hour"
        else -> "${dt.format(FICHE_DAY).replaceFirstChar { it.uppercase() }} $hour"
    }
}

private fun GardeDto.describe(today: LocalDate): String =
    "${friendly(debut(), today)} — ${friendly(fin(), today)} · ${TypeGarde.from(type).label}"

@Composable
fun PharmacieFicheScreen(
    pharmacieId: Long,
    onBack: () -> Unit,
    viewModel: GardesViewModel      // le même que celui de la liste : les gardes sont déjà chargées
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val now by produceState(LocalDateTime.now()) {
        while (true) { delay(60_000); value = LocalDateTime.now() }
    }

    // Toutes les gardes de cette pharmacie, de la plus proche à la plus lointaine
    val mine = remember(state.gardes, pharmacieId) {
        state.gardes.filter { it.pharmacie.id == pharmacieId }.sortedBy { it.debut() }
    }
    val pharmacie = mine.firstOrNull()?.pharmacie
    val current = mine.firstOrNull { it.isOngoing(now) }
    val upcoming = mine.firstOrNull { it.debut().isAfter(now) }
    val today = now.toLocalDate()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextInk)
                }
                Text("Fiche pharmacie", color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    ) { padding ->
        if (pharmacie == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
                else Text("Pharmacie introuvable", color = TextMuted, fontSize = 13.sp)
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Photo (ou icône si la pharmacie n'en a pas)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                if (pharmacie.photo.isNullOrBlank()) {
                    Icon(
                        Icons.Default.LocalPharmacy, contentDescription = null,
                        tint = PrimaryBlue, modifier = Modifier.size(56.dp)
                    )
                } else {
                    AsyncImage(
                        model = pharmacie.photo,
                        contentDescription = pharmacie.nom,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                // Badge d'état
                val open = current != null
                Text(
                    if (open) "OUVERTE MAINTENANT" else "PAS DE GARDE EN CE MOMENT",
                    color = if (open) GreenPharmacy else TextMuted,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (open) GreenSoft else Color(0xFFF1F5F9),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )

                Spacer(Modifier.height(10.dp))
                Text(pharmacie.nom, color = TextInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, contentDescription = null,
                        tint = TextMuted, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${pharmacie.adresse}, ${pharmacie.ville}",
                        color = TextMuted, fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Appeler", Icons.Default.Call, Modifier.weight(1f)) {
                        dial(context, pharmacie.telephone)
                    }
                    ActionButton("Itinéraire", Icons.Default.Directions, Modifier.weight(1f)) {
                        openInMaps(context, pharmacie)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Informations", color = TextInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))

                InfoRow(
                    Icons.Default.Schedule, "Horaires de garde",
                    current?.describe(today) ?: "Aucune garde en ce moment"
                )
                Spacer(Modifier.height(12.dp))
                InfoRow(Icons.Default.Phone, "Téléphone", pharmacie.telephone)
                Spacer(Modifier.height(12.dp))
                InfoRow(
                    Icons.Default.CalendarToday, "Prochaine garde",
                    upcoming?.describe(today) ?: "Aucune garde planifiée"
                )
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue)
        Spacer(Modifier.height(6.dp))
        Text(label, color = TextInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextMuted, fontSize = 12.sp)
        }
    }
}