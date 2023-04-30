package com.br.triatodetect.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityHomeBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.BaseActivity
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import java.util.Objects

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //permissão GPS
        this.permissionGPS()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        sessionManager = SessionManager.getInstance(applicationContext)
        setContentView(binding.root)

        val user: User? = sessionManager.getUserData()

        val name: String = user?.name!!
            .split(" ")[0]
            .lowercase()
            .replaceFirstChar {
                it.uppercase()
            }
        val nameTitle: String = getString(R.string.welcome_title, name);
        supportActionBar ?.title = nameTitle

        binding.floatButtonCamera.setOnClickListener { this.permissionCamera() }
    }

    override fun onBackPressed() {
        if(Objects.nonNull(sessionManager.getUserData())) {
            moveTaskToBack(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                // Se a permissão foi concedida, abra a câmera.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.openCamera()
                } else {
                    Toast.makeText(this, "A permissão para acessar a câmera foi negada.", Toast.LENGTH_SHORT).show()
                }
                return
            }
            FINE_LOCATION_REQUEST -> {
                // Se a permissão foi concedida, abra a câmera.
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "A permissão para acessar o GPS foi negada.", Toast.LENGTH_SHORT).show()
                    this.signOut()
                }
                return
            }
        }
    }

    private fun permissionGPS() {
        if (Utils.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_REQUEST)
        }
    }

    private fun permissionCamera() {
        if (Utils.checkPermission(this, Manifest.permission.CAMERA)) {
            // Se a permissão não tiver sido concedida, solicite-a ao usuário.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            this.openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

}