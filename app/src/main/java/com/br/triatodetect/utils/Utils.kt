package com.br.triatodetect.utils

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.br.triatodetect.R
import java.io.IOException
import java.util.Locale

// Utilitários gerais para o aplicativo
object Utils {
    var args: Bundle? = null

    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    fun getCityAndStateFromLocation(context: Context, latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        var city = ""
        var state = ""

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                city = address.locality ?: ""
                if(city.isBlank()) {
                    city = address.subAdminArea ?: ""
                }
                state = address.adminArea ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return if (city.isNotEmpty() && state.isNotEmpty()) {
            "$city - $state"
        } else {
            null
        }
    }

    fun showLoading(context: Context, layout: ViewGroup, hideViews: List<View>): ProgressBar {
        val progressBar = ProgressBar(context)
        progressBar.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // Define o padding horizontal em pixels (150dp)
        val paddingInDp = 150f
        val scale = context.resources.displayMetrics.density
        val paddingInPixels = (paddingInDp * scale + 0.5f).toInt()
        progressBar.setPadding(paddingInPixels, 0, paddingInPixels, 0)

        progressBar.setBackgroundResource(R.drawable.loading)
        progressBar.elevation = 4f

        layout.addView(progressBar)

        hideViews.forEach { view: View ->
            view.visibility  = View.GONE
        }

        return progressBar
    }

    fun hideLoading(progressBar: ProgressBar, layout: ViewGroup, showViews: List<View>) {
        showViews.forEach { view: View ->
            view.visibility  = View.VISIBLE
        }
        layout.removeView(progressBar)
    }

}