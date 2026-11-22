@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.GardesViewModel
import java.text.Normalizer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.util.Locale
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import tn.pharmaciesdegarde.viewmodel.ALL_REGIONS


private val DAY_HOUR = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardesScreen(
    onBack: () -> Unit,
    onAccueil: () -> Unit,
    onAdministration: () -> Unit,
    viewModel: GardesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showRegions by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var initialised by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialised) { viewModel.selectRegion(ALL_REGIONS); initialised = true }
    }

    // gardes qui couvrent la date choisie (toutes si aucune date)
    val visible = remember(state.gardes, state.date) {
        val d = state.date
        if (d == null) state.gardes
        else {
            val start = d.atStartOfDay()
            val end = start.plusDays(1)
            state.gardes.filter {
                LocalDateTime.parse(it.dateDebut).isBefore(end) &&
                        LocalDateTime.parse(it.dateFin).isAfter(start)
            }
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = { AdminHeader(title = "Plannings de garde", onBack = onBack) },
        bottomBar = { AdminBottomBar(AdminTab.ADMINISTRATION, onAccueil, onAdministration) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            FiltersCard(
                gouvernorat = state.gouvernorat,
                date = state.date,
                onRegionClick = { showRegions = true },
                onDate = viewModel::selectDate
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    buildString {
                        append("${visible.size} garde(s) · ${state.gouvernorat}")
                        state.date?.let { append(" · ${it.format(SHORT_DATE)}") }
                    },
                    color = TextMuted, fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.clearFormError(); showAdd = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("+ Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(12.dp))

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
                visible.isEmpty() -> Text(
                    if (state.date != null) "Aucune garde pour cette date" else "Aucune garde planifiée pour cette région",
                    color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visible, key = { it.id }) { g ->
                        GardeCard(g, onClick = { openInMaps(context, g.pharmacie) })
                    }
                }
            }
        }
    }

    if (showRegions) {
        RegionSheet(
            selected = state.gouvernorat,
            onSelect = { viewModel.selectRegion(it); showRegions = false },
            onDismiss = { showRegions = false },
            includeAll = true
        )
    }

    if (showAdd) {
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            GardeForm(
                pharmacies = state.pharmacies,
                saving = state.saving,
                error = state.formError,
                onClose = { showAdd = false },
                onSubmit = { p, debut, fin, type ->
                    viewModel.add(p, debut, fin, type) { showAdd = false }
                },
                onTyping = viewModel::clearFormError
            )
        }
    }
}

@Composable
private fun GardeCard(g: GardeDto, onClick: () -> Unit) {
    val type = TypeGarde.from(g.type)
    val night = type == TypeGarde.NUIT
    val debut = LocalDateTime.parse(g.dateDebut).format(DAY_HOUR)
    val fin = LocalDateTime.parse(g.dateFin).format(DAY_HOUR)

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)          // ← carte cliquable
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(
                if (night) Color(0xFFFFF4E5) else PrimaryBlueSoft, RoundedCornerShape(12.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (night) Icons.Default.Schedule else Icons.Default.CalendarToday,
                contentDescription = null,
                tint = if (night) Color(0xFFB7791F) else PrimaryBlue
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(type.label.uppercase(), color = PrimaryBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(g.pharmacie.nom, color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${g.pharmacie.ville} · ${g.pharmacie.gouvernorat}", color = TextMuted, fontSize = 11.sp)
            Text("$debut — $fin", color = TextMuted, fontSize = 11.sp)
        }
        // indique qu'un toucher ouvre la carte
        Icon(
            Icons.Default.LocationOn,
            contentDescription = "Voir sur la carte",
            tint = PrimaryBlue
        )
    }
}

@Composable
private fun GardeForm(
    pharmacies: List<PharmacieDto>,
    saving: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSubmit: (PharmacieDto?, LocalDateTime?, LocalDateTime?, TypeGarde?) -> Unit,
    onTyping: () -> Unit
) {
    var pharmacie by remember { mutableStateOf<PharmacieDto?>(null) }
    var debut by remember { mutableStateOf<LocalDateTime?>(null) }
    var fin by remember { mutableStateOf<LocalDateTime?>(null) }
    var type by remember { mutableStateOf<TypeGarde?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ajouter une garde", color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
        }
        Spacer(Modifier.height(12.dp))

        if (pharmacies.isEmpty()) {
            Text("Aucune pharmacie enregistrée", color = RedClosed, fontSize = 12.sp)
        } else {
            PharmacieField(
                pharmacies = pharmacies,
                selected = pharmacie,
                onSelected = { pharmacie = it; onTyping() }
            )
            pharmacie?.let {
                Spacer(Modifier.height(4.dp))
                Text("Région : ${it.gouvernorat}", color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        DateTimeField("Début de la garde", debut) { debut = it; onTyping() }
        Spacer(Modifier.height(10.dp))
        DateTimeField("Fin de la garde", fin) { fin = it; onTyping() }
        Spacer(Modifier.height(10.dp))
        DropdownField(
            label = "Type de garde",
            value = type?.label ?: "",
            items = TypeGarde.entries.toList(),
            itemLabel = { it.label },
            onSelected = { type = it; onTyping() }
        )
        type?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                when (it.name) {
                    "WEEK_END" -> "Début et fin un samedi ou un dimanche"
                    "JOUR" -> "Début et fin le même jour"
                    "NUIT" -> "À partir de 18:00, jusqu'au lendemain 12:00 au plus tard"
                    "VINGT_QUATRE_H" -> "Durée exacte de 24 heures"
                    else -> ""
                },
                color = TextMuted, fontSize = 11.sp
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedClosed, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(pharmacie, debut, fin, type) },            enabled = !saving && pharmacies.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Enregistrer", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = { onSelected(item); expanded = false }
                )
            }
        }
    }
}

