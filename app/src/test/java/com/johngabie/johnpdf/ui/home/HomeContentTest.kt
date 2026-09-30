package com.johngabie.johnpdf.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.dp
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

        val total = rule.onAllNodesWithText("Open PDF").fetchSemanticsNodes().size
        repeat(total) { i ->
            val b = rule.onAllNodesWithText("Open PDF")[i].getBoundsInRoot()
            val noCantoInferiorDireito = b.top > root.height * 0.75f && b.right > root.width * 0.5f
            assertFalse("há um 'Abrir PDF' flutuando no canto inferior direito: $b", noCantoInferiorDireito)
        }
    }

    @Test fun barra_inferior_troca_de_aba_com_rotulos_sempre_visiveis() {
        show(HomeUiState())
        rule.onNodeWithText("Recents").assertIsDisplayed()
        rule.onNodeWithText("All PDFs").assertIsDisplayed()
        rule.onNodeWithText("All PDFs").performClick()
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

    @Test fun recentes_vazio_mostra_estado_com_acao() {
        show(HomeUiState())
        rule.onNodeWithText("No PDFs opened yet").assertIsDisplayed()
        rule.onNodeWithText("The PDFs you open will show up here.").assertIsDisplayed()
        rule.onNodeWithTag("empty_state_action").performClick()
        assertEquals(listOf("picker"), events)
    }

    @Test fun biblioteca_vazia_mostra_estado_com_acao() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true))
        rule.onNodeWithText("No PDFs on this phone").assertIsDisplayed()
        rule.onNodeWithText("No PDFs found on this phone.").assertIsDisplayed()
    }

    @Test fun linha_da_lista_tem_72dp_e_e_clicavel_inteira() {
        show(HomeUiState(recents = listOf(recent)))
        val root = rule.onRoot().getBoundsInRoot()
        val linha = rule.onNodeWithText("Fatura.pdf").assertIsDisplayed().getBoundsInRoot()
        assertTrue("linha com ${linha.height}, esperado >= 72dp", linha.height >= 72.dp)
        assertTrue("linha não ocupa a largura toda: ${linha.width} de ${root.width}", linha.width >= root.width - 1.dp)
    }

    @Test fun busca_vazia_nao_mostra_botao_de_limpar() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true))
        rule.onNodeWithText("Search PDFs").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test fun botao_de_limpar_zera_a_busca() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "bol"))
        rule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals(listOf("query:"), events)
    }

    @Test fun recent_card_shows_origin_and_date_and_opens() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("WhatsApp · today").assertIsDisplayed()
        rule.onNodeWithText("Fatura.pdf").performClick()
        assertEquals(listOf("recent:Fatura.pdf"), events)
    }

    @Test fun long_press_asks_before_removing() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("Fatura.pdf").performTouchInput { longClick() }
        rule.onNodeWithText("Remove from list?").assertIsDisplayed()
        rule.onNodeWithText("The file stays on your phone.").assertIsDisplayed()
        rule.onNodeWithText("Remove").performClick()
        assertEquals(listOf("remove:Fatura.pdf"), events)
    }

    @Test fun aba_todos_sem_permissao_pede_acesso() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("To show the PDFs on your phone, johnPDF needs permission.").assertIsDisplayed()
        rule.onNodeWithText("1. Tap Allow access\n2. Turn on johnPDF\n3. Come back to the app").assertIsDisplayed()
        rule.onNodeWithText("Allow access").performClick()
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

    @Test fun busca_sem_resultado_nao_oferece_acao() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "zzz"))
        rule.onNodeWithText("Nothing found").assertIsDisplayed()
        rule.onNodeWithText("No PDF with that name.").assertIsDisplayed()
        rule.onNodeWithTag("empty_state_action").assertDoesNotExist()
    }

    @Test fun error_dialog_is_shown() {
        show(HomeUiState(error = AppError.NO_SPACE))
        rule.onNodeWithText("Not enough space on your phone to open this file.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
