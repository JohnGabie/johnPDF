package com.johngabie.johnpdf

import android.app.Application
import android.app.AppOpsManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment

private const val OP_MANAGE_EXTERNAL_STORAGE = 92

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    init {
        // Sem isso, Environment.isExternalStorageManager() (chamado pelo StorageAccess real via
        // AppContainer/MainActivity) lança ArrayIndexOutOfBoundsException no Robolectric, que não
        // registra nenhum diretório externo por padrão. Precisa rodar antes do rule.before()
        // (que já dispara o onCreate da Activity), daí o init em vez de @Before.
        ShadowEnvironment.addExternalDir("primary")
        // No Robolectric, o app-op de "acesso a todos os arquivos" vem MODE_ALLOWED por padrão
        // (diferente de um aparelho real). Força MODE_ERRORED para o teste exercitar o mesmo
        // caminho (tela de permissão) que um usuário novo veria de verdade.
        val app = ApplicationProvider.getApplicationContext<Application>()
        val appOps = app.getSystemService(AppOpsManager::class.java)
        shadowOf(appOps).setMode(OP_MANAGE_EXTERNAL_STORAGE, app.applicationInfo.uid, app.packageName, AppOpsManager.MODE_ERRORED)
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shows_app_name() {
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
    }

    @Test
    fun home_starts_on_recents_and_switches_to_all() {
        rule.onNodeWithText("Os PDFs que você abrir vão aparecer aqui.").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").performClick()
        rule.onNodeWithText("Permitir acesso").assertIsDisplayed()
    }

    /**
     * Arranque a frio em modo escuro: a Activity sobe e o windowBackground do tema
     * de plataforma é a surface escura — é ele que evita o clarão branco antes de o
     * Compose desenhar.
     */
    @Test
    @Config(qualifiers = "+night")
    fun activity_starts_in_night_mode() {
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        assertEquals(0xFF0F1417.toInt(), rule.activity.getColor(R.color.window_background))
    }
}
