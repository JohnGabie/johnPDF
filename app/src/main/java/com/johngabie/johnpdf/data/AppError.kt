package com.johngabie.johnpdf.data

import androidx.annotation.StringRes
import com.johngabie.johnpdf.R

enum class AppError(@StringRes val messageRes: Int) {
    CORRUPTED(R.string.error_corrupted),
    GONE(R.string.error_gone),
    NO_SPACE(R.string.error_no_space),
}

@get:StringRes
val PAGE_RENDER_FAILED_MESSAGE: Int get() = R.string.error_page_render
