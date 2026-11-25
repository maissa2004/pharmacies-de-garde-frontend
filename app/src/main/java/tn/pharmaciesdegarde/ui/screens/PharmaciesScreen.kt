package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.PharmaciesViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import java.text.Normalizer
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.MyLocation
import tn.pharmaciesdegarde.viewmodel.GeoState
import androidx.compose.ui.platform.LocalFocusManager
import tn.pharmaciesdegarde.viewmodel.MapFocus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmaciesScreen(
    onBack: () -> Unit,
    viewModel: PharmaciesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = state.pharmacies.find { it.id == editingId }

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val indexed = remember(state.pharmacies) {
        state.pharmacies.map { it to it.searchText() }
    }
    val filtered = remember(indexed, query) {
        val tokens = query.normalized().split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) state.pharmacies
        else indexed.filter { (_, text) -> tokens.all { it in text } }.map { it.first }
    }
    val searching = query.isNotBlank()

    var page by rememberSaveable { mutableIntStateOf(0) }
    val totalPages = maxOf(1, (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val currentPage = page.coerceIn(0, totalPages - 1)   // évite une page vide après une suppression
    val pageItems = remember(filtered, currentPage) {
        filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    }
    val from = if (filtered.isEmpty()) 0 else currentPage * PAGE_SIZE + 1
    val to = minOf((currentPage + 1) * PAGE_SIZE, filtered.size)

// remonte en haut quand on change de page ou de recherche
    LaunchedEffect(query, currentPage) {
        listState.scrollToItem(0)
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = { AdminHeader(title = "Pharmacies", onBack = onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; page = 0 },
                placeholder = { Text("Nom, ville ou téléphone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; page = 0 }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        searching -> "${filtered.size} résultat(s) sur ${state.pharmacies.size}"
                        totalPages > 1 -> "$from–$to sur ${filtered.size}"
                        else -> "${filtered.size} établissement(s)"
                    },
                    color = TextMuted, fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.clearFormError(); viewModel.resetGeo(); showAdd = true },
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
                    TextButton(onClick = viewModel::load) { Text("Réessayer") }
                }
                filtered.isEmpty() -> Text(
                    if (searching) "Aucun résultat pour « ${query.trim()} »" else "Aucune pharmacie",
                    color = TextMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pageItems, key = { it.id }) { p ->
                        PharmacieCard(
                            p = p,
                            onMenuClick = { viewModel.clearFormError(); viewModel.resetGeo(); editingId = p.id }
                        )
                    }
                }
            }   // ← fin du when

            if (!state.loading && state.error == null && totalPages > 1) {
                PaginationBar(current = currentPage, total = totalPages, onPage = { page = it })
            }
        }   // ← fin de la Column
    }       // ← fin du Scaffold



    if (showAdd || editing != null) {
        ModalBottomSheet(
            onDismissRequest = { showAdd = false; editingId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            PharmacieForm(
                initial = editing,
                saving = state.saving,
                error = state.formError,
                geo = state.geo,
                onClose = { showAdd = false; editingId = null },
                onGeocode = viewModel::geocode,
                onPositionChanged = viewModel::setPosition,
                onSubmit = { nom, adresse, ville, gouvernorat, tel ->
                    if (editing != null) {
                        viewModel.update(
                            editing.id, nom, adresse, tel,
                            state.geo.lat, state.geo.lng
                        ) { editingId = null }
                    } else {
                        viewModel.add(nom, adresse, ville, gouvernorat, tel) {
                            showAdd = false; query = ""; page = 0
                        }
                    }
                },
                onDelete = editing?.let { e -> { viewModel.delete(e.id) { editingId = null } } },
                onTyping = viewModel::clearFormError
            )
        }
    }
}

@Composable
private fun PharmacieCard(p: PharmacieDto, onMenuClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(PrimaryBlueSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = PrimaryBlue) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.nom, color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(p.adresse, color = TextMuted, fontSize = 11.sp)
            Text(p.telephone, color = TextMuted, fontSize = 11.sp)
        }
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Modifier ${p.nom}", tint = TextMuted)
        }
    }
}

internal val GOUVERNORATS = listOf(
    "Ariana", "Béja", "Ben Arous", "Bizerte", "Gabès", "Gafsa", "Jendouba", "Kairouan",
    "Kasserine", "Kébili", "Le Kef", "Mahdia", "Manouba", "Médenine", "Monastir", "Nabeul",
    "Sfax", "Sidi Bouzid", "Siliana", "Sousse", "Tataouine", "Tozeur", "Tunis", "Zaghouan"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GouvernoratDropdown(value: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text("Gouvernorat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GOUVERNORATS.forEach { g ->
                DropdownMenuItem(text = { Text(g) }, onClick = { onSelected(g); expanded = false })
            }
        }
    }
}

