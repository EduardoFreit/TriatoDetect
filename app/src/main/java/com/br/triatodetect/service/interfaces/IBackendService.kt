package com.br.triatodetect.service.interfaces

import android.content.Context
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User

interface IBackendService {
    fun listImagesUser(email: String?, collection: String, callback: (Array<Img>) -> Unit)
    fun listImages(collection: String, callback: (Array<Img>) -> Unit)
    fun saveImageStores(image: ByteArray, user: User?, context: Context, classifyResult: MutableList<String>, callback: (Boolean) -> Unit)
    fun retrieveImage(user: User, image: Img, callback: (ByteArray?) -> Unit)
    fun sendEmailClassification(context: Context, rowImage: Img, image: ByteArray)
}