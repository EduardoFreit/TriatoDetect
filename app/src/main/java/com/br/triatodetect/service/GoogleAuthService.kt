package com.br.triatodetect.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.br.triatodetect.R
import com.br.triatodetect.models.User
import com.br.triatodetect.service.interfaces.IAuthService
import com.br.triatodetect.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Service responsável por encapsular toda a lógica de autenticação com Google + Firebase.
 * Mantém a Activity livre de detalhes de implementação.
 */
class GoogleAuthService(
    private val context: Context
) : IAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sessionManager: SessionManager = SessionManager.getInstance(context.applicationContext)
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /** Retorna o intent para iniciar o fluxo de login do Google. */
    override fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /** Trata o resultado do ActivityResult e realiza login no Firebase. */
    override fun handleActivityResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (User) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (resultCode != Activity.RESULT_OK) {
            onError(IllegalStateException("Login cancelado ou inválido"))
            return
        }
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                signInWithFirebase(account, onSuccess, onError)
            } else {
                onError(IllegalStateException("Conta Google nula"))
            }
        } else {
            onError(task.exception ?: RuntimeException("Falha ao obter conta Google"))
        }
    }

    /** Realiza login no Firebase usando credencial do Google e persiste usuário na sessão. */
    private fun signInWithFirebase(
        account: GoogleSignInAccount,
        onSuccess: (User) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = User(account.displayName, account.email)
                sessionManager.saveUserData(user)
                onSuccess(user)
            } else {
                onError(task.exception ?: RuntimeException("Falha ao autenticar no Firebase"))
            }
        }
    }

    override fun signOut(onComplete: (() -> Unit)?) {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            sessionManager.clearAllData()
            onComplete?.invoke()
        }
    }
}