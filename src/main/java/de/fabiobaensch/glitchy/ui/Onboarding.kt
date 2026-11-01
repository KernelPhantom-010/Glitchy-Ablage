package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Onboarding(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scale = remember { Animatable(0f) }
    val rot = remember { Animatable(-25f) }
    var typed by remember { mutableStateOf("") }
    var showRest by remember { mutableStateOf(false) }
    val full = "Learn things, you usually need a PC for. For Free."

    LaunchedEffect(Unit) {
        delay(350)
        Haptics.pop(ctx) // Phone vibriert beim Aufploppen
        launch { rot.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow)) }
        scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
        delay(250)
        full.forEachIndexed { i, _ ->
            typed = full.substring(0, i + 1)
            if (i % 3 == 0) Haptics.tick(ctx)
            delay(45)
        }
        delay(300)
        showRest = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .glitchBackground()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelMascot(
            sizeDp = 200.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; rotationZ = rot.value
            }
        )
        Spacer(Modifier.height(24.dp))
        Text("Glitchy", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Pink)
        Spacer(Modifier.height(12.dp))
        Text(
            typed + if (typed.length < full.length) "▌" else "",
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            color = Neon,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        AnimatedVisibility(showRest, enter = fadeIn(tween(600)) + slideInVertically { it / 2 }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    tr("Mathe (Klasse 11–13), Programmierung & Cybersecurity – direkt auf dem Handy.",
                        "Math, programming & cybersecurity – right on your phone."),
                    textAlign = TextAlign.Center,
                    color = Lilac
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { Haptics.success(ctx); onDone() },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text(tr("Los geht's", "Let's go"), fontSize = 18.sp) }
            }
        }
    }
}
