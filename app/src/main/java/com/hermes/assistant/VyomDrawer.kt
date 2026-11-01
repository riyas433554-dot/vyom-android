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
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Native Android drawer based on the supplied Vyom HUD reference. */
internal class VyomDrawer(
    private val activity: Activity,
    private val subtitle: String,
    private val onNavigate: (String) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()

    fun show() {
        val dialog = Dialog(activity)
        val accent = VyomTheme.accent(
            activity,
            activity.getSharedPreferences("vyom_settings", Activity.MODE_PRIVATE)
                .getString("theme_accent", "cyan") ?: "cyan"
        )
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), 0xF8070B14.toInt())
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(170, Color.red(accent), Color.green(accent), Color.blue(accent)))
            }
        }

        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(TextView(activity).apply {
            text = "VYOM"
            textSize = 22f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        brand.addView(TextView(activity).apply {
            text = subtitle
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            maxLines = 1
        })
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(activity).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            contentDescription = "Close navigation drawer"
            setPadding(dp(12), dp(8), dp(4), dp(8))
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(top, LinearLayout.LayoutParams(-1, -2))

        val search = EditText(activity).apply {
            hint = "Search workspace…"
            isSingleLine = true
            textSize = 14f
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setHintTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = VyomTheme.glass(activity, 14, accent)
        }
        val searchParams = LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(16)
            bottomMargin = dp(8)
        }
        root.addView(search, searchParams)

        val scroll = ScrollView(activity).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, dp(2), 0, dp(8))
        }
        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun render(filter: String) {
            list.removeAllViews()
            val query = filter.trim().lowercase()
            val matches = VyomDesignSpec.drawerItems.filter {
                query.isBlank() || it.label.lowercase().contains(query) || it.section.lowercase().contains(query)
            }
            var lastSection = ""
            matches.forEach { item ->
                if (item.section != lastSection) {
                    lastSection = item.section
                    list.addView(TextView(activity).apply {
                        text = item.section.uppercase()
                        textSize = 9f
                        letterSpacing = 0.16f
                        typeface = Typeface.MONOSPACE
                        setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                        setPadding(dp(4), dp(14), dp(4), dp(6))
                    }, LinearLayout.LayoutParams(-1, -2))
                }
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = GradientDrawable().apply {
                        setColor(if (item.enabled) 0x331B3550 else 0x180F1A2E)
                        cornerRadius = dp(13).toFloat()
                        if (item.id == "home") setStroke(dp(1), Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent)))
                    }
                    alpha = if (item.enabled) 1f else 0.62f
                    isEnabled = item.enabled
                    isClickable = item.enabled
                    isFocusable = item.enabled
                    setOnClickListener {
                        dialog.dismiss()
                        onNavigate(item.id)
                    }
                }
                row.addView(TextView(activity).apply {
                    text = "•"
                    textSize = 20f
                    setTextColor(if (item.enabled) accent else VyomDesignSpec.TEXT_SECONDARY)
                    gravity = Gravity.CENTER
                }, LinearLayout.LayoutParams(dp(24), -2))
                row.addView(TextView(activity).apply {
                    text = item.label
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(if (item.enabled) VyomDesignSpec.TEXT_PRIMARY else VyomDesignSpec.TEXT_SECONDARY)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                if (!item.enabled) {
                    row.addView(TextView(activity).apply {
                        text = "SOON"
                        textSize = 8f
                        typeface = Typeface.MONOSPACE
                        setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                    })
                }
                list.addView(row, LinearLayout.LayoutParams(-1, dp(44)).apply {
                    bottomMargin = dp(3)
                })
            }
            if (matches.isEmpty()) {
                list.addView(TextView(activity).apply {
                    text = "No matching destination"
                    textSize = 13f
                    setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                    setPadding(dp(12), dp(18), dp(12), dp(18))
                })
            }
        }
        render("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val stop = Button(activity).apply {
            text = "STOP CONTROL"
            contentDescription = "Emergency stop for all Vyom device actions"
            VyomTheme.stylePill(this, activity, "cyan", "danger")
            setOnClickListener {
                dialog.dismiss()
                onNavigate("stop_control")
            }
        }
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(8)
        })

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.68f)
            setLayout(dp(344).coerceAtMost(activity.resources.displayMetrics.widthPixels - dp(24)),
                WindowManager.LayoutParams.MATCH_PARENT)
            attributes = attributes.apply { gravity = Gravity.END }
        }
        dialog.show()
        dialog.window?.setLayout(
            dp(344).coerceAtMost(activity.resources.displayMetrics.widthPixels - dp(24)),
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }
}
