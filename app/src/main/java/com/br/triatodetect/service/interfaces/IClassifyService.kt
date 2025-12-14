package com.br.triatodetect.service.interfaces

import android.content.Context
import com.br.triatodetect.models.User

interface IClassifyService {
    suspend fun initClassify(context: Context, bytes: ByteArray, user: User?, callback: (Boolean) -> Unit)
}