// Date puis heure, via les sélecteurs natifs Android
// java.time est disponible sur API 24 grâce au desugaring (faux positif de lint)
@SuppressLint("NewApi")
@Composable
private fun DateTimeField(label: String, value: LocalDateTime?, onPicked: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    Box {
        OutlinedTextField(
            value = value?.format(DAY_HOUR) ?: "",
            onValueChange = {}, readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Choisir date et heure") },
            trailingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize().clickable {
                val base = value ?: LocalDateTime.now()
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        TimePickerDialog(
                            context,
                            { _, h, min -> onPicked(LocalDateTime.of(y, m + 1, d, h, min)) },
                            base.hour, base.minute, true
                        ).show()
                    },
                    base.year, base.monthValue - 1, base.dayOfMonth
                ).show()
            }
        )
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PharmacieField(
    pharmacies: List<PharmacieDto>,
    selected: PharmacieDto?,
    onSelected: (PharmacieDto?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf(selected?.nom ?: "") }
    var open by remember { mutableStateOf(false) }

    // texte cherchable calculé une seule fois par pharmacie
    val indexed = remember(pharmacies) { pharmacies.map { it to it.searchKey() } }

    val results = remember(indexed, query, selected) {
        val tokens = if (selected != null) emptyList()
        else query.foldForSearch().split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) pharmacies
        else indexed.filter { (_, key) -> tokens.all { it in key } }.map { it.first }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSelected(null); open = true },
            label = { Text("Pharmacie") },
            placeholder = { Text("Nom, ville ou quartier") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""; onSelected(null); open = true
                        focusRequester.requestFocus()
                    }) { Icon(Icons.Default.Close, contentDescription = "Effacer") }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { open = it.isFocused }
        )

        if (open) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(12.dp))
            ) {
                if (results.isEmpty()) {
                    Text(
                        "Aucune pharmacie ne correspond",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    LazyColumn {
                        items(results, key = { it.id }) { p ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = p.nom
                                        onSelected(p)
                                        open = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(p.nom, color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${p.adresse} · ${p.ville} · ${p.gouvernorat}", color = TextMuted, fontSize = 11.sp)                            }
                            HorizontalDivider(color = Color(0xFFE5EAF2))
                        }
                    }
                }
            }
        }
    }
}

private val ACCENTS = Regex("\\p{InCombiningDiacriticalMarks}+")

// minuscules, sans accents, sans espaces aux extrémités
private fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(ACCENTS, "")
        .lowercase()
        .replace(SEPARATORS, " ")
        .trim()

// tout ce qui est cherchable : nom, adresse (quartier, rue), ville, gouvernorat, téléphone
private fun PharmacieDto.searchKey(): String =
    listOf(nom, adresse, ville, gouvernorat, telephone).joinToString(" ").foldForSearch()

@Composable
 fun FiltersCard(
    gouvernorat: String,
    date: LocalDate?,
    onRegionClick: () -> Unit,
    onDate: (LocalDate?) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }
    val custom = date != null && date != today && date != tomorrow

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
    ) {
        // Région
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clickable(onClick = onRegionClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).background(PrimaryBlueSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Région", color = TextMuted, fontSize = 10.sp)
                Text(gouvernorat, color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Changer de région", tint = TextMuted)
        }

        HorizontalDivider(color = Color(0xFFE5EAF2))

        // Date
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DatePill("Toutes", selected = date == null) { onDate(null) }
            DatePill("Aujourd'hui", selected = date == today) { onDate(today) }
            DatePill("Demain", selected = date == tomorrow) { onDate(tomorrow) }
            DatePill(
                label = if (custom) date!!.format(SHORT_DATE) else "Choisir",
                selected = custom,
                icon = Icons.Default.Event
            ) {
                val base = date ?: today
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onDate(LocalDate.of(y, m + 1, d)) },
                    base.year, base.monthValue - 1, base.dayOfMonth
                ).show()
            }
        }
    }
}

@Composable
private fun DatePill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryBlue else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon, contentDescription = null, modifier = Modifier.size(14.dp),
                tint = if (selected) Color.White else TextMuted
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextInk
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    includeAll: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Text(
            "Choisir une région",
            color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.heightIn(max = 460.dp)) {
            items(if (includeAll) listOf(ALL_REGIONS) + GOUVERNORATS else GOUVERNORATS) { g ->
                val isSelected = g == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(g) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        g, fontSize = 14.sp,
                        color = if (isSelected) PrimaryBlue else TextInk,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}




// Ouvre la position de la pharmacie dans l'appli de cartes (Google Maps, etc.)
 fun openInMaps(context: Context, p: PharmacieDto) {
    val label = Uri.encode(p.nom)
    val geo = Uri.parse("geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}($label)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        // aucune appli de cartes : on ouvre la page web
        val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }
}
@Composable
fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextInk,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryBlue else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}