package com.br.triatodetect.ui.home.listImage

import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val imageDate: TextView
    private val imageStatus: TextView
    private val imageClassify: TextView
    private val imageImage: ImageView
    private var user: User? = null

    init {
        imageDate = itemView.findViewById(R.id.image_date)
        imageStatus = itemView.findViewById(R.id.image_status)
        imageClassify = itemView.findViewById(R.id.image_classify)
        imageImage = itemView.findViewById(R.id.image_image)
        val sessionManager = SessionManager.getInstance(itemView.context)
        user = sessionManager.getUserData()
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val modalBottomSheet = ModalBottomSheet()
        val fragment = v.context as? AppCompatActivity
        modalBottomSheet.show(fragment!!.supportFragmentManager, ModalBottomSheet.TAG)
    }

    fun bindImage(image: Img) {
        GlobalScope.launch {
            try {
                val bytes = Utils.retrieveImage(user!!, image)
                if (bytes != null) {
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageImage.setImageBitmap(bitmap)
                }
            } catch (exception: Exception) {
                // Lógica para lidar com a falha no download
                Log.e("Download", "Error downloading image", exception)
            }
        }
        imageDate.text = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)
        imageStatus.text = image.status.name
        imageClassify.text = image.label
    }
}
