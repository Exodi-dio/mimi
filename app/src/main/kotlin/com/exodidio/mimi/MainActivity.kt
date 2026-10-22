package com.exodidio.mimi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.exodidio.mimi.ui.MimiApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * Whether the player should be opened as soon as the app is on screen.
     *
     * Held here rather than read once, because a widget can be tapped while the app is
     * already open, and then the app is asked to open on the player all over again.
     */
    private val openPlayer = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPlayer.value = intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true
        // The app themes itself, because the theme is one of the settings.
        setContent {
            val asked by openPlayer.collectAsState()
            MimiApp(openPlayer = asked)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) openPlayer.value = true
    }

    companion object {
        /** The extra that asks the app to open on the player. */
        const val EXTRA_OPEN_PLAYER = "com.exodidio.mimi.OPEN_PLAYER"
    }
}
