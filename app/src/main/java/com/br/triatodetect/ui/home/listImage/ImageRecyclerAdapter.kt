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
        val uniqueNewImages = newImages.filter { newImg ->
            images.none { it.imageName == newImg.imageName } // assumindo que Img tem um id único
        }

        if (uniqueNewImages.isNotEmpty()) {
            val startPos = images.size
            images.addAll(uniqueNewImages)
            notifyItemRangeInserted(startPos, uniqueNewImages.size)
        }
    }
}
