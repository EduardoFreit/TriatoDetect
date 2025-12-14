package com.br.triatodetect.service.interfaces

import android.content.Intent
import com.br.triatodetect.models.User

/**
 * Contrato genérico para serviços de autenticação.
 * Permite trocar ou adicionar provedores (Google, Email/Senha, Apple etc.).
 */
interface IAuthService {
    /** Retorna o intent para iniciar o fluxo de autenticação (quando aplicável). */
    fun getSignInIntent(): Intent?

    /** Trata o resultado do fluxo (ActivityResult) e retorna via callbacks. */
    fun handleActivityResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (User) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun signOut(onComplete: (() -> Unit)? = null)
}