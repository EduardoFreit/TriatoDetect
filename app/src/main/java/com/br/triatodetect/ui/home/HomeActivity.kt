package com.br.triatodetect.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityHomeBinding
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.BaseActivity
import com.br.triatodetect.ui.camera.CameraActivity
import com.br.triatodetect.ui.home.listImage.ImageRecyclerAdapter
import com.br.triatodetect.ui.home.listImage.ModalBottomSheet
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import java.util.Objects

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //permissão GPS
        this.permissionGPS()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        sessionManager = SessionManager.getInstance(applicationContext)
        setContentView(binding.root)

        user = sessionManager.getUserData()

        val name: String = user?.name!!
            .split(" ")[0]
            .lowercase()
            .replaceFirstChar {
                it.uppercase()
            }
        val nameTitle: String = getString(R.string.welcome_title, name);
        supportActionBar ?.title = nameTitle

        val modalBottomSheet = ModalBottomSheet()
        modalBottomSheet.show(this.supportFragmentManager, ModalBottomSheet.TAG)

        binding.floatButtonCamera.setOnClickListener { this.openInstructionCamera() }

    }

    override fun onResume() {
        super.onResume()
        Utils.listImagesUser(user!!.email, "Images") { listImages: Array<Img> ->
            val recyclerView: RecyclerView = findViewById(binding.listView.id)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = ImageRecyclerAdapter(listImages, applicationContext)
        }
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

    private fun openInstructionCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

}