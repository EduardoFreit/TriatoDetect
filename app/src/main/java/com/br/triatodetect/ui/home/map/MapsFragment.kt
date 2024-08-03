package com.br.triatodetect.ui.home.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.br.triatodetect.R
import com.br.triatodetect.databinding.FragmentMapsBinding
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment() {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var user: User? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())
        user = sessionManager.getUserData()
    }

    private fun getMarkerIcon(color: String?): BitmapDescriptor {
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.parseColor(color), hsv)
        return BitmapDescriptorFactory.defaultMarker(hsv[0])
    }

    private fun colorToHex(context: Context, colorResId: Int): String {
        val colorInt = ContextCompat.getColor(context, colorResId)
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }

    private val callback = OnMapReadyCallback { mMap: GoogleMap ->
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(binding.root.context)
        if (!(ActivityCompat.checkSelfPermission(
                binding.root.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                binding.root.context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    )
        ) {
            if(Utils.args == null) {
                val zoomLevel = 6.3f
                val userLocation = LatLng(-8.363123, -37.861396)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel)
                mMap.moveCamera(cameraUpdate)

                Utils.listImagesUser(user!!.email, "Images") { listImages: Array<Img> ->
                    for (image in listImages) {
                        val localization = LatLng(image.latitude!!, image.longitude!!)

                        val textClassify: String = when (image.label) {
                            "n" -> getString(R.string.n)
                            "s" -> getString(R.string.s)
                            else -> getString(R.string.u)
                        }

                        val colorClassify: String = when (image.label) {
                            "n" -> this.colorToHex(binding.root.context, R.color.n)
                            "s" -> this.colorToHex(binding.root.context, R.color.s)
                            else -> this.colorToHex(binding.root.context, R.color.u)
                        }

                        val imageDate: String? = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)

                        mMap.addMarker(MarkerOptions().position(localization).title(textClassify).snippet(imageDate))?.setIcon(
                            getMarkerIcon(colorClassify))
                    }
                }
            } else {
                val latitudeImageSelect: Double? = Utils.args?.getDouble(LATITUDE_IMAGE)
                val longitudeImageSelect: Double? = Utils.args?.getDouble(LONGITUDE_IMAGE)
                val classifyImageSelect: String? = Utils.args?.getString(CLASSIFY)
                val dateImageSelect: String? = Utils.args?.getString(DATE)
                val zoomLevel = 13.0f
                val localization = LatLng(latitudeImageSelect!!, longitudeImageSelect!!)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(localization, zoomLevel)
                Utils.args = null

                mMap.moveCamera(cameraUpdate)
                mMap.addMarker(MarkerOptions().position(localization).title(classifyImageSelect).snippet(dateImageSelect))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    companion object {
        const val LATITUDE_IMAGE = "latitude"
        const val LONGITUDE_IMAGE = "longitude"
        const val CLASSIFY = "classify"
        const val DATE = "date"
    }
}