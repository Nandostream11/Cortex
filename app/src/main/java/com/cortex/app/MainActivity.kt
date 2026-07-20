package com.cortex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cortex.app.ui.navigation.CortexApp
import com.cortex.app.ui.theme.CortexTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as CortexApplication).container

        setContent {
            CortexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CortexApp(container)
                }
            }
        }
    }
}
