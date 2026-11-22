@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MyLocation
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
import kotlinx.coroutines.launch
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.ALL_REGIONS
import tn.pharmaciesdegarde.viewmodel.DEFAULT_REGION
import tn.pharmaciesdegarde.viewmodel.GardesViewModel

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
    val scope = rememberCoroutineScope()
    var showRegions by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(GardeFilter.ONGOING) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }

    // Position de l'utilisateur (null tant qu'elle n'est pas connue)
    val loc = state.userLocation
    val nearMe = loc != null && state.gouvernorat == ALL_REGIONS
    val regionLabel = if (state.gouvernorat == ALL_REGIONS) "Près de moi" else state.gouvernorat
    val where = if (state.gouvernorat == ALL_REGIONS) "près de vous" else "à ${state.gouvernorat}"

    // 1) récupère la position, 2) charge les gardes de toutes les régions
    fun locate() {
        scope.launch {
            locating = true
            locationMessage = null
            val found = fetchLocation(context)
            locating = false
            if (found == null) {
                locationMessage = "Position introuvable. Activez la localisation du téléphone puis réessayez."
            } else {
                viewModel.setUserLocation(found.latitude, found.longitude)
                viewModel.selectRegion(ALL_REGIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) locate()
        else locationMessage = "Autorisez l'accès à la position pour voir les pharmacies les plus proches."
    }

    fun onNearMeClick() {
        when {
            nearMe -> {   // second appui : on désactive
                viewModel.clearUserLocation()
                viewModel.selectRegion(DEFAULT_REGION)
            }
            hasLocationPermission(context) -> locate()
            else -> permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // L'heure est rafraîchie chaque minute : une garde terminée disparaît de la liste
    val now by produceState(LocalDateTime.now()) {
        while (true) { delay(60_000); value = LocalDateTime.now() }
    }

    // Pharmacies ouvertes maintenant : de la plus proche à la plus éloignée si la position est connue
    val open = remember(state.gardes, now, loc) {
        val list = state.gardes.filter { it.isOngoing(now) }
        if (loc != null) list.sortedBy { it.distanceKm(loc) } else list.sortedBy { it.pharmacie.nom }
    }
    // Gardes d'aujourd'hui pas encore terminées : en cours + celles qui commencent plus tard
    val todayGardes = remember(state.gardes, now) {
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay()
        state.gardes.filter { it.debut().isBefore(endOfDay) && it.fin().isAfter(now) }
    }
    val later = remember(todayGardes, now, loc) {
        val list = todayGardes.filter { !it.isOngoing(now) }
        if (loc != null) list.sortedBy { it.distanceKm(loc) } else list.sortedBy { it.debut() }
    }
    val filtered = when (filter) {
        GardeFilter.ONGOING -> open
        GardeFilter.LATER -> later
        GardeFilter.ALL -> if (loc != null) (open + later).sortedBy { it.distanceKm(loc) } else open + later
    }
    val mapGardes = remember(open, later) { open + later }
    val focusIds = remember(filtered) { filtered.map { it.pharmacie.id }.toSet() }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
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
            // Contrôles : région + près de moi sur une seule ligne
            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Région
                        Row(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(PrimaryBlueSoft)
                                .clickable { showRegions = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null,
                                tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                regionLabel, color = PrimaryBlue, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Changer de région",
                                tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }

                        // Près de moi
                        Row(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (nearMe) GreenPharmacy else Color.White)
                                .border(1.dp, if (nearMe) GreenPharmacy else Color(0xFFE5EAF2), RoundedCornerShape(20.dp))
                                .clickable(enabled = !locating) { onNearMeClick() }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (locating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null,
                                    tint = if (nearMe) Color.White else PrimaryBlue,
                                    modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (nearMe) "Actif" else "Près de moi",
                                color = if (nearMe) Color.White else PrimaryBlue,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    locationMessage?.let {
                        Text(it, color = RedClosed, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

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

            // Carte des pharmacies de garde
            item {
                GardesMap(
                    gardes = mapGardes,
                    focusIds = focusIds,                    now = now,
                    userLocation = loc,
                    onPharmacieClick = onPharmacieClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                )
            }

            // Liste
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
                            GardeFilter.ONGOING -> "Aucune pharmacie de garde en ce moment $where."
                            GardeFilter.LATER -> "Aucune autre garde prévue aujourd'hui $where."
                            GardeFilter.ALL -> "Aucune garde aujourd'hui $where."
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
                        distance = loc?.let { formatDistance(g.distanceKm(it)) },
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

// Carte d'une pharmacie : un clic ouvre la fiche, « Itinéraire » ouvre la carte
@Composable
private fun OpenGardeCard(
    g: GardeDto,
    today: LocalDate,
    ongoing: Boolean,
    distance: String?,
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
                        tint = if (distance != null) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        buildString {
                            if (distance != null) append("$distance · ")
                            append("${g.pharmacie.adresse} · ${g.pharmacie.ville}")
                        },
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