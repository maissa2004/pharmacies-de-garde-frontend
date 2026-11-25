package tn.pharmaciesdegarde.data.remote

import kotlinx.serialization.Serializable

enum class TypeGarde(val label: String) {
    JOUR("Garde de jour"),
    NUIT("Garde de nuit"),
    VINGT_QUATRE_H("Garde 24h"),
    WEEK_END("Garde de week-end"),
    JOUR_FERIE("Jour férié");

    companion object {
        fun from(name: String) = entries.firstOrNull { it.name == name } ?: JOUR
    }
}

@Serializable
data class GardeDto(
    val id: Long,
    val pharmacie: PharmacieDto,
    val dateDebut: String,   // "2026-11-25T20:00:00"
    val dateFin: String,
    val type: String
)

@Serializable
data class GardeRequest(
    val pharmacieId: Long,
    val gouvernorat: String,
    val dateDebut: String,
    val dateFin: String,
    val type: String
)