package com.buildaccent.`as`.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val IS_DELETE_ENABLED = booleanPreferencesKey("is_delete_enabled")
    private val IS_EDIT_ENABLED = booleanPreferencesKey("is_edit_enabled")
    private val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

    val isDeleteEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_DELETE_ENABLED] ?: false
        }

    val isEditEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_EDIT_ENABLED] ?: false
        }
        
    val preferredLanguage: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PREFERRED_LANGUAGE]
        }
        
    val hasSeenOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING] ?: false
        }

    suspend fun saveDeleteEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DELETE_ENABLED] = enabled
        }
    }

    suspend fun saveEditEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_EDIT_ENABLED] = enabled
        }
    }
    
    suspend fun savePreferredLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_LANGUAGE] = language
        }
    }
    
    suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = seen
        }
    }
}
