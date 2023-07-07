package com.br.triatodetect.ui.home.listImage

import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.models.Img


class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val imageDate: TextView
    private val imageStatus: TextView
    private val imageClassify: TextView
    private var image: Img? = null

    init {
        imageDate = itemView.findViewById(R.id.image_date)
        imageStatus = itemView.findViewById(R.id.image_status)
        imageClassify = itemView.findViewById(R.id.image_classify)
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        Toast.makeText(
            v.context, "Cidade selecionada: " +
                    image!!.email, Toast.LENGTH_SHORT
        ).show()
    }

    fun bindImage(image: Img) {
        this.image = image
        imageDate.text = image.email
        imageStatus.text = image.status.name
        imageClassify.text = image.label
    }
}
