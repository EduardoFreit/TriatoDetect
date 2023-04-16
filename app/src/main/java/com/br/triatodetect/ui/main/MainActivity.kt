package com.br.triatodetect.ui.main

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.br.triatodetect.R
import com.br.triatodetect.databinding.ActivityMainBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Objects

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sessionManager = SessionManager.getInstance(applicationContext)

        if(Objects.nonNull(sessionManager.getUserData())) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            return;
        }

        supportActionBar?.hide()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginButton.setOnClickListener { signInGoogle() }
    }

    private fun signInGoogle() {
        val signInClient: Intent = googleSignInClient.signInIntent
        launcher.launch(signInClient)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if(task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if(account != null) {
                updateUI(account)
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful) {
                val intent = Intent(this, HomeActivity::class.java)
                val user = User(account.displayName, account.email)
                sessionManager.saveUserData(user)
                startActivity(intent)
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        if(Objects.nonNull(sessionManager.getUserData())) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        super.onResume()
    }

}