package com.mbougar.swapware

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * La clase principal de la aplicación. Se necesita para que Hilt funcione.
 */
@HiltAndroidApp
class SwapWare : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}