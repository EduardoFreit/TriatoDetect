package com.br.triatodetect.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityMapsBinding
import com.br.triatodetect.models.Img
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


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val zoomLevel = 15.0f

                        val userLocation = LatLng(location.latitude, location.longitude)

                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel)
                        mMap.moveCamera(cameraUpdate)
                    }
                }

            Utils.listDocuments("Images") { listImages: Array<Img> ->
                // Add a marker in Sydney and move the camera

                for (image in listImages) {
                    val localization = LatLng(image.latitude!!, image.longitude!!)

                    val textClassify: String = when (image.label) {
                        "tb" -> getString(R.string.tb)
                        "tp" -> getString(R.string.tp)
                        "pm" -> getString(R.string.pm)
                        "pl" -> getString(R.string.pl)
                        else -> getString(R.string.un)
                    }

                    val colorClassify: String = when (image.label) {
                        "tb" -> this.colorToHex(binding.root.context, R.color.tb)
                        "tp" -> this.colorToHex(binding.root.context, R.color.tp)
                        "pm" -> this.colorToHex(binding.root.context, R.color.pm)
                        "pl" -> this.colorToHex(binding.root.context, R.color.pl)
                        else -> this.colorToHex(binding.root.context, R.color.un)
                    }

                    val imageDate = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(image.date)

                    mMap.addMarker(MarkerOptions().position(localization).title(textClassify).snippet(imageDate))?.setIcon(
                        getMarkerIcon(colorClassify))
                }
            }
        }

    }
}