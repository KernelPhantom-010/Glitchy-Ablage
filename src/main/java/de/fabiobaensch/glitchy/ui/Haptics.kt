package de.fabiobaensch.glitchy.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    var enabled = true

    private fun vib(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private fun run(ctx: Context, effect: VibrationEffect) {
        if (!enabled) return
        runCatching { vib(ctx)?.takeIf { it.hasVibrator() }?.vibrate(effect) }
    }

    /** Ganz leichtes Tippen (Tabs, Tipp-Animation). */
    fun tick(ctx: Context) = run(ctx, VibrationEffect.createOneShot(8, 60))

    /** Maskottchen-Pop: kurz, kräftig, kurz. */
    fun pop(ctx: Context) = run(
        ctx,
        VibrationEffect.createWaveform(longArrayOf(0, 25, 45, 60, 40, 20), intArrayOf(0, 120, 0, 255, 0, 90), -1)
    )

    fun success(ctx: Context) = run(
        ctx,
        VibrationEffect.createWaveform(longArrayOf(0, 20, 50, 35), intArrayOf(0, 110, 0, 220), -1)
    )

    fun error(ctx: Context) = run(
        ctx,
        VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 60), intArrayOf(0, 255, 0, 255), -1)
    )
}
