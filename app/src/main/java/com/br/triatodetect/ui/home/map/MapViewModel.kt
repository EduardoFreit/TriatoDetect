package com.br.triatodetect.ui.home.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.Utils
import kotlinx.coroutines.launch

class MapViewModel(private val string: String) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Olaaaa"
    }

    val text: LiveData<String> = _text
}