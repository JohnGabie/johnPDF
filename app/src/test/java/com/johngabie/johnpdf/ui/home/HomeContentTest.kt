package com.johngabie.johnpdf.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.Origin
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule val rule = createComposeRule()

    private val now = 1_790_000_000_000L
    private val recent = RecentItem("Fatura.pdf", Origin.WHATSAPP, "/x/Fatura.pdf", now, imported = true)
    private val events = mutableListOf<String>()

    private fun show(state: HomeUiState) = rule.setContent {
        JohnPdfTheme {
            HomeContent(
                state = state,
                onOpenPicker = { events += "picker" },
                onSelectTab = { events += "tab:$it" },
                onQueryChange = { events += "query:$it" },
                onOpenRecent = { events += "recent:${it.name}" },
                onRemoveRecent = { events += "remove:${it.name}" },
                onOpenPdf = { events += "pdf:${it.name}" },
                onRequestPermission = { events += "permission" },
                onDismissError = { events += "dismiss" },
                nowMillis = now,
            )
        }
    }

    @Test fun header_and_bottom_menu_are_visible() {
        show(HomeUiState())
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        rule.onNodeWithText("📂 Abrir").performClick()
        rule.onNodeWithText("Todos os PDFs").performClick()
        assertEquals(listOf("picker", "tab:ALL"), events)
    }

    @Test fun empty_recents_shows_hint() {
        show(HomeUiState())
        rule.onNodeWithText("Os PDFs que você abrir vão aparecer aqui.").assertIsDisplayed()
    }

    @Test fun recent_card_shows_origin_and_date_and_opens() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("WhatsApp · hoje").assertIsDisplayed()
        rule.onNodeWithText("Fatura.pdf").performClick()
        assertEquals(listOf("recent:Fatura.pdf"), events)
    }

    @Test fun long_press_asks_before_removing() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("Fatura.pdf").performTouchInput { longClick() }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("Sim").performClick()
        assertEquals(listOf("remove:Fatura.pdf"), events)
    }

    @Test fun all_tab_without_access_asks_permission() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("Para mostrar os PDFs do celular, o johnPDF precisa de permissão.").assertIsDisplayed()
        rule.onNodeWithText("Permitir").performClick()
        assertEquals(listOf("permission"), events)
    }

    @Test fun all_tab_lists_filtered_pdfs() {
        val pdfs = listOf(PdfFile("Boleto.pdf", "/d/Boleto.pdf", Origin.DOWNLOAD, now), PdfFile("Receita.pdf", "/d/R.pdf", Origin.DOCUMENTS, now))
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, allPdfs = pdfs, query = "bol"))
        rule.onNodeWithText("Boleto.pdf").performClick()
        rule.onNodeWithText("Receita.pdf").assertDoesNotExist()
        assertEquals(listOf("pdf:Boleto.pdf"), events)
    }

    @Test fun all_tab_empty_with_query_says_no_match() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "zzz"))
        rule.onNodeWithText("Nenhum PDF com esse nome.").assertIsDisplayed()
    }

    @Test fun error_dialog_is_shown() {
        show(HomeUiState(error = AppError.NO_SPACE))
        rule.onNodeWithText("Sem espaço no celular para abrir este arquivo.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
