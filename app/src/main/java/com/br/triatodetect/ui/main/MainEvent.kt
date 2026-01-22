package com.br.triatodetect.ui.main

import android.content.Intent

sealed class MainEvent {
    object NavigateHome : MainEvent()
    data class OpenLogin(val intent: Intent) : MainEvent()
    data class ShowError(val message: String) : MainEvent()
}