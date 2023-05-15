package com.br.triatodetect.ui.camera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.br.triatodetect.databinding.ActivityInstructionCameraBinding

class InstructionCameraActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityInstructionCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityInstructionCameraBinding.inflate(layoutInflater)

        viewBinding.instructionButton.setOnClickListener {
            this.openCamera()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(viewBinding.root)
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
}