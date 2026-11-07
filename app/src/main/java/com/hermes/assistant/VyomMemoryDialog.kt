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

/** Searchable, editable memory viewer. The encrypted store remains the source of truth. */
internal class VyomMemoryDialog(
    private val activity: Activity,
    private val store: VyomMemoryStore,
    private val onChanged: (String) -> Unit,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private lateinit var dialog: Dialog
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var addButton: Button

    fun show() {
        dialog = Dialog(activity)
        val accent = VyomDesignSpec.AMBER
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

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBlock = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        titleBlock.addView(TextView(activity).apply {
            text = "MEMORY"
            textSize = 21f
            letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        titleBlock.addView(TextView(activity).apply {
            text = "ENCRYPTED ON DEVICE · RELEVANT FACTS MAY BE SENT TO AI"
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
            contentDescription = "Close memory"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        status = TextView(activity).apply {
            textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(10), 0, dp(8))
            text = "Selected safe facts are stored locally. Relevant facts may be included in prompts sent to your connected AI model."
        }
        root.addView(status)

        toggle = Button(activity).apply {
            text = if (store.isEnabled()) "MEMORY: ON · TURN OFF" else "MEMORY: OFF · TURN ON"
            VyomTheme.stylePill(this, activity, "amber", "secondary")
            setOnClickListener {
                store.setEnabled(!store.isEnabled())
                refresh()
                onChanged(if (store.isEnabled()) "enabled" else "disabled")
            }
        }
        root.addView(toggle, LinearLayout.LayoutParams(-1, dp(44)))

        search = EditText(activity).apply {
            hint = "Search memory…"
            isSingleLine = true
            textSize = 13f
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = VyomTheme.glass(activity, 14, accent)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(12)
            bottomMargin = dp(8)
        })

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        addButton = Button(activity).apply {
            text = "＋ ADD FACT"
            VyomTheme.stylePill(this, activity, "amber", "secondary")
            setOnClickListener { if (store.isEnabled()) editFact(null) }
        }
        val clear = Button(activity).apply {
            text = "DELETE ALL"
            VyomTheme.stylePill(this, activity, "cyan", "danger")
            setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("Delete all saved memories?")
                    .setMessage("This clears the encrypted facts stored on this device. It cannot be undone.")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("DELETE ALL") { _, _ ->
                        store.clearFacts()
                        refresh()
                        onChanged("cleared")
                    }.show()
            }
        }
        actions.addView(addButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) })
        actions.addView(clear, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(actions)

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
                setTextColor(if (route == "profile") VyomDesignSpec.AMBER else VyomDesignSpec.TEXT_SECONDARY)
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
        refresh()

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

    private fun refresh() {
        toggle.text = if (store.isEnabled()) "MEMORY: ON · TURN OFF" else "MEMORY: OFF · TURN ON"
        search.isEnabled = store.isEnabled()
        addButton.isEnabled = store.isEnabled()
        render(search.text.toString())
    }

    private fun render(query: String) {
        list.removeAllViews()
        val enabled = store.isEnabled()
        val facts = store.facts()
        val filtered = facts.mapIndexed { index, fact -> index to fact }
            .filter { query.isBlank() || it.second.contains(query.trim(), true) }
        status.text = if (enabled) {
            "${facts.size} / 30 safe facts saved · Encrypted locally; relevant facts may be sent with prompts"
        } else {
            "Memory is OFF. New facts will not be captured or used in prompts."
        }
        if (!enabled) {
            list.addView(TextView(activity).apply {
                text = "Memory is paused. Turn it on to review or edit saved facts."
                textSize = 13f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(14), dp(24), dp(14), dp(24))
            })
            return
        }
        if (filtered.isEmpty()) {
            list.addView(TextView(activity).apply {
                text = if (facts.isEmpty()) "No saved memories yet. You can add a safe fact or ask Vyom to remember a preference." else "No memories match this search."
                textSize = 13f
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(14), dp(24), dp(14), dp(24))
            })
            return
        }
        filtered.forEach { (index, fact) ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(11), dp(13), dp(10))
                background = GradientDrawable().apply {
                    setColor(0xAA1B2A40.toInt())
                    cornerRadius = dp(15).toFloat()
                    setStroke(dp(1), Color.argb(70, Color.red(VyomDesignSpec.AMBER), Color.green(VyomDesignSpec.AMBER), Color.blue(VyomDesignSpec.AMBER)))
                }
            }
            card.addView(TextView(activity).apply {
                text = fact
                textSize = 14f
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
                setTextIsSelectable(true)
            })
            val buttons = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(8), 0, 0)
            }
            val edit = TextView(activity).apply {
                text = "EDIT"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.CYAN)
                setPadding(dp(12), dp(7), dp(12), dp(7))
                setOnClickListener { editFact(index to fact) }
            }
            val delete = TextView(activity).apply {
                text = "DELETE"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.ROSE)
                setPadding(dp(12), dp(7), dp(4), dp(7))
                setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle("Delete this memory?")
                        .setMessage("This saved fact will be removed from this device.")
                        .setNegativeButton("CANCEL", null)
                        .setPositiveButton("DELETE") { _, _ ->
                            val next = store.facts().toMutableList()
                            if (index in next.indices) next.removeAt(index)
                            store.replaceFacts(next.joinToString("\n"))
                            render(search.text.toString())
                            onChanged("deleted")
                        }.show()
                }
            }
            buttons.addView(edit)
            buttons.addView(delete)
            card.addView(buttons)
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun editFact(existing: Pair<Int, String>?) {
        if (!store.isEnabled()) return
        val input = EditText(activity).apply {
            hint = "A safe preference or project fact"
            setText(existing?.second.orEmpty())
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            minLines = 2
            maxLines = 5
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = VyomTheme.glass(activity, 12, VyomDesignSpec.AMBER)
        }
        AlertDialog.Builder(activity)
            .setTitle(if (existing == null) "Add memory" else "Edit memory")
            .setMessage("Do not add passwords, OTPs, tokens, payment details, health details or identifiers.")
            .setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val value = input.text.toString().trim()
                val facts = store.facts().toMutableList()
                if (existing == null) facts.add(value) else if (existing.first in facts.indices) facts[existing.first] = value
                store.replaceFacts(facts.joinToString("\n"))
                render(search.text.toString())
                onChanged(if (existing == null) "added" else "edited")
            }.show()
    }
}
