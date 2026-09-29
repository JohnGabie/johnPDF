package com.johngabie.johnpdf.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RotationLockSetting {
    val rotationLocked: Flow<Boolean>
    suspend fun setRotationLocked(locked: Boolean)
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) : RotationLockSetting {
    override val rotationLocked: Flow<Boolean> = dataStore.data.map { it[ROTATION_LOCKED] ?: false }

    override suspend fun setRotationLocked(locked: Boolean) {
        dataStore.edit { it[ROTATION_LOCKED] = locked }
    }

    private companion object {
        val ROTATION_LOCKED = booleanPreferencesKey("rotation_locked")
    }
}
