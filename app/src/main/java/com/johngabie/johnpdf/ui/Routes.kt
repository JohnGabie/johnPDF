package com.johngabie.johnpdf.ui

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class ReaderRoute(val path: String, val title: String)
