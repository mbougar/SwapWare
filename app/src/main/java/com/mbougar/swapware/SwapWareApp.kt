package com.mbougar.swapware

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SwapWare : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}