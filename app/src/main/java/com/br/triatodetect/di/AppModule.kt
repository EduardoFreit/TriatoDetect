package com.br.triatodetect.di

import com.br.triatodetect.models.User
import com.br.triatodetect.service.TensorFlowClassifyService
import com.br.triatodetect.service.FirebaseService
import com.br.triatodetect.service.GoogleAuthService
import com.br.triatodetect.service.interfaces.IAuthService
import com.br.triatodetect.service.interfaces.IBackendService
import com.br.triatodetect.service.interfaces.IClassifyService
import com.br.triatodetect.ui.home.listImage.ListImageViewModel
import org.koin.core.module.dsl.*
import org.koin.core.qualifier.named
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    val backendService: IBackendService = FirebaseService()

    single<IBackendService> {
        backendService
    }

    single<IClassifyService> {
        TensorFlowClassifyService(backendService)
    }

    single<IAuthService>(qualifier = named("google")) {
        GoogleAuthService(androidContext())
    }

    viewModel { (user: User) ->
        ListImageViewModel(user, get())
    }

}