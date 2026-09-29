package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Invariantes de tipografia e alvo de toque da spec D1 (docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md §8). */
@RunWith(AndroidJUnit4::class)
class ThemeTest {
    private val styles = mapOf(
        "headlineMedium" to JohnTypography.headlineMedium,
        "titleLarge" to JohnTypography.titleLarge,
        "titleMedium" to JohnTypography.titleMedium,
        "titleSmall" to JohnTypography.titleSmall,
        "bodyLarge" to JohnTypography.bodyLarge,
        "bodyMedium" to JohnTypography.bodyMedium,
        "bodySmall" to JohnTypography.bodySmall,
        "labelLarge" to JohnTypography.labelLarge,
        "labelMedium" to JohnTypography.labelMedium,
        "labelSmall" to JohnTypography.labelSmall,
    )

    @Test fun nenhum_estilo_abaixo_de_16sp() {
        styles.forEach { (name, style) ->
            assertTrue("$name tinha ${style.fontSize.value}sp, mínimo é 16sp", style.fontSize.value >= 16f)
        }
    }

    @Test fun hierarquia_e_decrescente() {
        val titleLarge = JohnTypography.titleLarge.fontSize.value
        val titleMedium = JohnTypography.titleMedium.fontSize.value
        val bodyLarge = JohnTypography.bodyLarge.fontSize.value
        val bodyMedium = JohnTypography.bodyMedium.fontSize.value
        val labelLarge = JohnTypography.labelLarge.fontSize.value
        assertTrue("titleLarge ($titleLarge) deve ser > titleMedium ($titleMedium)", titleLarge > titleMedium)
        assertTrue("titleMedium ($titleMedium) deve ser >= bodyLarge ($bodyLarge)", titleMedium >= bodyLarge)
        assertTrue("bodyLarge ($bodyLarge) deve ser > bodyMedium ($bodyMedium)", bodyLarge > bodyMedium)
        assertTrue("bodyMedium ($bodyMedium) deve ser >= labelLarge ($labelLarge)", bodyMedium >= labelLarge)
    }

    @Test fun entrelinha_maior_ou_igual_ao_tamanho() {
        styles.forEach { (name, style) ->
            assertTrue(
                "$name tem lineHeight ${style.lineHeight.value}sp menor que fontSize ${style.fontSize.value}sp",
                style.lineHeight.value >= style.fontSize.value,
            )
        }
    }

    @Test fun alvo_primario_acima_do_minimo_do_m3() {
        assertTrue("PrimaryTouchTarget deve ser >= 48dp", PrimaryTouchTarget >= 48.dp)
        assertTrue("ListItemMinHeight deve ser >= 72dp", ListItemMinHeight >= 72.dp)
    }
}
