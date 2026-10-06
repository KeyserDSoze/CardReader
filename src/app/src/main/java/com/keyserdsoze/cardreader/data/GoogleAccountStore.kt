package com.keyserdsoze.cardreader.data

import android.content.Context

data class GoogleAccountIdentity(
    val uniqueId: String,
    val email: String,
    val displayName: String?,
)

class GoogleAccountStore(context: Context) {
    private val preferences = context.getSharedPreferences("cardreader_google_account", Context.MODE_PRIVATE)

    fun read(): GoogleAccountIdentity? {
        val email = preferences.getString("email", null)?.takeIf(String::isNotBlank) ?: return null
        return GoogleAccountIdentity(
            uniqueId = preferences.getString("unique_id", "").orEmpty(),
            email = email,
            displayName = preferences.getString("display_name", null),
        )
    }

    fun write(identity: GoogleAccountIdentity) {
        preferences.edit()
            .putString("unique_id", identity.uniqueId)
            .putString("email", identity.email)
            .putString("display_name", identity.displayName)
            .commit()
    }

    fun clear() {
        preferences.edit().clear().commit()
    }
}
