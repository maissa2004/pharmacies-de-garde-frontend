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

data class PharmaciesUiState(
    val loading: Boolean = true,
    val pharmacies: List<PharmacieDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,      // erreur de chargement de la liste
    val formError: String? = null   // erreur du formulaire d'ajout
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

    fun add(nom: String, adresse: String, telephone: String, onSuccess: () -> Unit) {
        if (nom.isBlank() || adresse.isBlank() || telephone.isBlank()) {
            _state.value = _state.value.copy(formError = "Remplissez tous les champs"); return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            repo.create(nom, adresse, telephone)
                .onSuccess {
                    _state.value = _state.value.copy(
                        saving = false,
                        pharmacies = listOf(it) + _state.value.pharmacies
                    )
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, formError = it.message) }
        }
    }

    fun clearFormError() { _state.value = _state.value.copy(formError = null) }


    fun update(id: Long, nom: String, adresse: String, telephone: String, onSuccess: () -> Unit) {
        if (nom.isBlank() || adresse.isBlank() || telephone.isBlank()) {
            _state.value = _state.value.copy(formError = "Remplissez tous les champs"); return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, formError = null)
            repo.update(id, nom, adresse, telephone)
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        saving = false,
                        pharmacies = _state.value.pharmacies.map { if (it.id == updated.id) updated else it }
                    )
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, formError = it.message) }
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