package com.johngabie.johnpdf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.johngabie.johnpdf.AppContainer
import com.johngabie.johnpdf.engine.MuPdfEngine
import com.johngabie.johnpdf.ui.home.HomeScreen
import com.johngabie.johnpdf.ui.home.HomeViewModel
import com.johngabie.johnpdf.ui.reader.ReaderScreen
import com.johngabie.johnpdf.ui.reader.ReaderViewModel
import java.io.File

@Composable
fun AppNavHost(homeViewModel: HomeViewModel, container: AppContainer) {
    val nav = rememberNavController()
    LaunchedEffect(Unit) {
        homeViewModel.navigation.collect { route ->
            // Um leitor por vez: abrir outro PDF substitui o atual (e fecha o motor dele).
            nav.navigate(route) { popUpTo<HomeRoute>() }
        }
    }
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen(homeViewModel) }
        composable<ReaderRoute> { entry ->
            val route = entry.toRoute<ReaderRoute>()
            val vm: ReaderViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ReaderViewModel(File(route.path), route.title, MuPdfEngine(), container.settings) }
                },
            )
            ReaderScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
