package tn.pharmaciesdegarde.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.viewmodel.UserLocation

// Distance à vol d'oiseau entre deux points GPS, en kilomètres
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * 6371.0 * asin(sqrt(a))
}

fun GardeDto.distanceKm(from: UserLocation): Double =
    haversineKm(from.lat, from.lng, pharmacie.latitude, pharmacie.longitude)

// « 850 m » en dessous d'un kilomètre, « 1,2 km » au-dessus
fun formatDistance(km: Double): String =
    if (km < 1.0) "${((km * 100).roundToInt() * 10)} m"
    else String.format(Locale.FRENCH, "%.1f km", km)

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resume(null) }
    addOnCanceledListener { cont.resume(null) }
}

// À appeler seulement après avoir vérifié la permission. Renvoie null si la position est introuvable.
@SuppressLint("MissingPermission")
suspend fun fetchLocation(context: Context): Location? = withTimeoutOrNull(10_000) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token
    ).awaitOrNull() ?: client.lastLocation.awaitOrNull()
}