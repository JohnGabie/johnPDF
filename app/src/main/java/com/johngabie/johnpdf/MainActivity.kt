package com.johngabie.johnpdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.johngabie.johnpdf.ui.AppNavHost
import com.johngabie.johnpdf.ui.home.HomeViewModel
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme

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
        // auto(): ícones escuros no claro e claros no escuro, acompanhando o sistema.
        // Era light() enquanto não existia darkColorScheme (conserto F6); agora que
        // existe, light() seria o bug — ícones escuros sobre surface #0F1417 somem.
        // Os dois argumentos são os scrims usados quando falta contraste; TRANSPARENT
        // nos dois mantém o edge-to-edge real.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        // Só na primeira criação: após rotação o intent é o mesmo e não deve reimportar.
        if (savedInstanceState == null) handleViewIntent(intent)
        setContent { JohnPdfTheme { AppNavHost(homeViewModel, container) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let(homeViewModel::openUri)
    }
}
