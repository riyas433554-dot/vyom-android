package com.hermes.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Orbit cards show compact snapshots from the authenticated Vyom task API.
 * The Tasks panel remains the canonical detailed task view.
 */
internal class OrbitHomeView(context: Context) : View(context) {
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(27, 61, 102)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(dp(4f), dp(5f)), 0f)
    }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(224, 9, 16, 30) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(207, 232, 255)
        textSize = dp(11f)
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private var rotation = 0f
    private var downX = 0f
    private var dragging = false
    private var tasks: List<Pair<String, String>> = emptyList()
    private var accentColor: Int = Color.rgb(56, 189, 248)
    private var liteMode: Boolean = false
    private val colors = intArrayOf(
        Color.rgb(56, 189, 248), Color.rgb(167, 139, 250), Color.rgb(251, 191, 36),
        Color.rgb(244, 114, 182), Color.rgb(52, 211, 153)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.40f
        val rx = min(width * 0.43f, dp(150f))
        val ry = min(height * 0.29f, dp(235f))
        canvas.save()
        orbitPaint.color = if (liteMode) Color.rgb(Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.rgb(27, 61, 102)
        canvas.rotate(-18f, cx, cy)
        canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), orbitPaint)
        canvas.restore()
        val visibleTasks = tasks.take(5)
        if (visibleTasks.isEmpty()) {
            textPaint.color = Color.rgb(207, 232, 255)
            textPaint.textSize = dp(12f)
            canvas.drawText("TASKS", cx - dp(20f), cy - dp(6f), textPaint)
            textPaint.textSize = dp(10f)
            textPaint.color = Color.LTGRAY
            canvas.drawText("No saved tasks", cx - dp(35f), cy + dp(14f), textPaint)
            return
        }
        for (i in visibleTasks.indices) {
            val angle = Math.toRadians((i * 72f + rotation - 90f).toDouble())
            val x = cx + cos(angle).toFloat() * rx * 0.82f
            val y = cy + sin(angle).toFloat() * ry * 0.82f
            val w = dp(112f)
            val h = dp(62f)
            val rect = RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
            cardPaint.alpha = if (liteMode) 245 else if (sin(angle) > 0.6) 230 else 155
            canvas.drawRoundRect(rect, dp(5f), dp(5f), cardPaint)
            val state = visibleTasks[i].second.lowercase()
            val cardColor = when (state) {
                "failed", "error" -> Color.rgb(248, 113, 113)
                "running", "in_progress" -> Color.rgb(56, 189, 248)
                "success", "completed", "done" -> Color.rgb(52, 211, 153)
                else -> Color.rgb(251, 191, 36)
            }
            borderPaint.color = cardColor
            borderPaint.alpha = if (sin(angle) > 0.6) 235 else 150
            canvas.drawRoundRect(rect, dp(5f), dp(5f), borderPaint)
            textPaint.color = cardColor
            textPaint.textSize = dp(10f)
            canvas.drawText(state.uppercase().take(12), rect.left + dp(8f), rect.top + dp(16f), textPaint)
            textPaint.color = Color.rgb(207, 232, 255)
            textPaint.textSize = dp(10f)
            val title = visibleTasks[i].first
            canvas.drawText(if (title.length > 15) title.take(14) + "…" else title, rect.left + dp(8f), rect.top + dp(36f), textPaint)
            canvas.drawRoundRect(RectF(rect.left + dp(8f), rect.bottom - dp(9f), rect.right - dp(8f), rect.bottom - dp(6f)), dp(2f), dp(2f), borderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                dragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                rotation += (event.x - downX) * 0.35f
                downX = event.x
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return true
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        invalidate()
    }

    fun setLiteMode(enabled: Boolean) {
        liteMode = enabled
        invalidate()
    }

    fun setTaskSnapshots(items: List<Pair<String, String>>) {
        tasks = items
        invalidate()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
