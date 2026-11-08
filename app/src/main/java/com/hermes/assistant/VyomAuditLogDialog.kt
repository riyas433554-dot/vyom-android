package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

/** Read-only audit viewer. All historical strings are sanitized again before display. */
internal class VyomAuditLogDialog(
    private val activity: Activity,
    private val rawEvents: String,
    private val onClear: () -> Unit,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private lateinit var dialog: Dialog
    private lateinit var search: EditText
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private var filter = "all"
    private val events: List<JSONObject> = try {
        val json = JSONArray(rawEvents)
        (0 until json.length()).mapNotNull { json.optJSONObject(it) }
    } catch (_: Exception) { emptyList() }

    fun show() {
        dialog = Dialog(activity)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), VyomDesignSpec.BACKGROUND)
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(110, Color.red(VyomDesignSpec.ROSE), Color.green(VyomDesignSpec.ROSE), Color.blue(VyomDesignSpec.ROSE)))
            }
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        title.addView(TextView(activity).apply {
            text = "AUDIT LOG"
            textSize = 21f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        title.addView(TextView(activity).apply {
            text = "LOCAL DEVICE ACTION HISTORY · READ ONLY"
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
            contentDescription = "Close audit log"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        count = TextView(activity).apply {
            textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(10), 0, dp(8))
        }
        root.addView(count)
        search = EditText(activity).apply {
            hint = "Search action history…"
            isSingleLine = true
            textSize = 13f
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = VyomTheme.glass(activity, 14, VyomDesignSpec.ROSE)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(9) })

        val filters = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }
        listOf("all", "blocked", "failed", "ui-agent", "app").forEach { key ->
            filters.addView(TextView(activity).apply {
                text = key.uppercase()
                textSize = 8f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setTextColor(if (filter == key) VyomDesignSpec.BACKGROUND else VyomDesignSpec.TEXT_SECONDARY)
                background = GradientDrawable().apply {
                    setColor(if (filter == key) VyomDesignSpec.ROSE else 0x441B2B42)
                    cornerRadius = dp(999).toFloat()
                }
                setOnClickListener {
                    filter = key
                    filters.children().forEach { child ->
                        if (child is TextView) {
                            val active = child.text.toString().equals(key, true)
                            child.setTextColor(if (active) VyomDesignSpec.BACKGROUND else VyomDesignSpec.TEXT_SECONDARY)
                            (child.background as? GradientDrawable)?.setColor(if (active) VyomDesignSpec.ROSE else 0x441B2B42)
                        }
                    }
                    render()
                }
            }, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                if (key != "app") marginEnd = dp(4)
            })
        }
        root.addView(filters)

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val clear = Button(activity).apply {
            text = "CLEAR AUDIT HISTORY"
            VyomTheme.stylePill(this, activity, "cyan", "danger")
            setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("Clear local audit history?")
                    .setMessage("This removes saved action history from this device. It does not change device permissions or task state.")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("CLEAR") { _, _ ->
                        onClear()
                        dialog.dismiss()
                    }.show()
            }
        }
        root.addView(clear, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(8) })

        val bottom = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        listOf("Home" to "home", "Chat" to "chat", "Tasks" to "tasks", "Profile" to "profile").forEach { (label, route) ->
            bottom.addView(TextView(activity).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (route == "profile") VyomDesignSpec.ROSE else VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(8), dp(10), dp(8), dp(8))
                setOnClickListener {
                    dialog.dismiss()
                    onNavigate(route)
                }
            }, LinearLayout.LayoutParams(0, dp(36), 1f))
        }
        root.addView(bottom)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        render()

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

    private fun render() {
        list.removeAllViews()
        val query = search.text.toString().trim()
        val filtered = events.filter { row ->
            val action = VyomAuditPolicy.sanitize(row.optString("action"))
            val result = VyomAuditPolicy.sanitize(row.optString("result"))
            val source = VyomAuditPolicy.sanitize(row.optString("source"))
            val categoryMatches = when (filter) {
                "blocked" -> result.contains("blocked", true) || result.contains("rejected", true)
                "failed" -> result.contains("failed", true) || result.contains("error", true)
                "ui-agent" -> source.equals("ui-agent", true)
                "app" -> source.equals("app", true)
                else -> true
            }
            categoryMatches && (query.isBlank() || "$action $result $source".contains(query, true))
        }
        count.text = "Showing ${filtered.size} of ${events.size} events · Sensitive strings are redacted"
        if (filtered.isEmpty()) {
            list.addView(TextView(activity).apply {
                text = if (events.isEmpty()) "No device actions recorded yet." else "No events match this filter."
                textSize = 13f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(14), dp(24), dp(14), dp(24))
            })
            return
        }
        filtered.forEach { row ->
            val result = VyomAuditPolicy.sanitize(row.optString("result"))
            val color = when {
                result.contains("failed", true) || result.contains("error", true) || result.contains("rejected", true) -> VyomDesignSpec.ROSE
                result.contains("blocked", true) || result.contains("awaiting", true) -> VyomDesignSpec.AMBER
                else -> VyomDesignSpec.GREEN
            }
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(11), dp(13), dp(10))
                background = GradientDrawable().apply {
                    setColor(0xAA1B2A40.toInt())
                    cornerRadius = dp(15).toFloat()
                    setStroke(dp(1), Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)))
                }
            }
            val action = VyomAuditPolicy.sanitize(row.optString("action"))
            val source = VyomAuditPolicy.sanitize(row.optString("source"))
            val time = row.optString("at").take(40).ifBlank { "TIME NOT PROVIDED" }
            card.addView(TextView(activity).apply {
                text = action
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            })
            card.addView(TextView(activity).apply {
                text = result
                textSize = 11f
                setTextColor(color)
                setPadding(0, dp(5), 0, dp(4))
            })
            card.addView(TextView(activity).apply {
                text = "$time · $source · ${row.optLong("elapsedMs").coerceAtLeast(0)} ms"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            })
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun LinearLayout.children(): List<android.view.View> =
        (0 until childCount).map { getChildAt(it) }
}
