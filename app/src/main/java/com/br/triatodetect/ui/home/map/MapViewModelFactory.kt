package com.br.triatodetect.ui.home.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.br.triatodetect.models.User

class MapViewModelFactory(private val string: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(string) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
