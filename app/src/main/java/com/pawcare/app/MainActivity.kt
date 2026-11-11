package com.pawcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pawcare.app.di.AppContainer
import com.pawcare.app.ui.navigation.PawCareNavHost
import com.pawcare.app.ui.theme.PawCareTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PawCareTheme {
                PawCareNavHost(container = container)
            }
        }
    }
}
