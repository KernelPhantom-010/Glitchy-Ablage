package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

val Ink = Color(0xFF07030F)
val Deep = Color(0xFF160A33)
val Violet = Color(0xFF7C3AED)
val Lilac = Color(0xFFB388FF)
val Pink = Color(0xFFE879F9)
val Neon = Color(0xFF39FF14)
val CardBg = Color(0xFF1A0F33)
val Danger = Color(0xFFFF5C7A)

private val Scheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Pink,
    background = Ink,
    onBackground = Color(0xFFEDE7FF),
    surface = CardBg,
    onSurface = Color(0xFFEDE7FF),
    surfaceVariant = Color(0xFF241447),
    onSurfaceVariant = Color(0xFFC9BDE8),
    outline = Color(0xFF4B3A7A)
)

@Composable
fun GlitchyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

fun isDe() = Locale.getDefault().language == "de"
fun tr(de: String, en: String) = if (isDe()) de else en

/** Verlauf von dunklem Lila nach Schwarz. */
fun Modifier.glitchBackground() = this.background(Brush.verticalGradient(listOf(Deep, Ink)))

@Composable
fun GlowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Violet, Pink.copy(alpha = 0.5f))))
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun GradientBar(percent: Int, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(percent.coerceIn(0, 100) / 100f, tween(900), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2A1A52))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Violet, Pink)))
        )
    }
}

@Composable
fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .glitchBackground()
            .statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}
