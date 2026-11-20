package tn.pharmaciesdegarde.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("pharmacies")
    suspend fun getPharmacies(
        @Query("ville") ville: String? = null
    ): List<PharmacieDto>


    @POST("auth/login")    suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("admin/pharmacies")
    suspend fun createPharmacie(@Body body: PharmacieRequest): PharmacieDto
}