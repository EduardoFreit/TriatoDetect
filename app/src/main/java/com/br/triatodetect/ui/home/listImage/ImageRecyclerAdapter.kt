package com.br.triatodetect.ui.home.listImage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.models.Img

class ImageRecyclerAdapter(private val images: MutableList<Img> = mutableListOf()) :
    RecyclerView.Adapter<ImageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val inflatedView = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_listitem, parent, false)
        return ImageHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.bindImage(images[position])
    }

    override fun getItemCount(): Int = images.size

    // Atualiza a lista sem duplicatas
    fun updateData(newImages: Array<Img>) {
        // Se a lista estiver vazia, adiciona todas as imagens
        if (images.isEmpty()) {
            images.addAll(newImages)
            notifyDataSetChanged()
            return
        }

        // Verifica se há novas imagens
        val uniqueNewImages = newImages.filter { newImg ->
            images.none { it.imageName == newImg.imageName }
        }

        if (uniqueNewImages.isNotEmpty()) {
            val startPos = images.size
            images.addAll(uniqueNewImages)
            notifyItemRangeInserted(startPos, uniqueNewImages.size)
        }
    }

    // Substitui completamente a lista (usado para refresh)
    fun refreshData(newImages: Array<Img>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }
}
