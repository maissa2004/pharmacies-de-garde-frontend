package tn.pharmaciesdegarde.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPharmacy
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmaciesScreen(
    onBack: () -> Unit,
    viewModel: PharmaciesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showForm by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(state.pharmacies, query) {
        val q = query.trim()
        if (q.isEmpty()) state.pharmacies
        else state.pharmacies.filter {
            it.nom.contains(q, true) || it.adresse.contains(q, true) || it.ville.contains(q, true)
        }
    }

    Scaffold(containerColor = BackgroundLight) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // En-tête
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextInk)
                }
                Spacer(Modifier.width(8.dp))
                Text("Pharmacies", color = TextInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Nom, ville ou numéro d'ordre") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} établissement(s)", color = TextMuted, fontSize = 12.sp)
                Button(
                    onClick = { viewModel.clearFormError(); showForm = true },
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
                    "Aucune pharmacie", color = TextMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { PharmacieCard(it) }
                }
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            AddPharmacieForm(
                saving = state.saving,
                error = state.formError,
                onClose = { showForm = false },
                onSubmit = { nom, adresse, tel ->
                    viewModel.add(nom, adresse, tel) { showForm = false }
                },
                onTyping = viewModel::clearFormError
            )
        }
    }
}

@Composable
private fun PharmacieCard(p: PharmacieDto) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5EAF2), RoundedCornerShape(16.dp))
            .padding(14.dp),
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
    }
}

@Composable
private fun AddPharmacieForm(
    saving: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    onTyping: () -> Unit
) {
    var nom by rememberSaveable { mutableStateOf("") }
    var adresse by rememberSaveable { mutableStateOf("") }
    var telephone by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ajouter une pharmacie", color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
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
            label = { Text("Adresse") }, placeholder = { Text("Rue, ville, gouvernorat") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = telephone, onValueChange = { telephone = it; onTyping() },
            label = { Text("Téléphone") }, placeholder = { Text("+216…") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedClosed, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(nom, adresse, telephone) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Enregistrer", fontWeight = FontWeight.Bold)
        }
    }
}