package com.example.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationUtil {
    data class LocationData(
        val lat: Double,
        val lon: Double,
        val place: String? = null
    )

    suspend fun getCurrentLocation(context: Context): LocationData? {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val locationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            location?.let {
                LocationData(
                    lat = it.latitude,
                    lon = it.longitude,
                    place = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
