package com.br.triatodetect.ui.home.listImage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.service.FirebaseService
import com.br.triatodetect.ui.component.ModalDetailsImage
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils

class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val imageDate: TextView = itemView.findViewById(R.id.image_date)
    private val imageLocal: TextView = itemView.findViewById(R.id.image_local)
    private val imageClassify: TextView = itemView.findViewById(R.id.image_classify)
    private val imageImage: ImageView = itemView.findViewById(R.id.image_image)
    private var user: User? = null
    private var imageObject: Img? = null
    private var bitmap: Bitmap? = null
    private val firebaseService = FirebaseService()

    companion object {
        // Cache simples para imagens já carregadas
        private val imageCache = mutableMapOf<String, Bitmap>()
        
        // Método para limpar o cache se necessário (evitar uso excessivo de memória)
        fun clearImageCache() {
            imageCache.clear()
        }
        
        // Método para remover imagens antigas do cache (manter apenas as últimas 50)
        fun trimImageCache() {
            if (imageCache.size > 50) {
                val keysToRemove = imageCache.keys.take(imageCache.size - 50)
                keysToRemove.forEach { imageCache.remove(it) }
            }
        }
    }

    init {
        val sessionManager = SessionManager.getInstance(itemView.context)
        user = sessionManager.getUserData()
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val modalBottomSheet = ModalDetailsImage(imageObject, bitmap)
        val fragment = v.context as? AppCompatActivity
        modalBottomSheet.show(fragment!!.supportFragmentManager, ModalDetailsImage.TAG)
    }

    fun bindImage(image: Img) {
        imageObject = image
        
        // Limpa a imagem anterior para evitar conflito visual
        imageImage.setImageDrawable(null)
        bitmap = null
        
        // Verifica se a imagem já está no cache
        val cachedBitmap = imageCache[image.imageName]
        if (cachedBitmap != null) {
            bitmap = cachedBitmap
            imageImage.setImageBitmap(bitmap)
            imageImage.setBackgroundColor(0x00000000)
        } else {
            // Define uma cor de fundo cinza enquanto carrega
            imageImage.setBackgroundColor(0xFFE0E0E0.toInt())
            
            firebaseService.retrieveImage(user!!, image) { bytes: ByteArray? ->
                // Verifica se ainda é a mesma imagem (evita race conditions)
                if (imageObject?.imageName == image.imageName && bytes != null) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    // Adiciona ao cache e verifica se precisa limpar cache antigo
                    bitmap?.let { 
                        imageCache[image.imageName!!] = it
                        trimImageCache()
                    }
                    
                    imageImage.setImageBitmap(bitmap)
                    // Remove o background após carregar a imagem
                    imageImage.setBackgroundColor(0x00000000)
                }
            }
        }
        
        imageDate.text = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)

        imageLocal.text = Utils.getCityAndStateFromLocation(
            itemView.context,
            image.latitude!!,
            image.longitude!!
        )

        val textClassify: String = when (image.label) {
            "n" -> itemView.context.getString(R.string.n)
            "s" -> itemView.context.getString(R.string.s)
            else -> itemView.context.getString(R.string.u)
        }

        imageClassify.text = textClassify
    }
}