@Composable
private fun PharmacieForm(
    initial: PharmacieDto?,            // null = ajout, non null = modification
    saving: Boolean,
    error: String?,
    geo: GeoState,
    onClose: () -> Unit,
    onGeocode: (String, String, String) -> Unit,
    onPositionChanged: (Double, Double) -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit,
    onDelete: (() -> Unit)?,
    onTyping: () -> Unit
) {
    val isEdit = initial != null
    val focusManager = LocalFocusManager.current
    var nom by rememberSaveable(initial?.id) { mutableStateOf(initial?.nom ?: "") }
    var adresse by rememberSaveable(initial?.id) { mutableStateOf(initial?.adresse ?: "") }
    var ville by rememberSaveable(initial?.id) { mutableStateOf(initial?.ville ?: "") }
    var gouvernorat by rememberSaveable(initial?.id) { mutableStateOf(initial?.gouvernorat ?: "") }
    var telephone by rememberSaveable(initial?.id) { mutableStateOf(initial?.telephone ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    // Quand la carte renvoie une adresse, on remplit le formulaire (sans écraser avec du vide)
    LaunchedEffect(geo.fill) {
        geo.fill?.let {
            if (it.adresse.isNotBlank()) adresse = it.adresse
            // ville et gouvernorat sont cachés en modification et ne sont pas envoyés
            if (!isEdit) {
                if (it.ville.isNotBlank()) ville = it.ville
                if (it.gouvernorat.isNotBlank()) gouvernorat = it.gouvernorat
            }
        }
    }
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
            Text(
                if (isEdit) "Modifier la pharmacie" else "Ajouter une pharmacie",
                color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = nom, onValueChange = { nom = it; onTyping() },
            label = { Text("Nom officiel") }, placeholder = { Text("Pharmacie…") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = adresse, onValueChange = { adresse = it; onTyping() },
            label = { Text("Adresse") }, placeholder = { Text("Rue, numéro, quartier") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        if (!isEdit) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = ville, onValueChange = { ville = it; onTyping() },
                label = { Text("Ville") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(10.dp))
            GouvernoratDropdown(value = gouvernorat, onSelected = { gouvernorat = it; onTyping() })
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = telephone, onValueChange = { telephone = it; onTyping() },
            label = { Text("Téléphone") }, placeholder = { Text("+216…") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
        )
        // --- Localisation (ajout ET modification) ---
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = { focusManager.clearFocus(); onGeocode(adresse, ville, gouvernorat) },
            enabled = !geo.loading && !saving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (geo.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Localiser sur la carte", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        geo.message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = if (geo.lat != null) TextMuted else RedClosed, fontSize = 12.sp)
        }

// En modification, la carte démarre sur la position actuelle de la pharmacie
        // En modification, la carte démarre sur la position actuelle (objet créé une seule fois)
        val initialFocus = remember(initial?.id) {
            initial?.let { MapFocus(it.latitude, it.longitude) }
        }
        val mapFocus = geo.focus ?: initialFocus
        val shownLat = geo.lat ?: initial?.latitude
        val shownLng = geo.lng ?: initial?.longitude

        Spacer(Modifier.height(8.dp))
        PharmacieMapPicker(
            focus = mapFocus,
            onPositionChanged = onPositionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (shownLat != null && shownLng != null)
                "Position : %.5f, %.5f".format(shownLat, shownLng)
            else "Aucune position choisie : touchez la carte ou utilisez « Localiser »",
            color = TextMuted, fontSize = 11.sp
        )





        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedClosed, fontSize = 12.sp)
        }

        if (onDelete != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { confirmDelete = true },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFDECEC),
                    contentColor = RedClosed
                )
            ) { Text("Supprimer la pharmacie", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSubmit(nom, adresse, ville, gouvernorat, telephone) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Enregistrer", fontWeight = FontWeight.Bold)
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer la pharmacie ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Supprimer", color = RedClosed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}
private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")

// minuscules + sans accents + sans espaces aux extrémités
private fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .trim()

// tout ce qui est cherchable dans une pharmacie, calculé une seule fois par pharmacie
private fun PharmacieDto.searchText(): String =
    listOf(nom, adresse, ville, gouvernorat, telephone, telephone.replace(" ", ""))
        .joinToString(" ")
        .normalized()

private const val PAGE_SIZE = 10

@Composable
private fun PaginationBar(current: Int, total: Int, onPage: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = "Page précédente",
            enabled = current > 0,
            onClick = { onPage(current - 1) }
        )
        pageWindow(current, total).forEach { p ->
            if (p == null) {
                Text(
                    "…", color = TextMuted, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.width(16.dp)
                )
            } else {
                PageButton(label = "${p + 1}", selected = p == current, onClick = { onPage(p) })
            }
        }
        PageButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = "Page suivante",
            enabled = current < total - 1,
            onClick = { onPage(current + 1) }
        )
    }
}

@Composable
private fun PageButton(
    label: String? = null,
    icon: ImageVector? = null,
    description: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = when {
        selected -> Color.White
        enabled -> TextInk
        else -> TextMuted.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryBlue else Color.White)
            .border(1.dp, if (selected) PrimaryBlue else BorderLine, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled && !selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = description, tint = contentColor, modifier = Modifier.size(18.dp))
        } else {
            Text(label ?: "", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Toujours 7 emplacements max : 1 … 4 5 6 … 49 (null = « … »)
private fun pageWindow(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (0 until total).toList()
    return when {
        current <= 3 -> listOf(0, 1, 2, 3, 4, null, total - 1)
        current >= total - 4 -> listOf(0, null) + (total - 5 until total).toList()
        else -> listOf(0, null, current - 1, current, current + 1, null, total - 1)
    }
}