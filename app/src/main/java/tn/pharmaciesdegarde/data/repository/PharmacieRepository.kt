package tn.pharmaciesdegarde.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import tn.pharmaciesdegarde.data.remote.ApiService
import tn.pharmaciesdegarde.data.remote.ErrorResponse
import tn.pharmaciesdegarde.data.remote.GeocodeRequest
import tn.pharmaciesdegarde.data.remote.GeocodeResponse
import tn.pharmaciesdegarde.data.remote.PharmacieDto
import tn.pharmaciesdegarde.data.remote.PharmacieRequest
import tn.pharmaciesdegarde.data.remote.PharmacieUpdateRequest
import tn.pharmaciesdegarde.data.remote.ReverseGeocodeResponse

@Singleton
class PharmacieRepository @Inject constructor(
    private val api: ApiService,
    private val json: Json
) {
    suspend fun list(): Result<List<PharmacieDto>> = safe { api.getPharmacies() }

    suspend fun create(
        nom: String, adresse: String, ville: String, gouvernorat: String,
        telephone: String, latitude: Double, longitude: Double
    ): Result<PharmacieDto> = safe {
        api.createPharmacie(
            PharmacieRequest(
                nom.trim(), adresse.trim(), telephone.trim(),
                ville.trim(), gouvernorat.trim(), latitude, longitude
            )
        )
    }
    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val msg = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ErrorResponse>(it).message }.getOrNull() }
        Result.failure(Exception(msg ?: "Erreur serveur (${e.code()})"))
    } catch (e: IOException) {
        Result.failure(Exception("Impossible de joindre le serveur"))
    }


    suspend fun update(id: Long, nom: String, adresse: String, telephone: String): Result<PharmacieDto> =
        safe { api.updatePharmacie(id,
            PharmacieUpdateRequest(nom.trim(), adresse.trim(), telephone.trim())
        ) }

    suspend fun geocode(adresse: String, ville: String, gouvernorat: String): Result<GeocodeResponse> =
        safe { api.geocode(GeocodeRequest(adresse.trim(), ville.trim(), gouvernorat.trim())) }
    suspend fun delete(id: Long): Result<Unit> = safe { api.deletePharmacie(id) }

    suspend fun reverseGeocode(lat: Double, lng: Double): Result<ReverseGeocodeResponse> =
        safe { api.reverseGeocode(lat, lng) }
}