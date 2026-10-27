package com.hermes.assistant

import android.content.Context
import android.graphics.Color

/** Shared visual tokens for the Vyom native UI. */
internal object VyomTheme {
    const val BACKGROUND: Int = 0xFF0B0F1A.toInt()
    const val RADIUS_SMALL_DP = 14
    const val RADIUS_LARGE_DP = 20
    const val TYPE_SMALL_SP = 12
    const val TYPE_BODY_SP = 16
    const val TYPE_TITLE_SP = 24
    const val TYPE_HERO_SP = 32
    const val GLASS_BLUR_MIN_DP = 20
    const val GLASS_BLUR_MAX_DP = 40

    fun accent(context: Context, theme: String = "cyan"): Int = when (theme.lowercase()) {
        "amber" -> Color.rgb(255, 183, 77)
        "violet" -> Color.rgb(167, 139, 250)
        "green" -> Color.rgb(52, 211, 153)
        else -> Color.rgb(56, 189, 248)
    }
}
