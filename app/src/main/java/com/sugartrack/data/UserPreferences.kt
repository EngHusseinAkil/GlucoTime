package com.sugartrack.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.userDataStore by preferencesDataStore("user_preferences")

class UserPreferences(private val context: Context) {
    private val nameKey = stringPreferencesKey("user_name")
    private val completeKey = booleanPreferencesKey("onboarding_completed")
    private val tutorialKey = booleanPreferencesKey("tutorial_completed")
    private val languageKey = stringPreferencesKey("language")
    private val appearanceKey = stringPreferencesKey("appearance")
    private val currentProfileIdKey = longPreferencesKey("current_profile_id")

    suspend fun readName() = context.userDataStore.data.first()[nameKey].orEmpty()
    suspend fun completed() = context.userDataStore.data.first()[completeKey] ?: false
    suspend fun tutorialCompleted() = context.userDataStore.data.first()[tutorialKey] ?: false
    
    suspend fun readLanguage() = context.userDataStore.data.first()[languageKey] ?: "en"
    suspend fun saveLanguage(value: String) { context.userDataStore.edit { it[languageKey] = value } }
    
    suspend fun readAppearance() = context.userDataStore.data.first()[appearanceKey] ?: "system"
    suspend fun saveAppearance(value: String) { context.userDataStore.edit { it[appearanceKey] = value } }
    
    suspend fun complete(value: String) {
        context.userDataStore.edit {
            it[nameKey] = value.trim()
            it[completeKey] = true
        }
    }
    
    suspend fun setTutorialCompleted(completed: Boolean = true) {
        context.userDataStore.edit { it[tutorialKey] = completed }
    }
    
    suspend fun readCurrentProfileId() = context.userDataStore.data.first()[currentProfileIdKey]
    suspend fun saveCurrentProfileId(value: Long) { context.userDataStore.edit { it[currentProfileIdKey] = value } }
}
