package com.pasiflonet.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class AppPrefs(private val ctx: Context) {

    private object Keys {
        val API_ID = intPreferencesKey("tg_api_id")
        val API_HASH = stringPreferencesKey("tg_api_hash")
        val PHONE = stringPreferencesKey("tg_phone")
    }

    val apiId: Flow<Int?> = ctx.dataStore.data.map { it[Keys.API_ID] }
    val apiHash: Flow<String?> = ctx.dataStore.data.map { it[Keys.API_HASH] }
    val phone: Flow<String?> = ctx.dataStore.data.map { it[Keys.PHONE] }

    suspend fun setApi(id: Int, hash: String) {
        ctx.dataStore.edit { p ->
            p[Keys.API_ID] = id
            p[Keys.API_HASH] = hash
        }
    }

    suspend fun setPhone(phone: String) {
        ctx.dataStore.edit { p ->
            p[Keys.PHONE] = phone
        }
    }
}
