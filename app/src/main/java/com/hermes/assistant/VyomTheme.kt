package com.hermes.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

/**
 * Vyom premium design tokens: Obsidian surfaces, Platinum typography and restrained Indigo/Champagne accents.
 */
internal object VyomTheme {
    const val BACKGROUND: Int = 0xFF090B10.toInt()
    const val SURFACE_GLASS: Int = 0xF2171A22.toInt()
    const val SURFACE_GLASS_SOFT: Int = 0xD91B1E27.toInt()
    const val SURFACE_USER_BUBBLE: Int = 0xFF25283A.toInt()
    const val SURFACE_ASSISTANT_BUBBLE: Int = 0xFF171A22.toInt()
    const val SURFACE_TASK_CARD: Int = 0xFF1C202A.toInt()
    const val TEXT_PRIMARY: Int = 0xFFE8E8ED.toInt()
    const val TEXT_SECONDARY: Int = 0xFF9296A3.toInt()
    const val ACCENT_INDIGO: Int = 0xFF7775E8.toInt()
    @Deprecated("Use ACCENT_INDIGO")
    const val ACCENT_CYAN: Int = ACCENT_INDIGO
    const val ACCENT_VIOLET: Int = 0xFF9997F0.toInt()
    const val ACCENT_AMBER: Int = 0xFFC6AD82.toInt()
    const val ACCENT_GREEN: Int = 0xFF55B995.toInt()
    const val ACCENT_DANGER: Int = 0xFFD96C7A.toInt()
    const val BORDER_SUBTLE: Int = 0x3DFFFFFF
    const val BORDER_GLASS: Int = 0x337775E8
    const val RADIUS_SMALL_DP = 12
    const val RADIUS_LARGE_DP = 18
    const val RADIUS_CHAT_DP = 16
    const val SPACE_XS_DP = 4
    const val SPACE_SM_DP = 8
    const val SPACE_MD_DP = 12
    const val SPACE_LG_DP = 16
    const val TYPE_SMALL_SP = 12
    const val TYPE_BODY_SP = 16
    const val TYPE_TITLE_SP = 24
    const val TYPE_HERO_SP = 32
    const val GLASS_BLUR_MIN_DP = 20
    const val GLASS_BLUR_MAX_DP = 40

    fun canonicalAccentTheme(theme: String?): String = when (theme?.trim()?.lowercase()) {
        "indigo", "cyan", null, "" -> "indigo"
        "amber" -> "amber"
        "violet" -> "violet"
        "green" -> "green"
        "danger", "red" -> "danger"
        else -> "indigo"
    }

    fun accent(context: Context, theme: String = "indigo"): Int = when (canonicalAccentTheme(theme)) {
        "amber" -> ACCENT_AMBER
        "violet" -> ACCENT_VIOLET
        "green" -> ACCENT_GREEN
        "danger" -> ACCENT_DANGER
        else -> ACCENT_INDIGO
    }

    fun panel(context: Context, theme: String = "indigo"): GradientDrawable {
        val color = accent(context, theme)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(SURFACE_TASK_CARD, SURFACE_ASSISTANT_BUBBLE)
        ).apply {
            cornerRadius = RADIUS_LARGE_DP * context.resources.displayMetrics.density
            setStroke((context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                Color.argb(58, Color.red(color), Color.green(color), Color.blue(color)))
        }
    }

    fun glass(context: Context, radiusDp: Int = RADIUS_LARGE_DP, accent: Int = ACCENT_INDIGO): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(SURFACE_GLASS)
            cornerRadius = radiusDp * context.resources.displayMetrics.density
            setStroke((context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                Color.argb(42, Color.red(accent), Color.green(accent), Color.blue(accent)))
        }

    fun stylePill(button: Button, context: Context, theme: String = "indigo", variant: String = "primary") {
        val color = when (variant.lowercase()) {
            "danger" -> ACCENT_DANGER
            "secondary" -> accent(context, theme)
            "amber" -> ACCENT_AMBER
            "violet" -> ACCENT_VIOLET
            "green" -> ACCENT_GREEN
            else -> accent(context, theme)
        }
        val density = context.resources.displayMetrics.density
        button.background = when (variant.lowercase()) {
            "secondary" -> GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(22, Color.red(color), Color.green(color), Color.blue(color)))
                cornerRadius = 999f * density
                setStroke((density).toInt().coerceAtLeast(1),
                    Color.argb(92, Color.red(color), Color.green(color), Color.blue(color)))
            }
            "danger" -> GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ACCENT_DANGER, ACCENT_DANGER)).apply {
                cornerRadius = 999f * density
            }
            else -> {
                GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = 999f * density
                    setStroke((density).toInt().coerceAtLeast(1), color)
                }
            }
        }
        button.setTextColor(if (variant.lowercase() == "secondary") color else Color.WHITE)
        button.isAllCaps = false
        button.textSize = 14f
        button.minHeight = (44f * density).toInt()
        button.setPadding((16f * density).toInt(), 0, (16f * density).toInt(), 0)
        button.elevation = if (variant.lowercase() == "secondary") 0f else 3f * density
    }
}
