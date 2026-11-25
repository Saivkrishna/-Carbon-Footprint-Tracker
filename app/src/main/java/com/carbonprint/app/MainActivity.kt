package com.carbonprint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carbonprint.app.ui.MainScreen
import com.carbonprint.app.ui.theme.CarbonPrintTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarbonPrintTheme {
                MainScreen()
            }
        }
    }
}
