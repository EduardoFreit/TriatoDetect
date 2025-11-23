package com.br.triatodetect.ui.home.listImage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.service.FirebaseService

class ListImageViewModel(private val user: User) : ViewModel() {

    private val _listImage = MutableLiveData<Array<Img>>()
    val listImage: LiveData<Array<Img>> = _listImage
    private val firebaseService = FirebaseService()

    init {
        loadListImages()
    }

    private fun loadListImages() {
        firebaseService.listImagesUser(user.email, "Images") { listImages: Array<Img> ->
            _listImage.value = listImages
        }
    }

    fun refreshListImages() {
        loadListImages()
    }
}