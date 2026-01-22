package com.br.triatodetect.ui.main

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.triatodetect.service.interfaces.IAuthService
import com.br.triatodetect.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val authService: IAuthService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        if (sessionManager.getUserData() != null) {
            viewModelScope.launch {
                _event.emit(MainEvent.NavigateHome)
            }
        }
    }

    fun onLoginClicked() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            _event.emit(
                MainEvent.OpenLogin(
                    authService.getSignInIntent()!!
                )
            )
        }
    }

    fun handleActivityResult(
        resultCode: Int,
        data: Intent?
    ) {
        authService.handleActivityResult(
            resultCode,
            data,
            onSuccess = {
                _uiState.value = MainUiState()
                viewModelScope.launch {
                    _event.emit(MainEvent.NavigateHome)
                }
            },
            onError = {
                _uiState.value = MainUiState(errorMessage = it.message)
                viewModelScope.launch {
                    _event.emit(
                        MainEvent.ShowError(
                            it.message ?: "Erro no login"
                        )
                    )
                }
            }
        )
    }
}

