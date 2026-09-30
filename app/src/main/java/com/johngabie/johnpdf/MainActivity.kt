package com.johngabie.johnpdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.johngabie.johnpdf.ui.AppNavHost
import com.johngabie.johnpdf.ui.home.HomeViewModel
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import com.johngabie.johnpdf.ui.theme.rememberThemeController

class MainActivity : ComponentActivity() {
    private val container: AppContainer get() = (application as JohnPdfApp).container

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                HomeViewModel(container.recents, container.library, container.openPdf, container::hasFilesAccess)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Só na primeira criação: após rotação o intent é o mesmo e não deve reimportar.
        if (savedInstanceState == null) handleViewIntent(intent)
        setContent {
            val controller = rememberThemeController()
            val dark = controller.mode.resolveDark(isSystemInDarkTheme())
            ApplySystemBarIcons(dark)
            JohnPdfTheme(controller) { AppNavHost(homeViewModel, container) }
        }
    }

    /**
     * Os ícones da barra de status/navegação não são desenhados pelo Compose — quem decide é
     * a janela. Por isso o enableEdgeToEdge mora aqui dentro, reagindo ao modo escolhido: sem
     * isso, trocar para escuro manualmente num celular claro deixaria ícones escuros sobre
     * barras escuras até reabrir o app.
     *
     * Antes era `SystemBarStyle.light(...)` fixo, que forçava ícones escuros sempre. Fazia
     * sentido enquanto não havia esquema escuro; agora inverteria o problema.
     */
    @Composable
    private fun ApplySystemBarIcons(dark: Boolean) {
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { dark },
                navigationBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { dark },
            )
            onDispose { }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let(homeViewModel::openUri)
    }
}
