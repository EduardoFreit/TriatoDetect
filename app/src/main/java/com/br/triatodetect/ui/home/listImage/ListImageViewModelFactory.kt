package com.br.triatodetect.ui.home.listImage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.br.triatodetect.models.User

class ListImageViewModelFactory(private val user: User) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListImageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListImageViewModel(user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
