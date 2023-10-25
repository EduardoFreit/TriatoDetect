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
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils


class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val imageDate: TextView
    private val imageLocal: TextView
    private val imageClassify: TextView
    private val imageImage: ImageView
    private var user: User? = null
    private var imageObject: Img? = null
    private var bitmap: Bitmap? = null

    init {
        imageDate = itemView.findViewById(R.id.image_date)
        imageLocal = itemView.findViewById(R.id.image_local)
        imageClassify = itemView.findViewById(R.id.image_classify)
        imageImage = itemView.findViewById(R.id.image_image)
        val sessionManager = SessionManager.getInstance(itemView.context)
        user = sessionManager.getUserData()
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val modalBottomSheet = ModalDetailsImage(imageObject, user, bitmap)
        val fragment = v.context as? AppCompatActivity
        modalBottomSheet.show(fragment!!.supportFragmentManager, ModalDetailsImage.TAG)
    }

    fun bindImage(image: Img) {
        imageObject = image
        Utils.retrieveImage(user!!, image) { bytes: ByteArray? ->
            if (bytes != null) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageImage.setImageBitmap(bitmap)
            }
        }
        imageDate.text = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)

        imageLocal.text = Utils.getCityAndStateFromLocation(
            itemView.context,
            image.latitude!!,
            image.longitude!!
        )

        val textClassify: String = when (image.label) {
            "tb" -> itemView.context.getString(R.string.tb)
            "tp" -> itemView.context.getString(R.string.tp)
            "pm" -> itemView.context.getString(R.string.pm)
            "pl" -> itemView.context.getString(R.string.pl)
            "un" -> itemView.context.getString(R.string.un)
            else -> itemView.context.getString(R.string.un)
        }

        imageClassify.text = textClassify
    }
}
