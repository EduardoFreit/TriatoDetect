package com.br.triatodetect.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.ui.theme.TriatoDetectTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.handleActivityResult(
                it.resultCode,
                it.data
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        supportActionBar?.hide()

        observeEvents()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TriatoDetectTheme {
                MainScreen(
                    isLoading = uiState.isLoading,
                    onLoginClick = {
                        viewModel.onLoginClicked()
                    }
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {

                        is MainEvent.OpenLogin ->
                            launcher.launch(event.intent)

                        MainEvent.NavigateHome ->
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    HomeActivity::class.java
                                )
                            )

                        is MainEvent.ShowError ->
                            Toast.makeText(
                                this@MainActivity,
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
            }
        }
    }
}