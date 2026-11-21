package tn.pharmaciesdegarde.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PharmacieDto(
    val id: Long,
    val nom: String,
    val adresse: String,
    val ville: String,
    val gouvernorat: String,
    val latitude: Double,
    val longitude: Double,
    val telephone: String,
    val photo: String? = null
)
@Serializable
data class PharmacieRequest(
    val nom: String,
    val adresse: String,
    val telephone: String,
    val ville: String,
    val gouvernorat: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class PharmacieUpdateRequest(val nom: String, val adresse: String, val telephone: String)

@Serializable
data class GeocodeRequest(val adresse: String, val ville: String, val gouvernorat: String)

@Serializable
data class GeocodeResponse(val latitude: Double, val longitude: Double, val precise: Boolean)
@Serializable
data class ReverseGeocodeResponse(
    val adresse: String = "",
    val ville: String = "",
    val gouvernorat: String = ""
)