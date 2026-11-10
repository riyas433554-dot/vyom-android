package com.hermes.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

/**
 * Vyom Design System v2 tokens.
 * Palette and component geometry follow the supplied "Vyom — Design System v2 + screens" reference.
 */
internal object VyomTheme {
    const val BACKGROUND: Int = 0xFF070B14.toInt()
    const val SURFACE_GLASS: Int = 0xE6101A2E.toInt()
    const val SURFACE_GLASS_SOFT: Int = 0xB80F1A2E.toInt()
    const val SURFACE_USER_BUBBLE: Int = 0xFF142C43.toInt()
    const val SURFACE_ASSISTANT_BUBBLE: Int = 0xE60F1A2E.toInt()
    const val SURFACE_TASK_CARD: Int = 0xC7182846.toInt()
    const val TEXT_PRIMARY: Int = 0xFFE6F1FF.toInt()
    const val TEXT_SECONDARY: Int = 0xFF8FA3BF.toInt()
    const val ACCENT_CYAN: Int = 0xFF22D3EE.toInt()
    const val ACCENT_VIOLET: Int = 0xFF8B5CF6.toInt()
    const val ACCENT_AMBER: Int = 0xFFFBBF24.toInt()
    const val ACCENT_GREEN: Int = 0xFF34D399.toInt()
    const val ACCENT_DANGER: Int = 0xFFF43F5E.toInt()
    const val BORDER_SUBTLE: Int = 0x506482AA
    const val BORDER_GLASS: Int = 0x4722D3EE
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

    fun accent(context: Context, theme: String = "cyan"): Int = when (theme.lowercase()) {
        "amber" -> ACCENT_AMBER
        "violet" -> ACCENT_VIOLET
        "green" -> ACCENT_GREEN
        "danger", "red" -> ACCENT_DANGER
        else -> ACCENT_CYAN
    }

    fun panel(context: Context, theme: String = "cyan"): GradientDrawable {
        val color = accent(context, theme)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(24, 40, 70), Color.rgb(15, 26, 46))
        ).apply {
            cornerRadius = RADIUS_LARGE_DP * context.resources.displayMetrics.density
            setStroke((context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                Color.argb(105, Color.red(color), Color.green(color), Color.blue(color)))
        }
    }

    fun glass(context: Context, radiusDp: Int = RADIUS_LARGE_DP, accent: Int = ACCENT_CYAN): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(SURFACE_GLASS)
            cornerRadius = radiusDp * context.resources.displayMetrics.density
            setStroke((context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent)))
        }

    fun stylePill(button: Button, context: Context, theme: String = "cyan", variant: String = "primary") {
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
                setColor(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)))
                cornerRadius = 999f * density
                setStroke((density).toInt().coerceAtLeast(1),
                    Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)))
            }
            "danger" -> GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFF95C75.toInt(), ACCENT_DANGER)).apply {
                cornerRadius = 999f * density
            }
            else -> {
                val start = Color.rgb((Color.red(color) * 0.55f).toInt(),
                    (Color.green(color) * 0.55f).toInt(), (Color.blue(color) * 0.55f).toInt())
                GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, color)).apply {
                    cornerRadius = 999f * density
                    setStroke((density).toInt().coerceAtLeast(1), color)
                }
            }
        }
        button.setTextColor(if (variant.lowercase() == "secondary") color else Color.rgb(4, 20, 30))
        button.isAllCaps = false
        button.textSize = 14f
        button.minHeight = (44f * density).toInt()
        button.setPadding((16f * density).toInt(), 0, (16f * density).toInt(), 0)
        button.elevation = if (variant.lowercase() == "secondary") 0f else 3f * density
    }
}
