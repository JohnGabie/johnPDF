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
        // statusBarStyle/navigationBarStyle "light" força ícones escuros sobre fundo
        // transparente, independente do modo escuro do sistema (senão os ícones da
        // barra de status somem no dark mode).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
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
