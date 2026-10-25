package com.hermes.assistant

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var orb: OrbSurfaceView
    private lateinit var status: TextView
    private lateinit var pairingInput: EditText
    private lateinit var chatInput: EditText
    private lateinit var transcript: TextView
    private lateinit var pairButton: Button
    private lateinit var sendButton: Button
    private lateinit var api: VyomApiClient
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = VyomApiClient(applicationContext)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
        }

        orb = OrbSurfaceView(this).apply {
            isClickable = false
            isFocusable = false
            setOrbMode(0)
        }

        status = TextView(this).apply {
            text = "VYOM  •  CONNECTING"
            textSize = 9f
            letterSpacing = 0.20f
            setTextColor(Color.rgb(0, 190, 255))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(18))
            setBackgroundColor(Color.argb(225, 8, 12, 20))
        }

        pairingInput = EditText(this).apply {
            hint = "First-time pairing code"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
        }
        pairButton = Button(this).apply {
            text = "CONNECT VYOM"
            setOnClickListener { pair() }
        }
        transcript = TextView(this).apply {
            text = "Hermes chat will appear here."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        }
        val transcriptScroll = ScrollView(this).apply {
            addView(transcript)
        }
        chatInput = EditText(this).apply {
            hint = "Message Hermes..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
        }
        sendButton = Button(this).apply {
            text = "SEND"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }

        panel.addView(pairingInput, LinearLayout.LayoutParams(-1, dp(48)))
        panel.addView(pairButton, LinearLayout.LayoutParams(-1, dp(48)))
        panel.addView(transcriptScroll, LinearLayout.LayoutParams(-1, dp(100)))
        panel.addView(chatInput, LinearLayout.LayoutParams(-1, dp(52)))
        panel.addView(sendButton, LinearLayout.LayoutParams(-1, dp(46)))

        root.addView(orb, FrameLayout.LayoutParams(1, 1, Gravity.CENTER))
        root.addView(status, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(12)
        })
        root.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        root.post {
            val size = (minOf(root.width, root.height) * 0.60f).toInt()
            orb.layoutParams = (orb.layoutParams as FrameLayout.LayoutParams).apply {
                width = size
                height = size
                gravity = Gravity.CENTER
            }
        }

        if (api.hasPairingCode()) {
            status.text = "VYOM  •  RECONNECTING"
            io.execute {
                try {
                    api.reconnect()
                    runOnUiThread {
                        status.text = "VYOM  •  READY"
                        sendButton.isEnabled = true
                        pairingInput.visibility = View.GONE
                        pairButton.visibility = View.GONE
                    }
                } catch (error: Exception) {
                    runOnUiThread { status.text = "VYOM  •  PAIRING REQUIRED" }
                }
            }
        }
    }

    private fun pair() {
        val code = pairingInput.text.toString().trim()
        if (code.isBlank()) {
            status.text = "VYOM  •  ENTER PAIRING CODE"
            return
        }
        pairButton.isEnabled = false
        status.text = "VYOM  •  CONNECTING"
        io.execute {
            try {
                api.pair(code)
                runOnUiThread {
                    status.text = "VYOM  •  READY"
                    pairingInput.visibility = View.GONE
                    pairButton.visibility = View.GONE
                    sendButton.isEnabled = true
                    transcript.text = "Connected to Hermes.\n"
                }
            } catch (error: Exception) {
                runOnUiThread {
                    status.text = "VYOM  •  CONNECTION FAILED"
                    transcript.text = error.message ?: "Could not connect to Hermes."
                    pairButton.isEnabled = true
                }
            }
        }
    }

    private fun sendMessage() {
        val message = chatInput.text.toString().trim()
        if (message.isBlank()) return
        chatInput.setText("")
        sendButton.isEnabled = false
        status.text = "VYOM  •  THINKING"
        transcript.text = "You: $message\n\nHermes: thinking..."
        io.execute {
            try {
                val reply = api.chat(message)
                runOnUiThread {
                    transcript.text = "You: $message\n\nHermes: $reply"
                    status.text = "VYOM  •  READY"
                    sendButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    transcript.text = "You: $message\n\nError: ${error.message ?: "Request failed"}"
                    status.text = "VYOM  •  OFFLINE"
                    sendButton.isEnabled = true
                }
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        if (::orb.isInitialized) orb.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::orb.isInitialized) orb.onResume()
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}
