package com.ayushi.letstrack.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TrackingEventRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun listenForGroupExits(
        groupId: String,
        currentUserId: String,
        onOtherMemberExit: () -> Unit
    ): ListenerRegistration {

        return firestore
            .collection("tracking_events")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("type", "GEOFENCE_EXIT")
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {

                    if (change.type !=
                        com.google.firebase.firestore.DocumentChange.Type.ADDED
                    ) {
                        continue
                    }

                    val userId =
                        change.document.getString("userId")

                    if (
                        userId != null &&
                        userId != currentUserId
                    ) {
                        onOtherMemberExit()
                    }
                }
            }
    }
}