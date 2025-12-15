package com.br.triatodetect.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityMainBinding
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.service.interfaces.IAuthService
import org.koin.android.ext.android.getKoin
import org.koin.core.qualifier.named
import java.util.Objects

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authService: IAuthService
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Força modo claro (desabilita modo escuro)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sessionManager = SessionManager.getInstance(applicationContext)

        if (Objects.nonNull(sessionManager.getUserData())) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            return
        }

        supportActionBar?.hide()
        setContentView(binding.root)

        binding.loginGoogleButton.setOnClickListener {
            authService = getKoin().get(qualifier = named("google"))
            signIn()
        }
    }

    private fun signIn() {
        val intent = authService.getSignInIntent()
        launcher.launch(intent!!)
    }

    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            authService.handleActivityResult(
                result.resultCode,
                result.data,
                onSuccess = { user ->
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                },
                onError = { err ->
                    Toast.makeText(
                        this,
                        err.message ?: getString(R.string.falha_no_login),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }


    override fun onResume() {
        if (Objects.nonNull(sessionManager.getUserData())) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        super.onResume()
    }

}