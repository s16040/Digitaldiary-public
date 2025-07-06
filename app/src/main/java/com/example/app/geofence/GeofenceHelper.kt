package com.example.app.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.app.model.Entry
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class GeofenceHelper(private val context: Context) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    suspend fun registerGeofences(entries: List<Entry>) {
        try {
            // Usuń istniejące geofence'y
            client.removeGeofences(pendingIntent).await()

            val geofences = entries
                .filter { it.lat != null && it.lon != null }
                .map { entry ->
                    Geofence.Builder()
                        .setRequestId(entry.id)
                        .setCircularRegion(
                            entry.lat!!,
                            entry.lon!!,
                            100f // promień 100 metrów
                        )
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or
                                    Geofence.GEOFENCE_TRANSITION_EXIT
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build()
                }

            if (geofences.isNotEmpty()) {
                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences)
                    .build()

                // Sprawdź uprawnienia przed dodaniem
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    client.addGeofences(request, pendingIntent).await()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Dla Android 10+ sprawdź też background location
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Brak uprawnień do lokalizacji w tle
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}