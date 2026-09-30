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

interface UpdateCheckSetting {
    val autoCheckUpdates: Flow<Boolean>
    suspend fun setAutoCheckUpdates(enabled: Boolean)
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) : RotationLockSetting, UpdateCheckSetting {
    override val rotationLocked: Flow<Boolean> = dataStore.data.map { it[ROTATION_LOCKED] ?: false }
    override val autoCheckUpdates: Flow<Boolean> = dataStore.data.map { it[AUTO_CHECK_UPDATES] ?: false }

    override suspend fun setRotationLocked(locked: Boolean) {
        dataStore.edit { it[ROTATION_LOCKED] = locked }
    }

    override suspend fun setAutoCheckUpdates(enabled: Boolean) {
        dataStore.edit { it[AUTO_CHECK_UPDATES] = enabled }
    }

    private companion object {
        val ROTATION_LOCKED = booleanPreferencesKey("rotation_locked")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }
}
