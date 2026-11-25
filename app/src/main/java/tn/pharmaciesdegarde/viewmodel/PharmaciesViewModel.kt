package tn.pharmaciesdegarde.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.data.repository.PharmacieRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
data class PharmaciesUiState(
    val loading: Boolean = true,
    val pharmacies: List<PharmacieDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,       // erreur de chargement de la liste
    val formError: String? = null,    // erreur du formulaire d'ajout
    val geo: GeoState = GeoState()
)
// Volontairement PAS une data class : chaque géocodage crée un nouvel objet,
// donc la carte se recentre même si les coordonnées sont identiques.
class MapFocus(val lat: Double, val lng: Double)
class AddressFill(val adresse: String, val ville: String, val gouvernorat: String)

data class GeoState(
    val loading: Boolean = false,
    val lat: Double? = null,       // position qui sera enregistrée (null = pas encore validée)
    val lng: Double? = null,
    val focus: MapFocus? = null,   // dernier résultat du géocodage : la carte se centre dessus
    val fill: AddressFill? = null,
    val message: String? = null
)
@HiltViewModel
class PharmaciesViewModel @Inject constructor(
    private val repo: PharmacieRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PharmaciesUiState())
    val state: StateFlow<PharmaciesUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repo.list()
                .onSuccess { _state.value = _state.value.copy(loading = false, pharmacies = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun add(
        nom: String, adresse: String, ville: String, gouvernorat: String,
        telephone: String, onSuccess: () -> Unit
    ) {
        if (nom.isBlank() || adresse.isBlank() || ville.isBlank() ||
            gouvernorat.isBlank() || telephone.isBlank()
        ) {
            _state.update { it.copy(formError = "Remplissez tous les champs") }; return
        }
        val lat = _state.value.geo.lat
        val lng = _state.value.geo.lng
        if (lat == null || lng == null) {
            _state.update { it.copy(formError = "Localisez la pharmacie sur la carte avant d'enregistrer") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, formError = null) }
            repo.create(nom, adresse, ville, gouvernorat, telephone, lat, lng)
                .onSuccess { created ->
                    _state.update {
                        it.copy(saving = false, pharmacies = listOf(created) + it.pharmacies, geo = GeoState())
                    }
                    onSuccess()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, formError = e.message) } }
        }
    }

    fun geocode(adresse: String, ville: String, gouvernorat: String) {
        if (adresse.isBlank() || ville.isBlank() || gouvernorat.isBlank()) {
            _state.update {
                it.copy(geo = it.geo.copy(message = "Renseignez l'adresse, la ville et le gouvernorat"))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(geo = it.geo.copy(loading = true, message = null), formError = null) }
            repo.geocode(adresse, ville, gouvernorat)
                .onSuccess { r ->
                    val focus = MapFocus(r.latitude, r.longitude)
                    _state.update {
                        it.copy(
                            geo = if (r.precise) GeoState(
                                lat = r.latitude, lng = r.longitude, focus = focus,
                                message = "Position trouvée. Déplacez le marqueur si besoin."
                            ) else GeoState(
                                // approximatif : on centre la carte mais on NE valide PAS la position
                                focus = focus,
                                message = "Adresse non trouvée : position approximative. " +
                                        "Placez le marqueur sur la pharmacie pour valider."
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(geo = it.geo.copy(loading = false, message = e.message)) }
                }
        }
    }

    // Appelée par la carte (glisser ou toucher) : elle peut venir d'un autre thread, d'où update { }
    private var reverseJob: Job? = null

    // Appelée par la carte (glisser ou toucher)
    fun setPosition(lat: Double, lng: Double) {
        _state.update {
            it.copy(
                geo = it.geo.copy(lat = lat, lng = lng, message = "Recherche de l'adresse…"),
                formError = null
            )
        }
        reverseJob?.cancel()   // un seul appel à la fois (Nominatim limite à 1 requête/seconde)
        reverseJob = viewModelScope.launch {
            repo.reverseGeocode(lat, lng)
                .onSuccess { r ->
                    _state.update {
                        it.copy(
                            geo = it.geo.copy(
                                fill = AddressFill(r.adresse, r.ville, r.gouvernorat),
                                message = "Adresse mise à jour d'après la carte. Vérifiez-la."
                            )
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            geo = it.geo.copy(
                                message = "Position enregistrée, mais l'adresse n'a pas pu être déduite. " +
                                        "Complétez-la à la main."
                            )
                        )
                    }
                }
        }
    }

    fun resetGeo() { _state.update { it.copy(geo = GeoState()) } }


    fun clearFormError() { _state.value = _state.value.copy(formError = null) }


    fun update(
        id: Long, nom: String, adresse: String, telephone: String,
        lat: Double?, lng: Double?, onSuccess: () -> Unit
    ) {
        if (nom.isBlank() || adresse.isBlank() || telephone.isBlank()) {
            _state.update { it.copy(formError = "Remplissez tous les champs") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, formError = null) }
            repo.update(id, nom, adresse, telephone, lat, lng)
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            saving = false,
                            pharmacies = it.pharmacies.map { p -> if (p.id == updated.id) updated else p },
                            geo = GeoState()
                        )
                    }
                    onSuccess()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, formError = e.message) } }
        }
    }

    fun delete(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            repo.delete(id)
                .onSuccess {
                    _state.value = _state.value.copy(
                        saving = false,
                        pharmacies = _state.value.pharmacies.filter { it.id != id }
                    )
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, formError = it.message) }
        }
    }
}