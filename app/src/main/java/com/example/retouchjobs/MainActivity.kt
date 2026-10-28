package com.example.retouchjobs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.retouchjobs.ui.MainScreen
import com.example.retouchjobs.ui.theme.RetouchJobsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetouchJobsTheme {
                MainScreen()
            }
        }
    }
}
