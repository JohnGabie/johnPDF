package com.johngabie.johnpdf.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun rotation_lock_defaults_false_and_persists() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "s.preferences_pb") }
        val repo = SettingsRepository(store)
        assertFalse(repo.rotationLocked.first())
        repo.setRotationLocked(true)
        assertTrue(repo.rotationLocked.first())
    }
}
