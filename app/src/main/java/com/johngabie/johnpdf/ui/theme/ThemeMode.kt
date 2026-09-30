package com.johngabie.johnpdf.ui.theme

import android.content.Context

/**
 * Como o app escolhe entre claro e escuro.
 *
 * Existe porque "seguir o sistema" não basta: quem lê PDF no ônibus de manhã e na cama à
 * noite quer trocar na hora, sem sair do app e mexer nas configurações do Android. E quem
 * deixa o celular no escuro o dia todo pode querer o johnPDF claro mesmo assim.
 */
enum class ThemeMode {
    /** O padrão: o que o sistema disser. */
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** `systemDark` é o `isSystemInDarkTheme()` do Compose — só o SYSTEM olha para ele. */
    fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    /**
     * O próximo modo do ciclo do botão.
     *
     * É um botão só, não um menu: são três opções e a tela é de leitura, não de ajustes.
     * O ciclo fecha (DARK volta a SYSTEM) para dar como voltar ao automático sem sair da tela.
     */
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/**
 * Guarda o modo escolhido em `SharedPreferences`.
 *
 * SharedPreferences e não o DataStore do resto do app de propósito: o tema é lido no
 * primeiro frame, antes de qualquer corrotina, e uma leitura assíncrona aqui piscaria o
 * tema errado a cada abertura.
 */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Valor ausente ou estragado vira SYSTEM: o tema não é motivo para crash na abertura. */
    fun read(): ThemeMode {
        val saved = prefs.getString(KEY, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun write(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
    }

    companion object {
        const val FILE = "theme"
        const val KEY = "theme_mode"
    }
}
