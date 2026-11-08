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
import org.json.JSONArray
import org.json.JSONObject

/** Task details render only fields actually returned by the task API. */
internal class VyomTaskDetailDialog(
    private val activity: Activity,
    private val task: JSONObject,
    private val onCancel: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()

    fun show() {
        val id = task.optString("id", "—")
        val status = task.optString("status", "unknown").lowercase()
        val accent = VyomDesignSpec.taskStatusAccent(status)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), VyomDesignSpec.BACKGROUND)
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent)))
            }
        }
        val dialog = Dialog(activity)

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        title.addView(TextView(activity).apply {
            text = "TASK #$id"
            textSize = 20f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        title.addView(TextView(activity).apply {
            text = "UPDATED · ${task.optString("updatedAt", task.optString("updated_at", "TIME NOT PROVIDED"))}"
            textSize = 8f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            contentDescription = "Close task details"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(8))
        }
        scroll.addView(body, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun section(label: String) {
            body.addView(TextView(activity).apply {
                text = label.uppercase()
                textSize = 9f
                letterSpacing = 0.12f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(0, dp(13), 0, dp(6))
            })
        }
        fun card(content: String, color: Int = VyomDesignSpec.TEXT_PRIMARY) {
            body.addView(TextView(activity).apply {
                text = content
                textSize = 13f
                setTextColor(color)
                setTextIsSelectable(true)
                setPadding(dp(13), dp(11), dp(13), dp(11))
                background = GradientDrawable().apply {
                    setColor(0xAA1B2A40.toInt())
                    cornerRadius = dp(14).toFloat()
                }
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }

        section("Status")
        card(status.uppercase(), accent)
        section("Task")
        card(task.optString("prompt", "Task description unavailable"))

        task.optString("phase").takeIf { it.isNotBlank() && it != "null" }?.let {
            section("Current phase")
            card(it, VyomDesignSpec.CYAN)
        }

        val agents = task.optJSONArray("agents")
        if (agents != null && agents.length() > 0) {
            section("Agents · ${agents.length()}")
            for (i in 0 until agents.length()) {
                val agent = agents.optJSONObject(i) ?: continue
                card("${agent.optString("name", "Agent ${i + 1}")} · ${agent.optString("status", "status unavailable")}")
            }
        }

        val steps = task.optJSONArray("steps")
        if (steps != null && steps.length() > 0) {
            section("Steps · ${steps.length()}")
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i)
                val value = if (step == null) steps.optString(i) else
                    step.optString("title", step.optString("name", "Step ${i + 1}"))
                val stepStatus = step?.optString("status", "").orEmpty()
                card(if (stepStatus.isBlank()) value else "$value · ${stepStatus.uppercase()}")
            }
        }

        val logs = task.optJSONArray("logs")
        if (logs != null && logs.length() > 0) {
            section("Logs · ${logs.length()}")
            for (i in 0 until logs.length()) {
                val entry = logs.opt(i)
                val value = when (entry) {
                    is JSONObject -> entry.optString("message", entry.optString("text", entry.toString()))
                    is JSONArray -> entry.toString()
                    else -> entry?.toString().orEmpty()
                }
                if (value.isNotBlank()) card(value, VyomDesignSpec.TEXT_SECONDARY)
            }
        }

        val resultValue = task.opt("result")
        val resultText = when (resultValue) {
            null, JSONObject.NULL -> ""
            is JSONObject -> resultValue.toString(2)
            else -> resultValue.toString()
        }
        if (resultText.isNotBlank()) {
            section("Result")
            card(resultText)
        }
        val errorCode = task.optString("errorCode", task.optString("error_code", ""))
        if (errorCode.isNotBlank() && errorCode != "null") {
            section("Error")
            card(errorCode, VyomDesignSpec.ROSE)
        }

        if (status in setOf("queued", "pending", "running", "in_progress", "processing", "working")) {
            val cancel = Button(activity).apply {
                text = "CANCEL TASK"
                VyomTheme.stylePill(this, activity, "cyan", "danger")
                setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle("Cancel task #$id?")
                        .setMessage("Vyom will ask the task service to cancel this task.")
                        .setNegativeButton("KEEP TASK", null)
                        .setPositiveButton("CANCEL TASK") { _, _ ->
                            dialog.dismiss()
                            onCancel(id)
                        }.show()
                }
            }
            root.addView(cancel, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        }

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
}
