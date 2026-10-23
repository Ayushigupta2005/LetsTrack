package com.ayushi.letstrack.data

import android.content.Context

class TrackingPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        "tracking_preferences",
        Context.MODE_PRIVATE
    )

    fun saveGroupId(groupId: String) {
        preferences.edit()
            .putString("group_id", groupId)
            .apply()
    }

    fun getGroupId(): String? {
        return preferences.getString("group_id", null)
    }
}