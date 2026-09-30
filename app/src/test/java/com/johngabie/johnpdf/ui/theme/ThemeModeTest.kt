package com.johngabie.johnpdf.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeModeTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // ------------------------------------------------------------------- resolução

    @Test fun seguir_o_sistema_repassa_o_que_o_sistema_disser() {
        assertEquals(true, ThemeMode.SYSTEM.resolveDark(systemDark = true))
        assertEquals(false, ThemeMode.SYSTEM.resolveDark(systemDark = false))
    }

    /** O ponto da troca manual: a escolha do usuário ganha do sistema, nos dois sentidos. */
    @Test fun escolha_manual_ignora_o_sistema() {
        assertEquals(false, ThemeMode.LIGHT.resolveDark(systemDark = true))
        assertEquals(true, ThemeMode.DARK.resolveDark(systemDark = false))
    }

    // ------------------------------------------------------------------------ ciclo

    /**
     * Um botão só, três estados: a ordem tem de ser estável e fechar o ciclo, senão o
     * usuário não consegue voltar ao "seguir o sistema" sem ir em configurações.
     */
    @Test fun o_ciclo_passa_pelos_tres_e_volta_ao_comeco() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }

    @Test fun tres_toques_voltam_ao_modo_inicial() {
        ThemeMode.entries.forEach { inicial ->
            assertEquals(inicial, inicial.next().next().next())
        }
    }

    // ------------------------------------------------------------------ persistência

    @Test fun sem_nada_salvo_o_padrao_e_seguir_o_sistema() {
        assertEquals(ThemeMode.SYSTEM, ThemePreferences(context).read())
    }

    @Test fun o_modo_escolhido_sobrevive_a_releitura() {
        val prefs = ThemePreferences(context)
        ThemeMode.entries.forEach { mode ->
            prefs.write(mode)
            assertEquals("$mode não voltou igual", mode, ThemePreferences(context).read())
        }
    }

    /**
     * Valor estragado (downgrade, edição manual do XML) não pode derrubar o app: o tema é
     * lido no primeiro frame, então uma exceção aqui seria crash na abertura.
     */
    @Test fun valor_desconhecido_cai_no_padrao_em_vez_de_quebrar() {
        context.getSharedPreferences(ThemePreferences.FILE, Context.MODE_PRIVATE)
            .edit().putString(ThemePreferences.KEY, "SEPIA").commit()
        assertEquals(ThemeMode.SYSTEM, ThemePreferences(context).read())
    }
}
