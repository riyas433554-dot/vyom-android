package com.hermes.assistant

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Searchable local skill catalogue. Only implemented actions are shown as enabled skills. */
internal class VyomSkillsDialog(
    private val activity: Activity,
    private val onChanged: (String, Boolean) -> Unit,
    private val onTeach: () -> Unit,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private val prefs = activity.getSharedPreferences("vyom_skill_registry", Activity.MODE_PRIVATE)
    private lateinit var dialog: Dialog
    private lateinit var search: EditText
    private lateinit var list: LinearLayout
    private lateinit var count: TextView

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
                setStroke(dp(1), Color.argb(110, Color.red(VyomDesignSpec.CYAN), Color.green(VyomDesignSpec.CYAN), Color.blue(VyomDesignSpec.CYAN)))
            }
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        title.addView(TextView(activity).apply {
            text = "SKILLS"
            textSize = 21f
            letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        title.addView(TextView(activity).apply {
            text = "LOCAL DEVICE CAPABILITIES"
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
            contentDescription = "Close skills"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        count = TextView(activity).apply {
            textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(12), 0, dp(8))
        }
        root.addView(count)

        search = EditText(activity).apply {
            hint = "Search skills…"
            isSingleLine = true
            textSize = 13f
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = VyomTheme.glass(activity, 14, VyomDesignSpec.CYAN)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply {
            bottomMargin = dp(10)
        })

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val teach = android.widget.Button(activity).apply {
            text = "TEACH LIBRARY"
            VyomTheme.stylePill(this, activity, "cyan", "primary")
            setOnClickListener {
                dialog.dismiss()
                onTeach()
            }
        }
        root.addView(teach, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })

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
                setTextColor(if (route == "profile") VyomDesignSpec.CYAN else VyomDesignSpec.TEXT_SECONDARY)
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
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        render("")

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

    private fun render(query: String) {
        list.removeAllViews()
        val items = VyomDesignSpec.deviceSkills.filter {
            query.isBlank() || it.label.contains(query.trim(), true) || it.description.contains(query.trim(), true)
        }
        val enabledCount = VyomDesignSpec.deviceSkills.count { prefs.getBoolean(it.id, true) }
        count.text = "$enabledCount / ${VyomDesignSpec.deviceSkills.size} skills enabled · Changes apply on this device"
        if (items.isEmpty()) {
            list.addView(TextView(activity).apply {
                text = "No matching skills"
                textSize = 13f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(14), dp(24), dp(14), dp(24))
            })
            return
        }
        items.forEach { item ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(0xAA1B2A40.toInt())
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.argb(65, Color.red(VyomDesignSpec.CYAN), Color.green(VyomDesignSpec.CYAN), Color.blue(VyomDesignSpec.CYAN)))
                }
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(activity).apply {
                text = item.label
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(activity).apply {
                text = item.risk
                textSize = 8f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.GREEN)
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = GradientDrawable().apply {
                    setColor(0x2234D399)
                    cornerRadius = dp(999).toFloat()
                    setStroke(dp(1), 0x5534D399)
                }
            })
            card.addView(row)
            card.addView(TextView(activity).apply {
                text = item.description
                textSize = 11f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(0, dp(5), 0, dp(4))
            })
            val toggleRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            toggleRow.addView(TextView(activity).apply {
                text = "Enable this skill"
                textSize = 11f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            toggleRow.addView(Switch(activity).apply {
                isChecked = prefs.getBoolean(item.id, true)
                contentDescription = "Enable ${item.label}"
                setOnCheckedChangeListener { _, enabled ->
                    prefs.edit().putBoolean(item.id, enabled).apply()
                    onChanged(item.label, enabled)
                    count.text = "${VyomDesignSpec.deviceSkills.count { prefs.getBoolean(it.id, true) }} / ${VyomDesignSpec.deviceSkills.size} skills enabled · Changes apply on this device"
                }
            })
            card.addView(toggleRow)
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        list.addView(TextView(activity).apply {
            text = "Only capabilities with an implemented local adapter are listed. UI automation is confirmation-gated and works only in apps you explicitly allow; secure system surfaces and protected actions remain unavailable."
            textSize = 10f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(4), dp(8), dp(4), dp(12))
        })
    }
}
