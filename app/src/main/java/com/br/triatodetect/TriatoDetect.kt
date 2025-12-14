package com.br.triatodetect

import android.app.Application
import com.br.triatodetect.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TriatoDetect: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin() {
            androidContext(this@TriatoDetect)
            modules(appModule)
        }
    }
}