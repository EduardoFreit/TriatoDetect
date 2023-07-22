package com.br.triatodetect.ui.home.listImage

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.br.triatodetect.R
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ModalDetailsImage(private val image: Img?, private val user: User?, private val bitmap: Bitmap?) : BottomSheetDialogFragment() {

    private lateinit var imageStatus: TextView
    private lateinit var imageClassify: TextView
    private lateinit var imageImage: ImageView
    private lateinit var imageLocalization: TextView
    private lateinit var view: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(R.layout.modal_details_image, container, false)
        this.renderModalContent()
        return view
    }

    private fun renderModalContent() {

        val textClassify: String = when (image!!.label) {
            "tb" -> getString(R.string.tb)
            "tp" -> getString(R.string.tp)
            "pm" -> getString(R.string.pm)
            "pl" -> getString(R.string.pl)
            else -> getString(R.string.un)
        }

        val colorClassify: Int = when (image.label) {
            "tb" -> ContextCompat.getColor(view.context, R.color.tb)
            "tp" -> ContextCompat.getColor(view.context, R.color.tp)
            "pm" -> ContextCompat.getColor(view.context, R.color.pm)
            "pl" -> ContextCompat.getColor(view.context, R.color.pl)
            else -> ContextCompat.getColor(view.context, R.color.un)
        }

        val textStatus: String = when (image.status) {
            StatusImage.PENDENTE -> getString(R.string.pendente).trim().uppercase()
            StatusImage.AGUARDANDO_CONFIRMACAO -> getString(R.string.aguardando_confirmacao).trim().uppercase()
            StatusImage.FINALIZADO -> getString(R.string.finalizado).trim().uppercase()
        }

        imageStatus = view.findViewById(R.id.image_status)
        imageClassify = view.findViewById(R.id.image_classify)

        imageImage = view.findViewById(R.id.image_image)
        imageLocalization = view.findViewById(R.id.image_localization)

        imageClassify.text = textClassify
        imageClassify.paintFlags = imageClassify.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        imageClassify.setTextColor(colorClassify)
        imageLocalization.text =  Utils.getCityAndStateFromLocation(view.context, image.latitude!!, image.longitude!!)
        imageStatus.text = textStatus
        imageImage.setImageBitmap(bitmap)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.standard_bottom_sheet) as LinearLayout?
            BottomSheetBehavior.from(bottomSheet!!).state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Do something with your dialog like setContentView() or whatever
        return dialog
    }

    companion object {
        const val TAG = "ModalDetailsImage"
    }
}
