package de.fabiobaensch.glitchy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.fabiobaensch.glitchy.ui.GlitchyApp
import de.fabiobaensch.glitchy.ui.GlitchyTheme
import de.fabiobaensch.glitchy.ui.Haptics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Haptics.enabled = getSharedPreferences("glitchy", 0).getBoolean("haptics", true)
        setContent {
            GlitchyTheme { GlitchyApp() }
        }
    }
}
