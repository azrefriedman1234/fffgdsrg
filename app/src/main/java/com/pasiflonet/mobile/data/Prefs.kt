package com.pasiflonet.mobile.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pasiflonet_prefs")

object PrefsKeys {
    val logoEnabled = booleanPreferencesKey("logoEnabled")
    val logoUri = stringPreferencesKey("logoUri")
    val logoPreset = stringPreferencesKey("logoPreset")
    val logoX = floatPreferencesKey("logoX")
    val logoY = floatPreferencesKey("logoY")
    val logoScale = floatPreferencesKey("logoScale")
    val logoOpacity = floatPreferencesKey("logoOpacity")

    val apiId = intPreferencesKey("apiId")
    val apiHash = stringPreferencesKey("apiHash")
}

class Prefs(private val context: Context) {

    fun flow(): Flow<Preferences> = context.dataStore.data

    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setFloat(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { it[key] = value }
    }

    fun logoUriFlow(): Flow<Uri?> = flow().map { prefs ->
        prefs[PrefsKeys.logoUri]?.let { Uri.parse(it) }
    }
}
