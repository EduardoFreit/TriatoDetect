package com.br.triatodetect.di

import com.br.triatodetect.models.User
import com.br.triatodetect.service.TensorFlowClassifyService
import com.br.triatodetect.service.FirebaseService
import com.br.triatodetect.service.GoogleAuthService
import com.br.triatodetect.service.interfaces.IAuthService
import com.br.triatodetect.service.interfaces.IBackendService
import com.br.triatodetect.service.interfaces.IClassifyService
import com.br.triatodetect.ui.home.listImage.ListImageViewModel
import com.br.triatodetect.ui.main.MainViewModel
import com.br.triatodetect.utils.SessionManager
import org.koin.core.module.dsl.*
import org.koin.core.qualifier.named
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    single {
        SessionManager(androidContext())
    }

    single<IBackendService> {
        val backendService: IBackendService = FirebaseService()
        backendService
    }

    single<IClassifyService> {
        TensorFlowClassifyService(get())
    }

    single<IAuthService>(qualifier = named("google")) {
        GoogleAuthService(androidContext())
    }

    viewModel { (user: User) ->
        ListImageViewModel(user, get())
    }

    viewModel {
        MainViewModel(
            authService = get(named("google")),
            sessionManager = get()
        )
    }

}