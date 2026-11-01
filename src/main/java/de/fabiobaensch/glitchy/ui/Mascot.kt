package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Das Maskottchen "Glitch": ein pixeliges, kapuzentragendes Wesen mit Antenne
 * und Neon-Augen (kein Mensch). Schwebt leicht und blinzelt.
 */
private val GRID = listOf(
    "......a.....",
    "......s.....",
    "...hhhhhh...",
    "..hhhhhhhh..",
    "..hvvvvvvh..",
    "..hvEvvEvh..",
    "..hvvvvvvh..",
    "..hhvmmvhh..",
    "..hhhhhhhh..",
    ".hhbbhhbbhh.",
    ".hhbhhhhbhh.",
    "..hh....hh..",
    "..ll....ll.."
)

private fun colorOf(c: Char): Color? = when (c) {
    'h' -> Violet
    'v' -> Color(0xFF0B0618)
    'E' -> Neon
    'm' -> Pink
    'a' -> Neon
    's' -> Pink
    'b' -> Color(0xFF9F67FF)
    'l' -> Color(0xFF5B21B6)
    else -> null
}

@Composable
fun PixelMascot(sizeDp: Dp = 140.dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "mascot")
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val blink by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(keyframes {
            durationMillis = 3200
            0f at 0
            0f at 2900
            1f at 3000
            0f at 3100
        }),
        label = "blink"
    )

    Canvas(modifier.size(sizeDp)) {
        val cols = GRID[0].length
        val rows = GRID.size
        val cell = minOf(size.width / cols, size.height / rows)
        val offX = (size.width - cell * cols) / 2f
        val offY = (size.height - cell * rows) / 2f - bob * cell * 0.7f
        GRID.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val c = if (ch == 'E' && blink > 0.5f) colorOf('v') else colorOf(ch)
                if (c != null) {
                    drawRect(c, Offset(offX + x * cell, offY + y * cell), Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}
