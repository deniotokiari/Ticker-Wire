package pl.deniotokiari.tickerwire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.deniotokiari.tickerwire.common.data.AndroidContextHolder
import pl.deniotokiari.tickerwire.feature.app.presentation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize context holder for platform-specific components
        AndroidContextHolder.initialize(this)

        setContent {
            App()
        }
    }
}
