@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.GardesViewModel
import androidx.compose.foundation.layout.height

private val HOME_HOUR = DateTimeFormatter.ofPattern("HH:mm")
private val HOME_DAY_HOUR = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private enum class GardeFilter { ONGOING, LATER, ALL }

@Composable
private fun StatusChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextInk,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

// « jusqu'à 08:00 », « jusqu'à demain 08:00 », « jusqu'au 25/11 08:00 »
private fun untilText(fin: LocalDateTime, today: LocalDate): String = when (fin.toLocalDate()) {
    today -> "jusqu'à ${fin.format(HOME_HOUR)}"
    today.plusDays(1) -> "jusqu'à demain ${fin.format(HOME_HOUR)}"
    else -> "jusqu'au ${fin.format(HOME_DAY_HOUR)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nom: String,
    onPharmacieClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onLogout: () -> Unit,
    viewModel: GardesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRegions by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(GardeFilter.ONGOING) }

    // L'heure est rafraîchie chaque minute : une garde terminée disparaît de la liste
    val now by produceState(LocalDateTime.now()) {
        while (true) { delay(60_000); value = LocalDateTime.now() }
    }

    // Pharmacies ouvertes maintenant = gardes en cours
    val open = remember(state.gardes, now) {
        state.gardes.filter { it.isOngoing(now) }.sortedBy { it.pharmacie.nom }
    }
// Gardes d'aujourd'hui pas encore terminées : en cours + celles qui commencent plus tard
    val todayGardes = remember(state.gardes, now) {
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay()
        state.gardes.filter { it.debut().isBefore(endOfDay) && it.fin().isAfter(now) }
    }
    val later = remember(todayGardes, now) {
        todayGardes.filter { !it.isOngoing(now) }.sortedBy { it.debut() }
    }
    val filtered = when (filter) {
        GardeFilter.ONGOING -> open
        GardeFilter.LATER -> later
        GardeFilter.ALL -> open + later
    }
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
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nom.isBlank()) "Bonsoir" else "Bonsoir, $nom",
                        color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trouvez une pharmacie",
                        color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Se déconnecter", tint = TextMuted
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Région (remplace la position écrite en dur)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showRegions = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryBlueSoft, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn, contentDescription = null,
                            tint = PrimaryBlue, modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Votre région", color = TextMuted, fontSize = 10.sp)
                        Text(
                            state.gouvernorat, color = TextInk,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Changer de région", tint = TextMuted
                    )
                }
            }

            // Barre de recherche
            item {
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
            }

            // Titre de section avec le vrai nombre de pharmacies ouvertes
            item {
                Column {
                    Text(
                        "Pharmacies de garde · aujourd'hui",
                        color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.loading) "Chargement…"
                        else "${open.size} ouverte(s) en ce moment · ${todayGardes.size - open.size} plus tard · ${state.gouvernorat}",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
            }

            // Carte des pharmacies de garde en ce moment
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("En cours (${open.size})", filter == GardeFilter.ONGOING, GreenPharmacy) {
                        filter = GardeFilter.ONGOING
                    }
                    StatusChip("Plus tard (${later.size})", filter == GardeFilter.LATER, Color(0xFFF59E0B)) {
                        filter = GardeFilter.LATER
                    }
                    StatusChip("Toutes", filter == GardeFilter.ALL, PrimaryBlue) {
                        filter = GardeFilter.ALL
                    }
                }
            }

            item {
                GardesMap(
                    gardes = filtered,
                    now = now,
                    onPharmacieClick = onPharmacieClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            // Liste des pharmacies ouvertes maintenant
            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = RedClosed, fontSize = 13.sp)
                        TextButton(onClick = viewModel::loadGardes) { Text("Réessayer") }
                    }
                }
                filtered.isEmpty() -> item {
                    Text(
                        when (filter) {
                            GardeFilter.ONGOING -> "Aucune pharmacie de garde en ce moment à ${state.gouvernorat}."
                            GardeFilter.LATER -> "Aucune autre garde prévue aujourd'hui à ${state.gouvernorat}."
                            GardeFilter.ALL -> "Aucune garde aujourd'hui à ${state.gouvernorat}."
                        },
                        color = TextMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> items(filtered, key = { it.id }) { g ->
                    OpenGardeCard(
                        g = g,
                        today = now.toLocalDate(),
                        ongoing = g.isOngoing(now),
                        onClick = { onPharmacieClick(g.pharmacie.id) },
                        onDirections = { openInMaps(context, g.pharmacie) }
                    )
                }
            }

            item {
                TextButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Voir toutes les gardes de la journée",
                        color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showRegions) {
        RegionSheet(
            selected = state.gouvernorat,
            onSelect = { viewModel.selectRegion(it); showRegions = false },
            onDismiss = { showRegions = false }
        )
    }
}

// Carte d'une pharmacie ouverte : un clic ouvre la fiche, « Itinéraire » ouvre la carte
@Composable
private fun OpenGardeCard(
    g: GardeDto,
    today: LocalDate,
    ongoing: Boolean,
    onClick: () -> Unit,
    onDirections: () -> Unit
) {
    val type = TypeGarde.from(g.type)
    val shape = RoundedCornerShape(18.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, Color(0xFFE5EAF2), shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(PrimaryBlueSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = PrimaryBlue) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (ongoing) "OUVERTE MAINTENANT" else "À PARTIR DE ${g.debut().format(HOME_HOUR)}",
                    color = if (ongoing) GreenPharmacy else Color(0xFFB7791F),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(if (ongoing) GreenSoft else Color(0xFFFFF4E5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Spacer(Modifier.height(4.dp))
                Text(g.pharmacie.nom, color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, contentDescription = null,
                        tint = TextMuted, modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${g.pharmacie.adresse} · ${g.pharmacie.ville}",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Voir la fiche", tint = TextMuted
            )
        }

        HorizontalDivider(color = Color(0xFFE5EAF2))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, contentDescription = null,
                    tint = TextMuted, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${type.label} · ${untilText(g.fin(), today)}",
                    color = TextMuted, fontSize = 11.sp
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDirections).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Itinéraire", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}