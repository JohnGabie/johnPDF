package com.johngabie.johnpdf.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.Origin
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun header_abre_o_seletor_de_arquivos() {
        show(HomeUiState())
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        rule.onNodeWithTag("open_pdf_header").assertIsDisplayed().performClick()
        assertEquals(listOf("picker"), events)
    }

    /** Review Focus 3: a decisão 3 do usuário revogou o FAB; nada com esse rótulo pode flutuar embaixo. */
    @Test fun home_nao_tem_botao_flutuante() {
        show(HomeUiState())
        val root = rule.onRoot().getBoundsInRoot()
        val header = rule.onNodeWithTag("open_pdf_header").getBoundsInRoot()
        assertTrue("o botão saiu do topo da tela: $header", header.bottom < root.height / 4f)

        val total = rule.onAllNodesWithText("Abrir PDF").fetchSemanticsNodes().size
        repeat(total) { i ->
            val b = rule.onAllNodesWithText("Abrir PDF")[i].getBoundsInRoot()
            val noCantoInferiorDireito = b.top > root.height * 0.75f && b.right > root.width * 0.5f
            assertFalse("há um 'Abrir PDF' flutuando no canto inferior direito: $b", noCantoInferiorDireito)
        }
    }

    @Test fun barra_inferior_troca_de_aba_com_rotulos_sempre_visiveis() {
        show(HomeUiState())
        rule.onNodeWithText("Recentes").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").performClick()
        assertEquals(listOf("tab:ALL"), events)
    }

    /** Review Focus 4: trocar emoji por Icon não pode deixar a barra torta. */
    @Test fun icones_da_navigation_bar_ficam_alinhados() {
        show(HomeUiState())
        val recentes = rule.onNodeWithTag("nav_icon_recents", useUnmergedTree = true).getBoundsInRoot()
        val todos = rule.onNodeWithTag("nav_icon_all", useUnmergedTree = true).getBoundsInRoot()
        assertEquals("topos diferentes: $recentes vs $todos", recentes.top.value, todos.top.value, 0.5f)
        assertEquals("alturas diferentes: $recentes vs $todos", recentes.height.value, todos.height.value, 0.5f)
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

    @Test fun aba_todos_sem_permissao_pede_acesso() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("Para mostrar os PDFs do celular, o johnPDF precisa de permissão.").assertIsDisplayed()
        rule.onNodeWithText("1. Toque em Permitir acesso\n2. Ative a opção do johnPDF\n3. Volte para o app").assertIsDisplayed()
        rule.onNodeWithText("Permitir acesso").performClick()
        assertEquals(listOf("permission"), events)
    }

    /** Spec D1 §3.2, invariante 1: nunca dois botões Filled na mesma superfície. */
    @Test fun permission_screen_has_exactly_one_primary_button() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
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
