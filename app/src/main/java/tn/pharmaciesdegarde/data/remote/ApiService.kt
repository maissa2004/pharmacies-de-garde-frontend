package tn.pharmaciesdegarde.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @PUT("admin/pharmacies/{id}")
    suspend fun updatePharmacie(@Path("id") id: Long, @Body body: PharmacieUpdateRequest): PharmacieDto

    @POST("admin/pharmacies/geocode")
    suspend fun geocode(@Body body: GeocodeRequest): GeocodeResponse
    @DELETE("admin/pharmacies/{id}")
    suspend fun deletePharmacie(@Path("id") id: Long)


    @GET("admin/pharmacies/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): ReverseGeocodeResponse
    @GET("gardes")
    suspend fun getGardes(@Query("gouvernorat") gouvernorat: String? = null): List<GardeDto>
    @POST("admin/gardes")
    suspend fun createGarde(@Body body: GardeRequest): GardeDto
}
