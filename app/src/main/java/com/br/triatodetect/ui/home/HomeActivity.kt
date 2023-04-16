package com.br.triatodetect.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.br.triatodetect.ui.main.MainActivity
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityHomeBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.BaseActivity
import com.br.triatodetect.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.Objects

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        sessionManager = SessionManager.getInstance(applicationContext)
        setContentView(binding.root)

        val user: User? = sessionManager.getUserData()
        binding.email.text = user?.email
        binding.nome.text = user?.name
        binding.logoutButton.setOnClickListener { super.signOut() }

        val name: String = user?.name!!
            .split(" ")[0]
            .lowercase()
            .replaceFirstChar {
                it.uppercase()
            }
        val nameTitle: String = getString(R.string.welcome_title, name);
        supportActionBar ?.title = nameTitle
    }

    override fun onBackPressed() {
        if(Objects.nonNull(sessionManager.getUserData())) {
            moveTaskToBack(true)
        }
    }


}