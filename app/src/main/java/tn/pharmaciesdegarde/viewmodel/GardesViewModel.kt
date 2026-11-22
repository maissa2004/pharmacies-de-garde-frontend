@file:SuppressLint("NewApi")
package tn.pharmaciesdegarde.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.data.remote.TypeGarde
import tn.pharmaciesdegarde.data.repository.GardeRepository
import tn.pharmaciesdegarde.data.repository.PharmacieRepository
import java.time.LocalDate

const val ALL_REGIONS = "Toutes les régions"
const val DEFAULT_REGION = "Tunis"

data class UserLocation(val lat: Double, val lng: Double)
data class GardesUiState(
    val gouvernorat: String = DEFAULT_REGION,
    val userLocation: UserLocation? = null,
    val date: LocalDate? = null,
    val loading: Boolean = true,
    val gardes: List<GardeDto> = emptyList(),
    val pharmacies: List<PharmacieDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val formError: String? = null
)

@HiltViewModel
class GardesViewModel @Inject constructor(
    private val gardeRepo: GardeRepository,
    private val pharmacieRepo: PharmacieRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GardesUiState())
    val state: StateFlow<GardesUiState> = _state

    init { loadPharmacies(); loadGardes() }

    private fun loadPharmacies() {
        viewModelScope.launch {
            pharmacieRepo.list().onSuccess { list -> _state.update { it.copy(pharmacies = list) } }
        }
    }

    fun selectRegion(g: String) {
        _state.update { it.copy(gouvernorat = g) }
        loadGardes()
    }

    fun loadGardes() {
        val g = _state.value.gouvernorat
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            gardeRepo.list(g.takeUnless { it == ALL_REGIONS })
                .onSuccess { list ->
                    // ignore une réponse arrivée après un changement de région
                    _state.update { if (it.gouvernorat == g) it.copy(loading = false, gardes = list) else it }
                }
                .onFailure { e ->
                    _state.update { if (it.gouvernorat == g) it.copy(loading = false, error = e.message) else it }
                }
        }
    }

    fun add(
        pharmacie: PharmacieDto?, debut: LocalDateTime?, fin: LocalDateTime?, type: TypeGarde?,
        onSuccess: () -> Unit
    ) {
        if (pharmacie == null || debut == null || fin == null || type == null) {
            _state.update { it.copy(formError = "Remplissez tous les champs") }; return
        }
        if (!fin.isAfter(debut)) {
            _state.update { it.copy(formError = "La date de fin doit être postérieure à la date de début") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, formError = null) }
            // la région envoyée est celle de la pharmacie choisie
            gardeRepo.create(pharmacie.id, pharmacie.gouvernorat, debut, fin, type)
                .onSuccess { created ->
                    val current = _state.value.gouvernorat
                    if (current == ALL_REGIONS ||
                        created.pharmacie.gouvernorat.equals(current, ignoreCase = true)
                    ) {
                        _state.update {
                            it.copy(saving = false, gardes = (it.gardes + created).sortedBy { x -> x.dateDebut })
                        }
                    } else {
                        // autre région que celle affichée : on affiche tout pour voir la nouvelle garde
                        _state.update { it.copy(saving = false, gouvernorat = ALL_REGIONS) }
                        loadGardes()
                    }
                    onSuccess()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, formError = e.message) } }
        }
    }

    fun clearFormError() { _state.update { it.copy(formError = null) } }

    fun selectDate(d: LocalDate?) { _state.update { it.copy(date = d) } }

    fun setUserLocation(lat: Double, lng: Double) {
        _state.update { it.copy(userLocation = UserLocation(lat, lng)) }
    }

    fun clearUserLocation() { _state.update { it.copy(userLocation = null) } }
}