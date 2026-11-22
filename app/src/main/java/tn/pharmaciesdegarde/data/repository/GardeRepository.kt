package tn.pharmaciesdegarde.data.repository

import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import tn.pharmaciesdegarde.data.remote.*

@Singleton
class GardeRepository @Inject constructor(
    private val api: ApiService,
    private val json: Json
) {
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun list(gouvernorat: String?): Result<List<GardeDto>> =
        safe { api.getGardes(gouvernorat) }

    suspend fun create(
        pharmacieId: Long, gouvernorat: String,
        debut: LocalDateTime, fin: LocalDateTime, type: TypeGarde
    ): Result<GardeDto> = safe {
        api.createGarde(
            GardeRequest(pharmacieId, gouvernorat, debut.format(iso), fin.format(iso), type.name)
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
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("Réponse inattendue du serveur : ${e.message}"))
    }

}