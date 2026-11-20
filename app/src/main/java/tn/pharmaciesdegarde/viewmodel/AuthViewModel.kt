package tn.pharmaciesdegarde.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tn.pharmaciesdegarde.data.local.TokenStore
import tn.pharmaciesdegarde.data.repository.AuthRepository

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Remplissez tous les champs"); return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            repo.login(email, password)
                .onSuccess { _state.value = AuthUiState(); onSuccess() }
                .onFailure { _state.value = AuthUiState(error = it.message) }
        }
    }
    // register(nom, email, password, onSuccess) : même schéma avec repo.register
    fun clearError() { _state.value = _state.value.copy(error = null) }
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val tokenStore: TokenStore) : ViewModel() {
    val loggedIn: StateFlow<Boolean?> = tokenStore.token
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null) // null = chargement
    val nom: StateFlow<String> = tokenStore.nom.map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    fun logout() = viewModelScope.launch { tokenStore.clear() }
}