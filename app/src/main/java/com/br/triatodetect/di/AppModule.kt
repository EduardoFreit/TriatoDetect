package com.br.triatodetect.di

import com.br.triatodetect.service.TensorFlowClassifyService
import com.br.triatodetect.service.FirebaseService
import com.br.triatodetect.service.interfaces.IBackendService
import com.br.triatodetect.service.interfaces.IClassifyService
import org.koin.dsl.module

val appModule = module {

    single<IBackendService> {
        FirebaseService()
    }

    single<IClassifyService> {
        val backendService: IBackendService = FirebaseService()
        TensorFlowClassifyService(backendService)
    }
}