package com.hermes.assistant

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Native implementation of the supplied Instructions screen. */
internal class VyomInstructionsDialog(
    private val activity: Activity,
    private val initialPersona: String,
    private val initialRole: String,
    private val initialRules: String,
    private val onSave: (String, String, String) -> Unit,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()

    fun show() {
        val dialog = Dialog(activity)
        var selectedPersona = initialPersona.takeIf {
            it in listOf("Hinglish", "English", "Hindi", "Formal", "Fun")
        } ?: "Hinglish"
        var selectedRole = initialRole.takeIf { it == "Jarvis" || it == "Ultron" } ?: "Jarvis"
        val personaChips = linkedMapOf<String, TextView>()
        val roleCards = linkedMapOf<String, TextView>()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), VyomDesignSpec.BACKGROUND)
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), 0x5522D3EE)
            }
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val heading = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(activity).apply {
            text = "INSTRUCTIONS"
            textSize = 21f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        heading.addView(TextView(activity).apply {
            text = "PERSONA · ROLE · CUSTOM RULES"
            textSize = 8f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            contentDescription = "Close instructions"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)

        val scroll = ScrollView(activity).apply { clipToPadding = false }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        scroll.addView(content, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun section(label: String) {
            content.addView(TextView(activity).apply {
                text = label.uppercase()
                textSize = 9f
                letterSpacing = 0.08f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(0, dp(12), 0, dp(8))
            })
        }
        fun optionBackground(accent: Int, selected: Boolean): GradientDrawable =
            GradientDrawable().apply {
                setColor(if (selected) Color.argb(42, Color.red(accent), Color.green(accent), Color.blue(accent)) else 0x33152235)
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), if (selected) accent else 0x506482AA)
            }
        fun refreshPersona() {
            personaChips.forEach { (name, chip) ->
                val selected = name == selectedPersona
                chip.background = optionBackground(VyomDesignSpec.CYAN, selected)
                chip.setTextColor(if (selected) VyomDesignSpec.CYAN else VyomDesignSpec.TEXT_SECONDARY)
            }
        }
        fun refreshRole() {
            roleCards.forEach { (name, card) ->
                val accent = if (name == "Ultron") VyomDesignSpec.AMBER else VyomDesignSpec.CYAN
                val selected = name == selectedRole
                card.background = optionBackground(accent, selected)
                card.setTextColor(if (selected) accent else VyomDesignSpec.TEXT_SECONDARY)
            }
        }

        section("Persona")
        listOf(listOf("Hinglish", "English", "Hindi"), listOf("Formal", "Fun")).forEach { rowOptions ->
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            rowOptions.forEach { name ->
                val chip = TextView(activity).apply {
                    text = name
                    textSize = 11f
                    gravity = Gravity.CENTER
                    minHeight = dp(40)
                    setPadding(dp(7), dp(8), dp(7), dp(8))
                    setOnClickListener {
                        selectedPersona = name
                        refreshPersona()
                    }
                }
                personaChips[name] = chip
                row.addView(chip, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    marginEnd = dp(6)
                })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, dp(46)))
        }
        refreshPersona()

        section("Role · changes orb colors")
        val roleRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Jarvis" to "CYAN · VIOLET", "Ultron" to "AMBER").forEach { (name, detail) ->
            val card = TextView(activity).apply {
                text = "$name\n$detail"
                textSize = 12f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(8), dp(12), dp(8), dp(12))
                minHeight = dp(68)
                setOnClickListener {
                    selectedRole = name
                    refreshRole()
                }
            }
            roleCards[name] = card
            roleRow.addView(card, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
                marginEnd = dp(8)
            })
        }
        content.addView(roleRow)
        refreshRole()

        section("Custom rules")
        val rules = EditText(activity).apply {
            hint = "Chhote jawab do. Control se pehle confirm pucho…"
            setText(initialRules)
            textSize = 13f
            minLines = 4
            maxLines = 7
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(VyomInstructionsPolicy.MAX_RULE_CHARS + 1))
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = VyomTheme.glass(activity, 16, VyomDesignSpec.CYAN)
        }
        content.addView(rules, LinearLayout.LayoutParams(-1, dp(132)))
        content.addView(TextView(activity).apply {
            text = "Max 1,000 characters. Never include passwords, OTPs, PINs or API keys. These preferences cannot override safety rules."
            textSize = 9f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(2), dp(7), dp(2), dp(4))
        })

        val save = Button(activity).apply {
            text = "SAVE"
            VyomTheme.stylePill(this, activity, "cyan", "primary")
            setOnClickListener {
                val raw = rules.text.toString()
                if (!VyomInstructionsPolicy.isValidRules(raw)) {
                    rules.error = if (raw.length > VyomInstructionsPolicy.MAX_RULE_CHARS) {
                        "Keep custom rules under 1,000 characters."
                    } else {
                        "Secrets and sensitive credentials cannot be saved here."
                    }
                    return@setOnClickListener
                }
                onSave(selectedPersona, selectedRole, VyomInstructionsPolicy.normalizeRules(raw))
                dialog.dismiss()
            }
        }
        root.addView(save, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })

        val bottom = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        }
        listOf("Home" to "home", "Chat" to "chat", "Tasks" to "tasks", "Profile" to "profile").forEach { (label, route) ->
            bottom.addView(TextView(activity).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (route == "profile") VyomDesignSpec.CYAN else VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(8), dp(9), dp(8), dp(8))
                setOnClickListener {
                    dialog.dismiss()
                    onNavigate(route)
                }
            }, LinearLayout.LayoutParams(0, dp(36), 1f))
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
}
