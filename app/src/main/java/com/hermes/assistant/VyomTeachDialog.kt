package com.hermes.assistant

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** HUD-style Teach Vyom sheet. Classification, persistence and execution stay in MainActivity. */
internal class VyomTeachDialog(
    private val activity: Activity,
    private val originalPhrase: String,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()

    fun show() {
        val dialog = Dialog(activity)
        val accent = VyomDesignSpec.CYAN
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), VyomDesignSpec.BACKGROUND)
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), 0x7722D3EE)
            }
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        title.addView(TextView(activity).apply {
            text = "TEACH VYOM"
            textSize = 20f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        title.addView(TextView(activity).apply {
            text = "LOCAL COMMAND PHRASE"
            textSize = 9f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            contentDescription = "Close Teach Vyom"
            setOnClickListener { dialog.dismiss(); onCancel() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        root.addView(TextView(activity).apply {
            text = "JAB MAIN BOLUN"
            textSize = 9f
            letterSpacing = 0.12f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(20), 0, dp(7))
        })
        root.addView(TextView(activity).apply {
            text = "“${originalPhrase.take(140)}”"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(VyomDesignSpec.CYAN)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = VyomTheme.glass(activity, 14, accent)
        })

        root.addView(TextView(activity).apply {
            text = "TAB YE KARO"
            textSize = 9f
            letterSpacing = 0.12f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(16), 0, dp(7))
        })
        val answer = EditText(activity).apply {
            hint = "Torch ON · volume up · open YouTube"
            setSingleLine(true)
            textSize = 14f
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = VyomTheme.glass(activity, 14, accent)
            contentDescription = "Supported device action to associate with this phrase"
        }
        root.addView(answer, LinearLayout.LayoutParams(-1, dp(50)))

        root.addView(TextView(activity).apply {
            text = "SUPPORTED EXAMPLES"
            textSize = 9f
            letterSpacing = 0.1f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(14), 0, dp(7))
        })
        val examples = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Torch ON", "Volume up", "Open YouTube").forEach { example ->
            examples.addView(TextView(activity).apply {
                text = example
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(dp(9), dp(8), dp(9), dp(8))
                background = GradientDrawable().apply {
                    setColor(0x33152235)
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), 0x506482AA)
                }
                setOnClickListener { answer.setText(example); answer.setSelection(answer.text.length) }
            }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginEnd = dp(5) })
        }
        root.addView(examples)

        root.addView(TextView(activity).apply {
            text = "Saved on this phone only. Vyom still applies its built-in safety rules; blocked actions cannot be taught."
            textSize = 10f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(2), dp(12), dp(2), dp(8))
        })

        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val cancel = Button(activity).apply {
            text = "CANCEL"
            VyomTheme.stylePill(this, activity, "cyan", "secondary")
            setOnClickListener { dialog.dismiss(); onCancel() }
        }
        val save = Button(activity).apply {
            text = "SAVE PHRASE"
            VyomTheme.stylePill(this, activity, "cyan", "primary")
            setOnClickListener {
                val command = answer.text.toString().trim()
                if (command.isBlank() || command.length > 120) {
                    answer.error = "Enter a supported command (1–120 characters)."
                    return@setOnClickListener
                }
                dialog.dismiss()
                onSave(command)
            }
        }
        buttons.addView(cancel, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        buttons.addView(save, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(buttons)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.72f)
        }
        dialog.show()
        dialog.window?.setLayout(
            activity.resources.displayMetrics.widthPixels.coerceAtMost(dp(480)),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
