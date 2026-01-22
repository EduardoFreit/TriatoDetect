package com.br.triatodetect.ui.main

data class MainUiState(
    val isLoading: Boolean = false,
    val isLogged: Boolean = false,
    val errorMessage: String? = null
)