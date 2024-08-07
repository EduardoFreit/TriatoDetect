package com.br.triatodetect.ui.home.listImage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.models.Img

class ImageRecyclerAdapter(private val images: Array<Img>) : RecyclerView.Adapter<ImageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.image_listitem, parent, false)
        return ImageHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.bindImage(images[position])
    }
    override fun getItemId(position: Int): Long {
        return position.toLong();
    }

    override fun getItemViewType(position: Int): Int {
        return position;
    }

    override fun getItemCount(): Int {
        return images.size
    }
}