package tn.pharmaciesdegarde.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import tn.pharmaciesdegarde.data.remote.ApiService
import tn.pharmaciesdegarde.data.remote.ErrorResponse
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.data.remote.PharmacieRequest

@Singleton
class PharmacieRepository @Inject constructor(
    private val api: ApiService,
    private val json: Json
) {
    suspend fun list(): Result<List<PharmacieDto>> = safe { api.getPharmacies() }

    suspend fun create(nom: String, adresse: String, telephone: String): Result<PharmacieDto> =
        safe { api.createPharmacie(PharmacieRequest(nom.trim(), adresse.trim(), telephone.trim())) }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val msg = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ErrorResponse>(it).message }.getOrNull() }
        Result.failure(Exception(msg ?: "Erreur serveur (${e.code()})"))
    } catch (e: IOException) {
        Result.failure(Exception("Impossible de joindre le serveur"))
    }
}