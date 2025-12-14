package com.br.triatodetect.ui.home.listImage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.br.triatodetect.models.User
import com.br.triatodetect.service.interfaces.IBackendService

class ListImageViewModelFactory(
    private val user: User,
    private val backendService: IBackendService
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ListImageViewModel::class.java) -> 
                ListImageViewModel(user, backendService) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}