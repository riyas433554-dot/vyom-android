package com.hermes.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

/** Shared visual tokens for the Vyom native UI. */
internal object VyomTheme {
    const val BACKGROUND: Int = 0xFF0B0F1A.toInt()
    const val RADIUS_SMALL_DP = 14
    const val RADIUS_LARGE_DP = 20
    const val RADIUS_CHAT_DP = 16
    const val SPACE_XS_DP = 4
    const val SPACE_SM_DP = 8
    const val SPACE_MD_DP = 12
    const val SPACE_LG_DP = 16
    const val SURFACE_USER_BUBBLE: Int = 0xFF1F3752.toInt()
    const val SURFACE_ASSISTANT_BUBBLE: Int = 0xFF111927.toInt()
    const val SURFACE_TASK_CARD: Int = 0xFF161D28.toInt()
    const val TEXT_PRIMARY: Int = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY: Int = 0xFFBFC8D8.toInt()
    const val BORDER_SUBTLE: Int = 0x506482AA
    const val TYPE_SMALL_SP = 12
    const val TYPE_BODY_SP = 16
    const val TYPE_TITLE_SP = 24
    const val TYPE_HERO_SP = 32
    const val GLASS_BLUR_MIN_DP = 20
    const val GLASS_BLUR_MAX_DP = 40

    fun stylePill(button: Button, context: Context, theme: String = "cyan") {
        val end = accent(context, theme)
        val start = Color.rgb((Color.red(end) * 0.55f).toInt(), (Color.green(end) * 0.55f).toInt(), (Color.blue(end) * 0.55f).toInt())
        button.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end)).apply {
            cornerRadius = 28f * context.resources.displayMetrics.density
            setStroke((1f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1), end)
        }
        button.setTextColor(Color.WHITE)
        button.isAllCaps = false
    }

    fun accent(context: Context, theme: String = "cyan"): Int = when (theme.lowercase()) {
        "amber" -> Color.rgb(255, 183, 77)
        "violet" -> Color.rgb(167, 139, 250)
        "green" -> Color.rgb(52, 211, 153)
        else -> Color.rgb(56, 189, 248)
    }
}
