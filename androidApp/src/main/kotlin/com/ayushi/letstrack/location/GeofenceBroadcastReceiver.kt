package com.ayushi.letstrack.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ayushi.letstrack.data.FirebaseAuthManager
import com.ayushi.letstrack.data.TrackingPreferences
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.ayushi.letstrack.notification.NotificationHelper

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        Log.d("Geofence", "BroadcastReceiver triggered")

        val event = GeofencingEvent.fromIntent(intent)


        if (event == null || event.hasError()) {
            Log.e("Geofence", "Geofence event error")
            return
        }

        if (
            event.geofenceTransition ==
            Geofence.GEOFENCE_TRANSITION_EXIT
        ) {

            Log.d(
                "Geofence",
                "USER EXITED GEOFENCE"
            )

            NotificationHelper.showExitNotification(context)

            val userId = FirebaseAuthManager()
                .getCurrentUserId()

            val groupId = TrackingPreferences(context)
                .getGroupId()

            Log.d(
                "Geofence",
                "userId = $userId"
            )

            Log.d(
                "Geofence",
                "groupId = $groupId"
            )

            if (userId == null || groupId == null) {
                Log.e(
                    "Geofence",
                    "Missing userId or groupId"
                )
                return
            }

            val data = hashMapOf(
                "type" to "GEOFENCE_EXIT",
                "userId" to userId,
                "groupId" to groupId,
                "timestamp" to System.currentTimeMillis()
            )

            FirebaseFirestore
                .getInstance()
                .collection("tracking_events")
                .add(data)
                .addOnSuccessListener { document ->

                    Log.d(
                        "Geofence",
                        "EXIT EVENT SAVED"
                    )

                    Log.d(
                        "Geofence",
                        "Event ID = ${document.id}"
                    )
                }
                .addOnFailureListener { exception ->

                    Log.e(
                        "Geofence",
                        "FAILED TO SAVE EXIT EVENT",
                        exception
                    )
                }
        }
    }
}