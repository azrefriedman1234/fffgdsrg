package com.pasiflonet.mobile.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class SavedAuth(
    val apiId: Int,
    val apiHash: String,
    val phone: String
)

class SettingsStore(private val ctx: Context) {
    private val K_API_ID = intPreferencesKey("api_id")
    private val K_API_HASH = stringPreferencesKey("api_hash")
    private val K_PHONE = stringPreferencesKey("phone")

    val authFlow: Flow<SavedAuth> = ctx.dataStore.data.map { p ->
        SavedAuth(
            apiId = p[K_API_ID] ?: 0,
            apiHash = p[K_API_HASH] ?: "",
            phone = p[K_PHONE] ?: ""
        )
    }

    suspend fun saveApi(apiId: Int, apiHash: String) {
        ctx.dataStore.edit { p ->
            p[K_API_ID] = apiId
            p[K_API_HASH] = apiHash
        }
    }

    suspend fun savePhone(phone: String) {
        ctx.dataStore.edit { p -> p[K_PHONE] = phone }
    }
}
