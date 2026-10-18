package com.exodidio.mimi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exodidio.mimi.ui.MimiApp
import com.exodidio.mimi.ui.theme.MimiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MimiTheme {
                MimiApp()
            }
        }
    }
}
