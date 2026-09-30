package com.johngabie.johnpdf

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.johngabie.johnpdf.data.ImportRepository
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.PdfLibraryRepository
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.data.SettingsRepository
import com.johngabie.johnpdf.data.StorageAccess
import com.johngabie.johnpdf.data.UpdateRepository
import java.io.File

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val app = context.applicationContext
    val recents = RecentsRepository(File(app.filesDir, "recents.json"))
    val importer = ImportRepository(app.contentResolver, File(app.filesDir, "imports"))
    val library = PdfLibraryRepository(app.contentResolver)
    val settings = SettingsRepository(app.settingsDataStore)
    val updates = UpdateRepository(app.settingsDataStore, BuildConfig.VERSION_NAME)
    val openPdf = OpenPdfUseCase(importer, recents)
    fun hasFilesAccess(): Boolean = StorageAccess.hasAllFilesAccess(app)
}
