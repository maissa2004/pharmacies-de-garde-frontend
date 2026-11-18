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