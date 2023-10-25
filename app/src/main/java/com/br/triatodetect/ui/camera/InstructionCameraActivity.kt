package com.br.triatodetect.ui.camera

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityInstructionCameraBinding
import com.br.triatodetect.utils.Utils

class InstructionCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstructionCameraBinding
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Utils.setUriByteArray(it, applicationContext)
            val intent = Intent(this@InstructionCameraActivity, ConfirmImageActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionCameraBinding.inflate(layoutInflater)

        binding.instructionButtonFoto.setOnClickListener {
            this.openCamera()
        }

        binding.instructionButtonImagem.setOnClickListener { getContent.launch("image/*") }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.instruction_title)

        setContentView(binding.root)
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
}