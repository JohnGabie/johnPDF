package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Restrição do plano: nenhum texto de UI abaixo de 20sp. */
@RunWith(AndroidJUnit4::class)
class ThemeTest {
    @Test fun every_typography_style_is_at_least_20sp() {
        val minSp = 20f
        val styles = mapOf(
            "headlineMedium" to BigTypography.headlineMedium,
            "titleLarge" to BigTypography.titleLarge,
            "titleMedium" to BigTypography.titleMedium,
            "titleSmall" to BigTypography.titleSmall,
            "bodyLarge" to BigTypography.bodyLarge,
            "bodyMedium" to BigTypography.bodyMedium,
            "bodySmall" to BigTypography.bodySmall,
            "labelLarge" to BigTypography.labelLarge,
            "labelMedium" to BigTypography.labelMedium,
            "labelSmall" to BigTypography.labelSmall,
        )
        styles.forEach { (name, style) ->
            assertTrue(
                "$name deve ter no mínimo ${minSp}sp, tinha ${style.fontSize.value}sp",
                style.fontSize.value >= minSp,
            )
        }
    }

    @Test fun alvo_primario_acima_do_minimo_do_m3() {
        assertTrue("PrimaryTouchTarget deve ser >= 48dp", PrimaryTouchTarget >= 48.dp)
        assertTrue("ListItemMinHeight deve ser >= 72dp", ListItemMinHeight >= 72.dp)
    }
}
