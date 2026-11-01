package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

/** Honest automation workspace: it displays live tasks but never invents schedules. */
internal class VyomAutomationsDialog(
    private val activity: Activity,
    private val tasks: List<JSONObject>,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private val activeStates = setOf("queued", "pending", "running", "in_progress", "processing", "working")
    private var selectedFilter = "all"
    private lateinit var list: LinearLayout
    private lateinit var countLine: TextView
    private lateinit var dialog: Dialog

    fun show() {
        dialog = Dialog(activity)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5101A2E.toInt(), VyomDesignSpec.BACKGROUND)
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(110, Color.red(VyomDesignSpec.CYAN), Color.green(VyomDesignSpec.CYAN), Color.blue(VyomDesignSpec.CYAN)))
            }
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBlock = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        titleBlock.addView(TextView(activity).apply {
            text = "AUTOMATIONS"
            textSize = 20f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        titleBlock.addView(TextView(activity).apply {
            text = "LIVE TASK QUEUE · SCHEDULES NOT CONNECTED"
            textSize = 8f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(titleBlock, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            contentDescription = "Close automations"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        val pending = tasks.count { it.optString("status", "queued").lowercase() in setOf("queued", "pending") }
        val working = tasks.count { it.optString("status", "").lowercase() in setOf("running", "in_progress", "processing", "working") }
        val done = tasks.count { it.optString("status", "").lowercase() in setOf("done", "success", "completed") }
        val stats = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, dp(12))
        }
        fun stat(label: String, value: Int, color: Int): TextView = TextView(activity).apply {
            text = "$value\n$label"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(color)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply {
                setColor(0x991B2B42.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.argb(80, Color.red(color), Color.green(color), Color.blue(color)))
            }
        }
        listOf(
            stat("PENDING", pending, VyomDesignSpec.AMBER),
            stat("WORKING", working, VyomDesignSpec.CYAN),
            stat("DONE", done, VyomDesignSpec.GREEN)
        ).forEachIndexed { index, view ->
            stats.addView(view, LinearLayout.LayoutParams(0, dp(66), 1f).apply {
                if (index < 2) marginEnd = dp(7)
            })
        }
        root.addView(stats)

        countLine = TextView(activity).apply {
            text = "Showing ${tasks.size} live task records"
            textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(2), 0, dp(8))
        }
        root.addView(countLine)

        val filters = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }
        listOf("all", "queued", "running", "done", "failed").forEach { key ->
            val chip = TextView(activity).apply {
                text = key.uppercase()
                textSize = 8f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(9), dp(8), dp(9), dp(8))
                setTextColor(if (selectedFilter == key) VyomDesignSpec.BACKGROUND else VyomDesignSpec.TEXT_SECONDARY)
                background = GradientDrawable().apply {
                    setColor(if (selectedFilter == key) VyomDesignSpec.CYAN else 0x441B2B42)
                    cornerRadius = dp(999).toFloat()
                }
                setOnClickListener {
                    selectedFilter = key
                    filters.children().forEach { child ->
                        if (child is TextView) {
                            val active = child.text.toString().equals(key, true)
                            child.setTextColor(if (active) VyomDesignSpec.BACKGROUND else VyomDesignSpec.TEXT_SECONDARY)
                            (child.background as? GradientDrawable)?.setColor(if (active) VyomDesignSpec.CYAN else 0x441B2B42)
                        }
                    }
                    renderTasks()
                }
            }
            filters.addView(chip, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                if (key != "failed") marginEnd = dp(4)
            })
        }
        root.addView(filters)

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        renderTasks()

        val newAutomation = Button(activity).apply {
            text = "＋  NEW AUTOMATION"
            VyomTheme.stylePill(this, activity, "cyan", "primary")
            setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("Scheduled automations aren't connected yet")
                    .setMessage("This build can submit immediate tasks, but it cannot save time-based or repeating schedules. Nothing has been scheduled. Use Tasks to start a task now.")
                    .setNegativeButton("CLOSE", null)
                    .setPositiveButton("OPEN TASKS") { _, _ ->
                        dialog.dismiss()
                        onNavigate("tasks")
                    }.show()
            }
        }
        root.addView(newAutomation, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(10)
        })

        val bottom = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        listOf("Home" to "home", "Chat" to "chat", "Tasks" to "tasks", "Profile" to "profile").forEach { (label, route) ->
            bottom.addView(TextView(activity).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (route == "tasks") VyomDesignSpec.CYAN else VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(8), dp(10), dp(8), dp(8))
                setOnClickListener {
                    dialog.dismiss()
                    onNavigate(route)
                }
            }, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
        root.addView(bottom)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.72f)
        }
        dialog.show()
        dialog.window?.setLayout(
            activity.resources.displayMetrics.widthPixels,
            activity.resources.displayMetrics.heightPixels
        )
    }

    private fun renderTasks() {
        list.removeAllViews()
        val filtered = tasks.filter { task ->
            val state = task.optString("status", "queued").lowercase()
            when (selectedFilter) {
                "queued" -> state in setOf("queued", "pending")
                "running" -> state in setOf("running", "in_progress", "processing", "working")
                "done" -> state in setOf("done", "success", "completed")
                "failed" -> state in setOf("failed", "error")
                else -> true
            }
        }.asReversed()
        countLine.text = "Showing ${filtered.size} of ${tasks.size} live task records"
        if (filtered.isEmpty()) {
            list.addView(TextView(activity).apply {
                text = if (tasks.isEmpty()) "No live tasks yet. Create an immediate task from Tasks." else "No tasks in this filter."
                textSize = 13f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(14), dp(28), dp(14), dp(28))
            })
            return
        }
        filtered.forEach { task ->
            val state = task.optString("status", "queued").lowercase()
            val accent = when {
                state in setOf("failed", "error") -> VyomDesignSpec.ROSE
                state in setOf("done", "success", "completed") -> VyomDesignSpec.GREEN
                state in activeStates && state !in setOf("queued", "pending") -> VyomDesignSpec.CYAN
                else -> VyomDesignSpec.AMBER
            }
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(0xAA17263C.toInt())
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.argb(75, Color.red(accent), Color.green(accent), Color.blue(accent)))
                }
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(activity).apply {
                text = "TASK #${task.optString("id", "—").takeLast(6)}"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(activity).apply {
                text = state.uppercase()
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(accent)
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = GradientDrawable().apply {
                    setColor(Color.argb(25, Color.red(accent), Color.green(accent), Color.blue(accent)))
                    cornerRadius = dp(999).toFloat()
                }
            })
            card.addView(row)
            card.addView(TextView(activity).apply {
                text = task.optString("prompt", "Task details unavailable")
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
                setPadding(0, dp(8), 0, dp(5))
                maxLines = 3
            })
            val phase = task.optString("phase").takeIf { it.isNotBlank() && it != "null" }
            if (phase != null) card.addView(TextView(activity).apply {
                text = "Phase · $phase"
                textSize = 11f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            })
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
    }

    private fun LinearLayout.children(): List<android.view.View> =
        (0 until childCount).map { getChildAt(it) }
}
