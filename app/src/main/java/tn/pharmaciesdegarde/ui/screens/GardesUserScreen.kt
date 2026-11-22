@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.GardesViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

private val USER_DAY_HOUR = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val USER_HOUR = DateTimeFormatter.ofPattern("HH:mm")
private val USER_SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

// Non privées : la fiche pharmacie les réutilise
fun GardeDto.debut(): LocalDateTime = LocalDateTime.parse(dateDebut)
fun GardeDto.fin(): LocalDateTime = LocalDateTime.parse(dateFin)
fun GardeDto.isOngoing(now: LocalDateTime) = !debut().isAfter(now) && !fin().isBefore(now)

@Composable
fun GardesUserScreen(
    onBack: () -> Unit,
    onPharmacieClick: (Long) -> Unit,
    viewModel: GardesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRegions by rememberSaveable { mutableStateOf(false) }
    var onlyOpen by rememberSaveable { mutableStateOf(true) }
    var selectedType by rememberSaveable { mutableStateOf<TypeGarde?>(null) }
    var showTypes by rememberSaveable { mutableStateOf(false) }
    var showDates by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Par défaut : les gardes d'aujourd'hui (une seule fois, pas à chaque rotation)
    var initialised by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialised) { viewModel.selectDate(LocalDate.now()); initialised = true }
    }

    // L'heure est rafraîchie chaque minute : le badge « maintenant » reste juste
    val now by produceState(LocalDateTime.now()) {
        while (true) { delay(60_000); value = LocalDateTime.now() }
    }
    val isToday = state.date == now.toLocalDate()
    val tomorrow = now.toLocalDate().plusDays(1)

    // Gardes qui couvrent la date choisie ; celles en cours d'abord, puis par heure de début
    val visible = remember(state.gardes, state.date, now, selectedType) {
        val d = state.date
        val list = if (d == null) state.gardes else {
            val start = d.atStartOfDay()
            val end = start.plusDays(1)
            state.gardes.filter { it.debut().isBefore(end) && it.fin().isAfter(start) }
        }
        list
            .filter { selectedType == null || TypeGarde.from(it.type) == selectedType }
            .sortedWith(compareByDescending<GardeDto> { it.isOngoing(now) }.thenBy { it.debut() })
    }
    val ongoingCount = visible.count { it.isOngoing(now) }

    // « Ouvertes maintenant » : uniquement les gardes en cours (seulement pour aujourd'hui)
    val shown = remember(visible, isToday, onlyOpen, now) {
        if (isToday && onlyOpen) visible.filter { it.isOngoing(now) } else visible
    }

    Scaffold(containerColor = BackgroundLight) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextInk)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pharmacies de garde",
                    color = TextInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(12.dp))

