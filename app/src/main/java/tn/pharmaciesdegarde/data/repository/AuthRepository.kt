package tn.pharmaciesdegarde.data.repository

import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import tn.pharmaciesdegarde.data.local.TokenStore
import tn.pharmaciesdegarde.data.remote.ApiService
import tn.pharmaciesdegarde.data.remote.AuthResponse
import tn.pharmaciesdegarde.data.remote.ErrorResponse
import tn.pharmaciesdegarde.data.remote.LoginRequest
import tn.pharmaciesdegarde.data.remote.RegisterRequest
import java.io.IOException

@Singleton
class
AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val json: Json
) {
    suspend fun login(email: String, password: String): Result<String> =
        call { api.login(LoginRequest(email.trim().lowercase(), password)) }

    suspend fun register(nom: String, email: String, password: String): Result<String> =
        call { api.register(RegisterRequest(nom.trim(), email.trim().lowercase(), password)) }

    private suspend fun call(block: suspend () -> AuthResponse): Result<String> = try {
        val r = block()
        tokenStore.save(r.token, r.nom, r.role)
        Result.success(r.role)
    } catch (e: HttpException) {
        val msg = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ErrorResponse>(it).message }.getOrNull() }
        Result.failure(Exception(msg ?: "Erreur serveur (${e.code()})"))
    } catch (e: IOException) {
        Result.failure(Exception("Impossible de joindre le serveur"))
    }
}