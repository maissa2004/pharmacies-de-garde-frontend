package tn.pharmaciesdegarde.data.remote
import kotlinx.serialization.Serializable
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val nom: String, val email: String, val password: String)
@Serializable data class AuthResponse(val token: String, val email: String, val nom: String = "", val role: String)
@Serializable data class ErrorResponse(val message: String)