package com.ayushi.letstrack.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseGroupRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createGroup(
        groupName: String,
        userId: String
    ): String {

        val group = hashMapOf(
            "name" to groupName,
            "createdBy" to userId,
            "members" to listOf(userId)
        )

        val document = firestore
            .collection("groups")
            .add(group)
            .await()

        return document.id
    }

    suspend fun joinGroup(
        groupId: String,
        userId: String
    ) {
        firestore
            .collection("groups")
            .document(groupId)
            .update(
                "members",
                FieldValue.arrayUnion(userId)
            )
            .await()
    }
}