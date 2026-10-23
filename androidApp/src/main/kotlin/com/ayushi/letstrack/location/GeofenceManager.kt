package com.ayushi.letstrack.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices

class GeofenceManager(
    private val context: Context
) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {

        val intent = Intent(
            context,
            GeofenceBroadcastReceiver::class.java
        )

        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGeofence(
        latitude: Double,
        longitude: Double,
        radius: Float
    ) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId("tracking_geofence")
            .setCircularRegion(
                latitude,
                longitude,
                radius
            )
            .setExpirationDuration(
                Geofence.NEVER_EXPIRE
            )
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        geofencingClient
            .addGeofences(
                com.google.android.gms.location.GeofencingRequest.Builder()
                    .setInitialTrigger(
                        com.google.android.gms.location.GeofencingRequest.INITIAL_TRIGGER_ENTER
                    )
                    .addGeofence(geofence)
                    .build(),
                pendingIntent
            )
            .addOnSuccessListener {
                android.util.Log.d(
                    "Geofence",
                    "GEOFENCE REGISTERED SUCCESSFULLY"
                )
            }
            .addOnFailureListener { exception ->
                android.util.Log.e(
                    "Geofence",
                    "GEOFENCE REGISTRATION FAILED",
                    exception
                )
            }
    }
}