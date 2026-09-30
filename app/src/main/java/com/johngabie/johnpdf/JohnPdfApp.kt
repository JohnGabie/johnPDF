package com.johngabie.johnpdf

import android.app.Application

class JohnPdfApp : Application() {
    val container by lazy { AppContainer(this) }
}
