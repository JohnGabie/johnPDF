package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Luminância relativa, WCAG 2.1: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance */
private fun luminance(c: Color): Double {
    fun ch(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
}

/** Razão de contraste WCAG: 1:1 (cores iguais) a 21:1 (preto sobre branco). */
internal fun contrast(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** Os cinco níveis de container que o app usa como fundo. */
internal fun ColorScheme.containers(): List<Pair<String, Color>> = listOf(
    "surfaceContainerLowest" to surfaceContainerLowest,
    "surfaceContainerLow" to surfaceContainerLow,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "surfaceContainerHighest" to surfaceContainerHighest,
)

/** Todo par texto/fundo que o app realmente desenha. */
internal fun ColorScheme.textPairs(): List<Triple<String, Color, Color>> = listOf(
    Triple("onPrimary/primary", onPrimary, primary),
    Triple("onSecondary/secondary", onSecondary, secondary),
    Triple("onTertiary/tertiary", onTertiary, tertiary),
    Triple("onError/error", onError, error),
    Triple("onBackground/background", onBackground, background),
    Triple("onSurface/surface", onSurface, surface),
    Triple("onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant),
    Triple("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
    Triple("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
    Triple("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
    Triple("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
    Triple("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
    Triple("inversePrimary/inverseSurface", inversePrimary, inverseSurface),
) + containers().flatMap { (bgName, bg) ->
    // Tudo que o app escreve sobre um container: texto principal, secundário,
    // rótulo de TextButton (primary) e mensagem de erro.
    listOf(
        "onSurface" to onSurface,
        "onSurfaceVariant" to onSurfaceVariant,
        "primary" to primary,
        "error" to error,
    ).map { (fgName, fg) -> Triple("$fgName/$bgName", fg, bg) }
}

class ContrastTest {

    private fun assertAa(schemeName: String, scheme: ColorScheme) {
        scheme.textPairs().forEach { (name, fg, bg) ->
            val r = contrast(fg, bg)
            assertTrue("[$schemeName] $name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    private fun assertAaa(schemeName: String, scheme: ColorScheme) {
        val backgrounds = listOf("surface" to scheme.surface, "background" to scheme.background) +
            scheme.containers()
        backgrounds.forEach { (bgName, bg) ->
            val r = contrast(scheme.onSurface, bg)
            assertTrue("[$schemeName] AAA onSurface/$bgName = %.2f:1, mínimo 7:1".format(r), r >= 7.0)
        }
        val variant = contrast(scheme.onSurfaceVariant, scheme.surface)
        assertTrue(
            "[$schemeName] AAA onSurfaceVariant/surface = %.2f:1, mínimo 7:1".format(variant),
            variant >= 7.0,
        )
    }

    private fun assertOutlines(schemeName: String, scheme: ColorScheme) {
        val backgrounds = listOf("surface" to scheme.surface) + scheme.containers()
        backgrounds.forEach { (bgName, bg) ->
            listOf("outline" to scheme.outline, "outlineVariant" to scheme.outlineVariant)
                .forEach { (fgName, fg) ->
                    val r = contrast(fg, bg)
                    assertTrue(
                        "[$schemeName] $fgName/$bgName = %.2f:1, mínimo 3:1".format(r),
                        r >= 3.0,
                    )
                }
        }
    }

    /** Regra do projeto, mais rígida que a WCAG (que isenta desabilitado). */
    private fun assertDisabled(schemeName: String, scheme: ColorScheme) {
        val container = scheme.onSurface.copy(alpha = 0.12f).compositeOver(scheme.surfaceContainer)
        val label = scheme.onSurface.copy(alpha = 0.60f).compositeOver(container)
        val r = contrast(label, container)
        assertTrue("[$schemeName] rótulo desabilitado = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
    }

    @Test fun light_text_meets_wcag_aa() = assertAa("claro", LightSchemeForTest)

    @Test fun light_main_text_meets_wcag_aaa() = assertAaa("claro", LightSchemeForTest)

    /** Review Focus 3: divisor da lista e borda do campo em todos os fundos. */
    @Test fun light_outlines_meet_non_text_minimum() = assertOutlines("claro", LightSchemeForTest)

    @Test fun light_disabled_label_stays_readable() = assertDisabled("claro", LightSchemeForTest)

    /** Review Focus 1: o pior par do claro é primary sobre surfaceContainerHighest. */
    @Test fun light_primary_is_readable_on_every_container() {
        LightSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(LightSchemeForTest.primary, bg)
            assertTrue("[claro] primary/$name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    /** Review Focus 2: ícone sem rótulo é informação não textual (WCAG 1.4.11). */
    @Test fun light_lone_icon_is_visible_on_every_container() {
        LightSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(LightSchemeForTest.onSurfaceVariant, bg)
            assertTrue("[claro] ícone onSurfaceVariant/$name = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }

    /** O seed da marca não pode mudar por acidente. */
    @Test fun light_primary_is_blue_lagoon() {
        assertTrue(
            "primary do claro deve ser #006494",
            LightSchemeForTest.primary == Color(0xFF006494),
        )
    }

    @Test fun dark_text_meets_wcag_aa() = assertAa("escuro", DarkSchemeForTest)

    @Test fun dark_main_text_meets_wcag_aaa() = assertAaa("escuro", DarkSchemeForTest)

    @Test fun dark_outlines_meet_non_text_minimum() = assertOutlines("escuro", DarkSchemeForTest)

    @Test fun dark_disabled_label_stays_readable() = assertDisabled("escuro", DarkSchemeForTest)

    @Test fun dark_primary_is_readable_on_every_container() {
        DarkSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(DarkSchemeForTest.primary, bg)
            assertTrue("[escuro] primary/$name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    @Test fun dark_lone_icon_is_visible_on_every_container() {
        DarkSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(DarkSchemeForTest.onSurfaceVariant, bg)
            assertTrue("[escuro] ícone onSurfaceVariant/$name = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }

    @Test fun dark_primary_is_blue_lagoon() {
        assertTrue(
            "primary do escuro deve ser #96C4E5",
            DarkSchemeForTest.primary == Color(0xFF96C4E5),
        )
    }

    /**
     * Review Focus 5: erro e confirmação. As duas mensagens precisam ser legíveis
     * sobre a superfície em que aparecem — e, como error e primary têm quase a
     * mesma luminância (1,00:1 no claro), o erro nunca é sinalizado só por cor.
     */
    @Test fun error_and_confirmation_are_readable_in_both_schemes() {
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            val erro = contrast(s.error, s.surfaceContainerHigh)
            assertTrue("[$n] error/surfaceContainerHigh = %.2f:1".format(erro), erro >= 4.5)
            val snack = contrast(s.inverseOnSurface, s.inverseSurface)
            assertTrue("[$n] snackbar = %.2f:1".format(snack), snack >= 4.5)
            val acao = contrast(s.inversePrimary, s.inverseSurface)
            assertTrue("[$n] ação do snackbar = %.2f:1".format(acao), acao >= 4.5)
        }
    }

    /** Nenhum papel pode ficar na cor de "não preenchido" (lilás do baseline M3). */
    @Test fun no_role_keeps_the_material_baseline_purple() {
        val lilases = listOf(
            Color(0xFFE8DEF8), Color(0xFFEADDFF), Color(0xFF6750A4), Color(0xFFD0BCFF),
            Color(0xFF4A4458), Color(0xFF21005D), Color(0xFF1D192B), Color(0xFF49454F),
        )
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            val usados = s.textPairs().flatMap { listOf(it.second, it.third) } +
                listOf(s.outline, s.outlineVariant, s.surfaceTint, s.scrim)
            lilases.forEach { roxo ->
                assertTrue(
                    "[$n] papel ficou no baseline roxo do M3: $roxo",
                    usados.none { it == roxo },
                )
            }
        }
    }
}
