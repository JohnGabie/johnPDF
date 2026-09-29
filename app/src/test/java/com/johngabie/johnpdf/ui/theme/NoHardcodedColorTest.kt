package com.johngabie.johnpdf.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cor fica num lugar só. O lilás vazado do baseline M3 voltou toda vez que um
 * componente escolheu a própria cor — ou deixou o M3 escolher por `tonalElevation`.
 */
class NoHardcodedColorTest {

    private val fontes: List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    /**
     * `Theme.kt` é a única fonte de cor. `MuPdfEngine.kt` usa `android.graphics.Color`
     * no `eraseColor` do bitmap, que não é o `Color` do Compose. `JohnIcons.kt` é a
     * tabela vetorial dos Material Symbols: os paths são monocromáticos e recebem
     * `tint` de quem desenha.
     */
    private val isentos = setOf("Theme.kt", "MuPdfEngine.kt", "JohnIcons.kt")

    /**
     * Varre só linhas de código: um comentário que *explica* por que não se usa
     * `tonalElevation` não é uma violação, e sem este filtro a própria documentação
     * da regra derrubaria o teste.
     */
    private fun ofensores(arquivos: List<File>, padrao: Regex): List<String> =
        arquivos.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, linha ->
                val codigo = linha.substringBefore("//").trim()
                if (codigo.startsWith("*")) null
                else if (padrao.containsMatchIn(codigo)) "${f.name}:${i + 1}: ${linha.trim()}"
                else null
            }
        }

    @Test fun sources_exist() {
        assertTrue(
            "nenhum .kt encontrado — diretório de trabalho inesperado: ${File(".").absolutePath}",
            fontes.size > 5,
        )
    }

    @Test fun no_literal_compose_color_outside_theme() {
        val literal = Regex("""Color\(0x|Color\.(White|Black|Red|Blue|Gray|LightGray|DarkGray|Green|Yellow|Cyan|Magenta)""")
        val achados = ofensores(fontes.filter { it.name !in isentos }, literal)
        assertTrue(
            "cor literal fora de Theme.kt — use um papel do tema:\n" + achados.joinToString("\n"),
            achados.isEmpty(),
        )
    }

    @Test fun no_tonal_elevation_anywhere() {
        val achados = ofensores(fontes, Regex("""tonalElevation\s*="""))
        assertTrue(
            "tonalElevation tinge a superfície com primary e reintroduz cor vazada;\n" +
                "use surfaceContainer* explícito:\n" + achados.joinToString("\n"),
            achados.isEmpty(),
        )
    }

    /** A regra só vale se o varredor realmente pegaria uma violação. */
    @Test fun the_guard_actually_catches_a_violation() {
        val falso = File.createTempFile("Fake", ".kt").apply {
            writeText("val x = Color(0xFF00FF00)\nSurface(tonalElevation = 3.dp)\n")
            deleteOnExit()
        }
        assertTrue(
            "o varredor não pegou uma cor literal plantada",
            ofensores(listOf(falso), Regex("""Color\(0x""")).isNotEmpty(),
        )
        assertTrue(
            "o varredor não pegou um tonalElevation plantado",
            ofensores(listOf(falso), Regex("""tonalElevation\s*=""")).isNotEmpty(),
        )
    }
}
