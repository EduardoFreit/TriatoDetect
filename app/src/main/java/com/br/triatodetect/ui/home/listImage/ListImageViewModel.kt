package com.br.triatodetect.ui.home.listImage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.Utils
import kotlinx.coroutines.launch

class ListImageViewModel(private val user: User) : ViewModel() {

    private val _listImage = MutableLiveData<Array<Img>>()
    val listImage: LiveData<Array<Img>> = _listImage

    init {
        loadListImages()
    }

    private fun loadListImages() {
        Utils.listImagesUser(user.email, "Images") { listImages: Array<Img> ->
            _listImage.value = listImages
        }
    }
}