// Barre de filtres compacte
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterButton(
                    label = state.gouvernorat,
                    icon = Icons.Default.LocationOn,
                    active = false,
                    onClick = { showRegions = true }
                )
                FilterButton(
                    label = when (state.date) {
                        null -> "Toutes dates"
                        now.toLocalDate() -> "Aujourd'hui"
                        tomorrow -> "Demain"
                        else -> state.date!!.format(USER_SHORT_DATE)
                    },
                    icon = Icons.Default.Event,
                    active = false,
                    onClick = { showDates = true }
                )
                FilterButton(
                    label = selectedType?.label ?: "Tous types",
                    icon = Icons.Default.FilterList,
                    active = selectedType != null,
                    onClick = { showTypes = true }
                )
                if (isToday) {
                    Text(
                        "Ouvertes maintenant",
                        color = if (onlyOpen) Color.White else TextInk,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (onlyOpen) GreenPharmacy else Color(0xFFF1F5F9))
                            .clickable { onlyOpen = !onlyOpen }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isToday && !state.loading && state.error == null) {
                Text(
                    if (ongoingCount > 0) "$ongoingCount pharmacie(s) de garde en ce moment"
                    else "Aucune pharmacie de garde en ce moment",
                    color = if (ongoingCount > 0) GreenPharmacy else TextMuted,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (ongoingCount > 0) GreenSoft else Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = RedClosed, fontSize = 13.sp)
                    TextButton(onClick = viewModel::loadGardes) { Text("Réessayer") }
                }
                shown.isEmpty() -> Text(
                    buildString {
                        if (isToday && onlyOpen) append("Aucune pharmacie ouverte en ce moment")
                        else append("Aucune pharmacie de garde pour cette date")
                        append(" à ${state.gouvernorat}")
                        selectedType?.let { append(" (${it.label.lowercase()})") }
                        append(".")
                        if (isToday && onlyOpen) append(" Désactivez « Ouvertes maintenant » pour voir toutes les gardes du jour.")
                    },
                    color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { g ->
                        GardeUserCard(
                            g = g,
                            now = now,
                            onClick = { onPharmacieClick(g.pharmacie.id) },
                            onCall = { dial(context, g.pharmacie.telephone) },
                            onDirections = { openInMaps(context, g.pharmacie) }
                        )
                    }
                }
            }
        }
    }
    if (showTypes) {
        PickerSheet(
            title = "Type de garde",
            items = listOf<TypeGarde?>(null) + TypeGarde.entries,
            label = { it?.label ?: "Tous types" },
            isSelected = { it == selectedType },
            onSelect = {
                selectedType = it
                if (it != null) onlyOpen = false   // évite une liste vide (ex. « nuit » en pleine journée)
                showTypes = false
            },
            onDismiss = { showTypes = false }
        )
    }

    if (showDates) {
        val today = now.toLocalDate()
        PickerSheet(
            title = "Date",
            items = listOf("Toutes dates", "Aujourd'hui", "Demain", "Choisir une date…"),
            label = { it },
            isSelected = {
                when (it) {
                    "Toutes dates" -> state.date == null
                    "Aujourd'hui" -> state.date == today
                    "Demain" -> state.date == tomorrow
                    else -> state.date != null && state.date != today && state.date != tomorrow
                }
            },
            onSelect = {
                when (it) {
                    "Toutes dates" -> viewModel.selectDate(null)
                    "Aujourd'hui" -> viewModel.selectDate(today)
                    "Demain" -> viewModel.selectDate(tomorrow)
                    else -> {
                        val base = state.date ?: today
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> viewModel.selectDate(LocalDate.of(y, m + 1, d)) },
                            base.year, base.monthValue - 1, base.dayOfMonth
                        ).show()
                    }
                }
                showDates = false
            },
            onDismiss = { showDates = false }
        )
    }
    if (showRegions) {
        RegionSheet(
            selected = state.gouvernorat,
            onSelect = { viewModel.selectRegion(it); showRegions = false },
            onDismiss = { showRegions = false }
        )
    }
}

@Composable
private fun GardeUserCard(
    g: GardeDto,
    now: LocalDateTime,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit
) {
    val ongoing = g.isOngoing(now)
    val type = TypeGarde.from(g.type)

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (ongoing) GreenPharmacy else Color(0xFFE5EAF2),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)          // ← ouvre la fiche
            .padding(14.dp)
    ) {
        // Badge d'état + flèche
        val badgeText = when {
            ongoing -> "DE GARDE MAINTENANT"
            g.debut().isAfter(now) -> "À PARTIR DE ${g.debut().format(USER_HOUR)}"
            else -> "GARDE TERMINÉE"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                badgeText,
                color = if (ongoing) GreenPharmacy else TextMuted,
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        if (ongoing) GreenSoft else Color(0xFFF1F5F9),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Voir la fiche", tint = TextMuted
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(g.pharmacie.nom, color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "${g.pharmacie.adresse} · ${g.pharmacie.ville}",
            color = TextMuted, fontSize = 12.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${type.label} · ${g.debut().format(USER_DAY_HOUR)} — ${g.fin().format(USER_DAY_HOUR)}",
            color = TextMuted, fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onCall,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Appeler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDirections,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Itinéraire", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Ouvre le composeur téléphonique : aucune permission nécessaire, l'utilisateur appuie lui-même sur « appeler »
fun dial(context: Context, phone: String) {
    val number = phone.filter { it.isDigit() || it == '+' }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
    }
}

@Composable
private fun FilterButton(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val fg = if (active) Color.White else TextInk
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (active) PrimaryBlue else Color.White, shape)
            .border(1.dp, if (active) PrimaryBlue else Color(0xFFE5EAF2), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (active) Color.White else PrimaryBlue)
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(2.dp))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = fg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PickerSheet(
    title: String,
    items: List<T>,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Text(
            title,
            color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        items.forEach { item ->
            val selected = isSelected(item)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label(item), fontSize = 14.sp,
                    color = if (selected) PrimaryBlue else TextInk,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}