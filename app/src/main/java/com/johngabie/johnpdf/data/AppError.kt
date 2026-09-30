package com.johngabie.johnpdf.data

enum class AppError(val message: String) {
    CORRUPTED("Não foi possível abrir este arquivo."),
    GONE("Este arquivo não está mais disponível."),
    NO_SPACE("Sem espaço no celular para abrir este arquivo."),
}

const val PAGE_RENDER_FAILED_MESSAGE = "Não foi possível mostrar esta página."
