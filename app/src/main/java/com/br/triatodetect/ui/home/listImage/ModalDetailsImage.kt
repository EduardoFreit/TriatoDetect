package com.br.triatodetect.ui.home.listImage

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.br.triatodetect.R
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.home.map.MapsFragment
import com.br.triatodetect.utils.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ModalDetailsImage(private val image: Img?, private val user: User?, private val bitmap: Bitmap?) : BottomSheetDialogFragment() {

    private lateinit var imageDate: TextView
    private lateinit var imageClassify: TextView
    private lateinit var imageImage: ImageView
    private lateinit var imageLocalization: TextView
    private lateinit var view: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val textDate: String = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)

        imageDate = view.findViewById(R.id.image_date)
        imageClassify = view.findViewById(R.id.image_classify)

        imageImage = view.findViewById(R.id.image_image)
        imageLocalization = view.findViewById(R.id.image_localization)

        imageClassify.text = textClassify
        imageClassify.paintFlags = imageClassify.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        imageClassify.setTextColor(colorClassify)
        imageLocalization.text =  Utils.getCityAndStateFromLocation(view.context, image.latitude!!, image.longitude!!)
        imageDate.text = textDate
        imageImage.setImageBitmap(bitmap)

        val bottomNavigationView: BottomNavigationView = requireActivity().findViewById(R.id.nav_view)
        val button: Button = view.findViewById(R.id.button_map)
        button.setOnClickListener {
            dismiss()
            Utils.args = Bundle()
            Utils.args!!.putDouble(MapsFragment.LATITUDE_IMAGE, image.latitude)
            Utils.args!!.putDouble(MapsFragment.LONGITUDE_IMAGE, image.longitude)
            Utils.args!!.putString(MapsFragment.CLASSIFY, textClassify)
            Utils.args!!.putString(MapsFragment.DATE, textDate)

            val navController = requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
            navController.popBackStack()
            navController.navigate(R.id.navigation_map)
            bottomNavigationView.selectedItemId = R.id.navigation_map
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val d = it as BottomSheetDialog
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
