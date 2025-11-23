package com.br.triatodetect.ui.camera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.br.triatodetect.databinding.ActivityConfirmImageBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.br.triatodetect.R
import com.br.triatodetect.service.ClassifyService
import com.br.triatodetect.utils.ImageUtils
import kotlinx.coroutines.launch

class ConfirmImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmImageBinding
    private lateinit var sessionManager: SessionManager
    private var user: User? = null
    private var image: ByteArray? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val classifyService = ClassifyService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmImageBinding.inflate(layoutInflater)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sessionManager = SessionManager.getInstance(applicationContext)
        this.user = sessionManager.getUserData()

        image = ImageUtils.getImageByteArray()
        image?.let {
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.imageView.setImageBitmap(bitmap)
        }

        supportActionBar?.title = getString(R.string.confirm_image)
        setContentView(binding.root)

        binding.floatButtonCancel.setOnClickListener { cancelImage() }
        binding.floatButtonAccept.setOnClickListener { processImage() }
    }

    fun processImage() {
        image?.let {
            val progressBar = Utils.showLoading(
                this,
                binding.layout,
                listOf(
                    binding.floatButtonAccept,
                    binding.floatButtonCancel
                )
            )
            lifecycleScope.launch {
                classifyService.initClassify(this@ConfirmImageActivity, it, user) { success: Boolean ->
                    Utils.hideLoading(progressBar, binding.layout, listOf())
                    if(!success) {
                        Toast.makeText(this@ConfirmImageActivity, getString(R.string.erro_proc_image), Toast.LENGTH_SHORT).show();
                    }
                    val intent = Intent(this@ConfirmImageActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun cancelImage() {
        ImageUtils.resetImageByteArray()
        val intent = Intent(this, InstructionCameraActivity::class.java)
        startActivity(intent)
        finish()
    }

}