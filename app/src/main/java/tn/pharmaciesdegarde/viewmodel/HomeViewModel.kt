package tn.pharmaciesdegarde.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tn.pharmaciesdegarde.data.remote.ApiService
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _pharmacies = MutableStateFlow<List<PharmacieDto>>(emptyList())
    val pharmacies: StateFlow<List<PharmacieDto>> = _pharmacies

    init {
        fetchPharmacies()
    }

    private fun fetchPharmacies() {
        viewModelScope.launch {
            try {
                _pharmacies.value = apiService.getPharmacies()
            } catch (e: Exception) {
                println("Erreur API : ${e.message}")
            }
        }
    }
}