package tn.pharmaciesdegarde.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("pharmacies")
    suspend fun getPharmacies(
        @Query("ville") ville: String? = null
    ): List<PharmacieDto>
}