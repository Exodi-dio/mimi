package com.exodidio.mimi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exodidio.mimi.ui.MimiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app themes itself, because the theme is one of the settings.
        setContent { MimiApp() }
    }
}
