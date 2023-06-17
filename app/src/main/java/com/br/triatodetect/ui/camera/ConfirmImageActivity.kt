package com.br.triatodetect.ui.camera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.br.triatodetect.databinding.ActivityCameraBinding
import com.br.triatodetect.databinding.ActivityConfirmImageBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.classify.ClassifyActivity
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class ConfirmImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmImageBinding
    private lateinit var sessionManager: SessionManager
    private var user: User? = null
    private var image: ByteArray? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmImageBinding.inflate(layoutInflater)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sessionManager = SessionManager.getInstance(applicationContext)
        this.user = sessionManager.getUserData()

        image = Utils.getImageByteArray()
        image?.let {
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.imageView.setImageBitmap(bitmap);
        }

        supportActionBar?.hide()
        setContentView(binding.root)


        binding.floatButtonCancel.setOnClickListener { cancelImage() }
        binding.floatButtonAccept.setOnClickListener { processImage() }
    }

    private fun processImage() {
        image?.let {
            Utils.saveImageStores(it, user, this)
            Utils.classify(this, it)
            val intent = Intent(this@ConfirmImageActivity, ClassifyActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun cancelImage() {
        Utils.resetImageByteArray()
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish()
    }

}