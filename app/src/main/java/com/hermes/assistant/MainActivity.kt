package com.hermes.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var orb: OrbSurfaceView
    private lateinit var status: TextView
    private lateinit var buildInfo: TextView
    private lateinit var pairingInput: EditText
    private lateinit var chatInput: EditText
    private lateinit var transcript: TextView
    private lateinit var taskInput: EditText
    private lateinit var taskList: LinearLayout
    private lateinit var taskScroll: ScrollView
    private lateinit var taskNotice: TextView
    private lateinit var pairButton: Button
    private lateinit var sendButton: Button
    private lateinit var createTaskButton: Button
    private lateinit var chatTab: Button
    private lateinit var tasksTab: Button
    private lateinit var settingsTab: Button
    private lateinit var filesTab: Button
    private lateinit var controlTab: Button
    private lateinit var chatPanel: LinearLayout
    private lateinit var tasksPanel: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    private lateinit var filesPanel: LinearLayout
    private lateinit var filesList: LinearLayout
    private lateinit var filesNotice: TextView
    private lateinit var controlPanel: LinearLayout
    private lateinit var controlNotice: TextView
    private lateinit var controlAuditList: LinearLayout
    private lateinit var reduceMotionButton: Button
    private lateinit var voiceRepliesButton: Button
    private lateinit var updateButton: Button
    private lateinit var updateNotice: TextView
    private lateinit var personaButton: Button
    private lateinit var settingsContent: LinearLayout
    private var updateManifest: JSONObject? = null
    private var pendingUpdateApk: File? = null
    private var updateCheckInFlight = false
    private lateinit var voiceButton: Button
    private lateinit var micIndicator: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var voiceRepliesEnabled = false
    private var hinglishPersona = true
    private var listening = false
    private lateinit var api: VyomApiClient
    private val io = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val settingsPrefs by lazy { getSharedPreferences("vyom_settings", MODE_PRIVATE) }
    private val controlPrefs by lazy { getSharedPreferences("vyom_control_audit", MODE_PRIVATE) }
    private var tasksTabVisible = false
    private var orbMode = 0
    private var reduceMotion = false
    private var taskRefreshInFlight = false
    private val tasksById = linkedMapOf<String, JSONObject>()
    private data class ChatMessage(val id: String, val text: String, var state: String, var reply: String = "")
    private val chatMessages = mutableListOf<ChatMessage>()
    private val chatPrefs by lazy { getSharedPreferences("vyom_chat_history", MODE_PRIVATE) }
    private var chatReady = false
    private val refreshTasksRunnable = object : Runnable {
        override fun run() {
            if (!tasksTabVisible || isFinishing || isDestroyed) return
            refreshTasks()
            mainHandler.postDelayed(this, TASK_POLL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = VyomApiClient(applicationContext)
        orbMode = settingsPrefs.getInt("orb_mode", 0).coerceIn(0, 3)
        reduceMotion = settingsPrefs.getBoolean("reduce_motion", false)
        voiceRepliesEnabled = settingsPrefs.getBoolean("voice_replies", false)
        hinglishPersona = settingsPrefs.getBoolean("hinglish_persona", true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
        }
        orb = OrbSurfaceView(this).apply {
            isClickable = false
            isFocusable = false
            setOrbMode(orbMode)
            setReduceMotion(reduceMotion)
        }
        status = TextView(this).apply {
            text = "VYOM  •  CONNECTING"
            textSize = 9f
            letterSpacing = 0.20f
            setTextColor(Color.rgb(0, 190, 255))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        buildInfo = TextView(this).apply {
            text = "Vyom v${BuildConfig.VERSION_NAME} • ${BuildConfig.GIT_COMMIT_SHORT}"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(238, 8, 12, 20))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.argb(150, 0, 190, 255))
            }
            elevation = dp(8).toFloat()
        }
        val tabBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chatTab = Button(this).apply {
            text = "CHAT"
            setOnClickListener { showTab(false) }
        }
        tasksTab = Button(this).apply {
            text = "TASKS"
            setOnClickListener { showTab(true) }
        }
        settingsTab = Button(this).apply {
            text = "SETTINGS"
            setOnClickListener { showTab(false, true) }
        }
        filesTab = Button(this).apply {
            text = "FILES"
            setOnClickListener { showTab(false, false, true) }
        }
        controlTab = Button(this).apply {
            text = "CONTROL"
            setOnClickListener { showTab(false, false, false, true) }
        }
        tabBar.addView(chatTab, LinearLayout.LayoutParams(0, dp(44), 1f))
        tabBar.addView(tasksTab, LinearLayout.LayoutParams(0, dp(44), 1f))
        tabBar.addView(settingsTab, LinearLayout.LayoutParams(0, dp(44), 1f))
        tabBar.addView(filesTab, LinearLayout.LayoutParams(0, dp(44), 1f))
        tabBar.addView(controlTab, LinearLayout.LayoutParams(0, dp(44), 1f))

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

        chatPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        transcript = TextView(this).apply {
            text = "Hermes chat will appear here."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
            setTextIsSelectable(true)
        }
        val transcriptScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(transcript, FrameLayout.LayoutParams(-1, -2))
        }
        val chatInputLabel = TextView(this).apply { text = "CHAT MESSAGE"; setTextColor(Color.LTGRAY); textSize = 10f }
        chatInput = EditText(this).apply {
            id = View.generateViewId()
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            hint = "Type a message for Chat..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
        }
        sendButton = Button(this).apply {
            text = "SEND"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        chatPanel.addView(transcriptScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        chatPanel.addView(chatInputLabel, LinearLayout.LayoutParams(-1, dp(16)))
        chatPanel.addView(chatInput, LinearLayout.LayoutParams(-1, dp(50)))
        chatPanel.addView(sendButton, LinearLayout.LayoutParams(-1, dp(44)))
        micIndicator = TextView(this).apply {
            text = "MIC OFF"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            contentDescription = "Microphone status: off"
        }
        voiceButton = Button(this).apply {
            text = "TALK"
            contentDescription = "Start foreground voice input"
            setOnClickListener { toggleVoiceInput() }
        }
        chatPanel.addView(micIndicator, LinearLayout.LayoutParams(-1, dp(18)))
        chatPanel.addView(voiceButton, LinearLayout.LayoutParams(-1, dp(40)))

        tasksPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        taskNotice = TextView(this).apply {
            text = "Tasks run one at a time. Queued is normal."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(5), 0, dp(5))
        }
        val taskInputLabel = TextView(this).apply { text = "TASK DESCRIPTION"; setTextColor(Color.LTGRAY); textSize = 10f }
        taskInput = EditText(this).apply {
            id = View.generateViewId()
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            hint = "Task-only input (max 4000 chars)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            minLines = 2
            maxLines = 4
        }
        createTaskButton = Button(this).apply {
            text = "CREATE TASK"
            isEnabled = false
            setOnClickListener { createTask() }
        }
        taskList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        taskScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(taskList, FrameLayout.LayoutParams(-1, -2))
        }
        tasksPanel.addView(taskNotice, LinearLayout.LayoutParams(-1, -2))
        tasksPanel.addView(taskInputLabel, LinearLayout.LayoutParams(-1, dp(16)))
        tasksPanel.addView(taskInput, LinearLayout.LayoutParams(-1, dp(66)))
        tasksPanel.addView(createTaskButton, LinearLayout.LayoutParams(-1, dp(44)))
        tasksPanel.addView(taskScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        settingsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(4), dp(8), dp(4), dp(4))
        }
        settingsContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        settingsContent.addView(TextView(this).apply {
            text = "ORB APPEARANCE"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }, LinearLayout.LayoutParams(-1, dp(24)))
        val orbColors = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("CYAN", "AMBER", "VIOLET", "GREEN").forEachIndexed { mode, label ->
            orbColors.addView(Button(this).apply {
                text = label
                contentDescription = "Set orb color to $label"
                setOnClickListener { setOrbAppearance(mode) }
            }, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        settingsContent.addView(orbColors, LinearLayout.LayoutParams(-1, dp(48)))
        reduceMotionButton = Button(this).apply {
            text = if (reduceMotion) "REDUCE MOTION: ON" else "REDUCE MOTION: OFF"
            contentDescription = "Toggle reduced motion"
            setOnClickListener { setReduceMotion(!reduceMotion) }
        }
        settingsContent.addView(reduceMotionButton, LinearLayout.LayoutParams(-1, dp(44)))
        voiceRepliesButton = Button(this).apply {
            text = if (voiceRepliesEnabled) "VOICE REPLIES: ON" else "VOICE REPLIES: OFF"
            contentDescription = "Toggle spoken replies"
            setOnClickListener { setVoiceRepliesEnabled(!voiceRepliesEnabled) }
        }
        settingsContent.addView(voiceRepliesButton, LinearLayout.LayoutParams(-1, dp(44)))
        personaButton = Button(this).apply {
            text = if (hinglishPersona) "PERSONA: HINGLISH" else "PERSONA: ENGLISH"
            contentDescription = "Switch Vyom response persona language"
            setOnClickListener { setHinglishPersona(!hinglishPersona) }
        }
        settingsContent.addView(personaButton, LinearLayout.LayoutParams(-1, dp(44)))
        settingsContent.addView(TextView(this).apply {
            text = "Settings are saved on this device. Orb choices change appearance only; they do not enable microphone or background listening."
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }, LinearLayout.LayoutParams(-1, -2))
        updateButton = Button(this).apply {
            text = "CHECK FOR UPDATES"
            setOnClickListener { checkForUpdate() }
        }
        settingsContent.addView(updateButton, LinearLayout.LayoutParams(-1, dp(44)))
        updateNotice = TextView(this).apply {
            text = "Updates are downloaded only after you ask. SHA-256 and app signature are checked before the Android installer opens."
            textSize = 10f
            setTextColor(Color.LTGRAY)
        }
        settingsContent.addView(updateNotice, LinearLayout.LayoutParams(-1, -2))
        val settingsScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(settingsContent, FrameLayout.LayoutParams(-1, -2))
        }
        settingsPanel.addView(settingsScroll, LinearLayout.LayoutParams(-1, 0, 1f))


        panel.addView(buildInfo, LinearLayout.LayoutParams(-1, dp(22)))
        panel.addView(tabBar, LinearLayout.LayoutParams(-1, dp(44)))
        panel.addView(pairingInput, LinearLayout.LayoutParams(-1, dp(46)))
        panel.addView(pairButton, LinearLayout.LayoutParams(-1, dp(44)))
        panel.addView(chatPanel, LinearLayout.LayoutParams(-1, dp(250)))
        panel.addView(tasksPanel, LinearLayout.LayoutParams(-1, dp(250)))
        panel.addView(settingsPanel, LinearLayout.LayoutParams(-1, dp(250)))
        filesPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        filesNotice = TextView(this).apply {
            text = "Files, artifact links and screenshots from completed task results appear here."
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(6))
        }
        filesList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val filesScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(filesList, FrameLayout.LayoutParams(-1, -2))
        }
        filesPanel.addView(filesNotice, LinearLayout.LayoutParams(-1, -2))
        filesPanel.addView(filesScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(filesPanel, LinearLayout.LayoutParams(-1, dp(250)))
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        controlNotice = TextView(this).apply {
            text = "Device actions always ask first. Only allowlisted Android Settings screens can be opened."
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(6))
        }
        val controlButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controlButtons.addView(Button(this).apply {
            text = "WI-FI SETTINGS"
            setOnClickListener { confirmControlAction("Open Wi-Fi settings", Settings.ACTION_WIFI_SETTINGS) }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        controlButtons.addView(Button(this).apply {
            text = "DISPLAY SETTINGS"
            setOnClickListener { confirmControlAction("Open display settings", Settings.ACTION_DISPLAY_SETTINGS) }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        controlPanel.addView(controlNotice, LinearLayout.LayoutParams(-1, -2))
        controlPanel.addView(controlButtons, LinearLayout.LayoutParams(-1, dp(48)))
        controlPanel.addView(Button(this).apply {
            text = "VYOM APP SETTINGS"
            setOnClickListener { confirmControlAction("Open Vyom app settings", "app-details") }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        controlPanel.addView(TextView(this).apply {
            text = "LOCAL AUDIT LOG"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(5), 0, dp(3))
        }, LinearLayout.LayoutParams(-1, dp(22)))
        controlAuditList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val auditScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(controlAuditList, FrameLayout.LayoutParams(-1, -2))
        }
        controlPanel.addView(auditScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(controlPanel, LinearLayout.LayoutParams(-1, dp(250)))

        root.addView(orb, FrameLayout.LayoutParams(1, 1, Gravity.CENTER))
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(12)
        })
        root.addView(panel, FrameLayout.LayoutParams(-1, dp(440), Gravity.BOTTOM))
        setContentView(root)
        textToSpeech = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("en", "IN")
            }
        }

        root.post {
            val size = (minOf(root.width, root.height) * 0.60f).toInt()
            orb.layoutParams = (orb.layoutParams as FrameLayout.LayoutParams).apply {
                width = size
                height = size
                gravity = Gravity.CENTER
            }
        }
        restoreChatMessages()
        showTab(false)
        if (api.hasPairingCode()) {
            status.text = "VYOM  •  RECONNECTING"
            io.execute {
                try {
                    api.reconnect()
                    runOnUiThread {
                        status.text = "VYOM  •  READY"
                        chatReady = true
                        sendButton.isEnabled = true
                        createTaskButton.isEnabled = true
                        pairingInput.visibility = View.GONE
                        pairButton.visibility = View.GONE
                        renderSavedTaskPlaceholders()
                        resumeQueuedChatMessages()
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        status.text = "VYOM  •  PAIRING REQUIRED"
                        taskNotice.text = SESSION_EXPIRED_MESSAGE
                    }
                }
            }
        } else {
            taskNotice.text = "Pair Vyom to use Tasks. Chat remains available after pairing."
        }
    }

    private fun showTab(showTasks: Boolean, showSettings: Boolean = false, showFiles: Boolean = false, showControl: Boolean = false) {
        tasksTabVisible = showTasks || showFiles
        val anyOtherPanel = showTasks || showSettings || showFiles || showControl
        chatPanel.visibility = if (anyOtherPanel) View.GONE else View.VISIBLE
        tasksPanel.visibility = if (showTasks) View.VISIBLE else View.GONE
        settingsPanel.visibility = if (showSettings) View.VISIBLE else View.GONE
        filesPanel.visibility = if (showFiles) View.VISIBLE else View.GONE
        controlPanel.visibility = if (showControl) View.VISIBLE else View.GONE
        chatTab.alpha = if (anyOtherPanel) 0.65f else 1f
        tasksTab.alpha = if (showTasks) 1f else 0.65f
        settingsTab.alpha = if (showSettings) 1f else 0.65f
        filesTab.alpha = if (showFiles) 1f else 0.65f
        controlTab.alpha = if (showControl) 1f else 0.65f
        mainHandler.removeCallbacks(refreshTasksRunnable)
        if (tasksTabVisible) {
            refreshTasks()
            mainHandler.postDelayed(refreshTasksRunnable, TASK_POLL_MS)
        }
        if (showControl) renderControlAudit()
    }

    private fun confirmControlAction(label: String, action: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm device action")
            .setMessage("Allow Vyom to $label? This opens an Android Settings screen; it does not change settings automatically.")
            .setNegativeButton("CANCEL") { _, _ -> logControlAction(label, "cancelled") }
            .setPositiveButton("OPEN") { _, _ -> performControlAction(label, action) }
            .show()
    }

    private fun performControlAction(label: String, action: String) {
        val intent = if (action == "app-details") {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        } else {
            Intent(action)
        }
        try {
            if (intent.resolveActivity(packageManager) == null) {
                logControlAction(label, "unavailable")
                controlNotice.text = "This settings screen is unavailable on this device."
                return
            }
            startActivity(intent)
            logControlAction(label, "opened")
            controlNotice.text = "$label opened."
        } catch (_: Exception) {
            logControlAction(label, "failed")
            controlNotice.text = "Could not open that settings screen."
        }
    }

    private fun logControlAction(action: String, result: String) {
        val rows = try { JSONArray(controlPrefs.getString("events", "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        next.put(JSONObject().put("at", java.time.Instant.now().toString()).put("action", action).put("result", result))
        for (i in 0 until minOf(rows.length(), 49)) next.put(rows.getJSONObject(i))
        controlPrefs.edit().putString("events", next.toString()).apply()
        renderControlAudit()
    }

    private fun renderControlAudit() {
        if (!::controlAuditList.isInitialized) return
        controlAuditList.removeAllViews()
        val rows = try { JSONArray(controlPrefs.getString("events", "[]")) } catch (_: Exception) { JSONArray() }
        if (rows.length() == 0) {
            controlAuditList.addView(TextView(this).apply {
                text = "No device actions yet."
                textSize = 11f
                setTextColor(Color.LTGRAY)
            })
            return
        }
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            controlAuditList.addView(TextView(this).apply {
                text = "${row.optString("at")} • ${row.optString("action")} — ${row.optString("result")}"
                textSize = 10f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(2), 0, dp(2))
            })
        }
    }

    private fun showPairingRequired() {
        pairingInput.visibility = View.VISIBLE
        pairButton.visibility = View.VISIBLE
        pairButton.isEnabled = true
        chatReady = false
        sendButton.isEnabled = false
        createTaskButton.isEnabled = false
        status.text = personaText("VYOM • PAIRING REQUIRED", "VYOM • DOBARA PAIR KARO")
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
                    chatReady = true
                    sendButton.isEnabled = true
                    createTaskButton.isEnabled = true
                    renderChatMessages()
                    taskNotice.text = "Tasks run one at a time. Queued is normal."
                    resumeQueuedChatMessages()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    status.text = "VYOM  •  CONNECTION FAILED"
                    transcript.text = "Could not connect to Hermes. Check the pairing code and try again."
                    pairButton.isEnabled = true
                }
            }
        }
    }

    private fun sendMessage() {
        val message = chatInput.text.toString().trim()
        if (message.isBlank() || !chatReady) return
        chatInput.setText("")
        val item = ChatMessage(UUID.randomUUID().toString(), message, "queued")
        chatMessages.add(item)
        persistChatMessages()
        renderChatMessages()
        status.text = "VYOM  •  QUEUED"
        // Single-thread IO executor preserves FIFO order; SEND stays enabled for queueing.
        scheduleChatMessage(item)
    }

    private fun scheduleChatMessage(item: ChatMessage) {
        io.execute {
            runOnUiThread {
                item.state = "running"
                persistChatMessages()
                status.text = "VYOM  •  THINKING"
                renderChatMessages()
            }
            try {
                val reply = api.chat(personaPrompt(item.text))
                runOnUiThread {
                    item.state = "done"
                    item.reply = reply
                    persistChatMessages()
                    if (voiceRepliesEnabled) speakReply(reply)
                    status.text = if (chatMessages.any { it.state == "queued" || it.state == "running" }) "VYOM  •  THINKING" else "VYOM  •  READY"
                    renderChatMessages()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    item.state = "error"
                    item.reply = error.message?.takeIf { it.isNotBlank() } ?: "Chat request failed. Please try again."
                    persistChatMessages()
                    if (error is VyomApiClient.SessionExpiredException) {
                        showPairingRequired()
                    } else {
                        status.text = "VYOM  •  OFFLINE"
                    }
                    renderChatMessages()
                }
            }
        }
    }

    private fun restoreChatMessages() {
        val raw = chatPrefs.getString("messages", null) ?: return
        try {
            val saved = JSONArray(raw)
            for (index in 0 until saved.length()) {
                val value = saved.optJSONObject(index) ?: continue
                val savedState = value.optString("state", "queued")
                chatMessages.add(ChatMessage(
                    value.optString("id", UUID.randomUUID().toString()),
                    value.optString("text"),
                    if (savedState == "running") "queued" else savedState,
                    value.optString("reply")
                ))
            }
            renderChatMessages()
        } catch (_: Exception) {
            // Keep the stored history intact if it cannot be parsed.
        }
    }

    private fun resumeQueuedChatMessages() {
        chatMessages.filter { it.state == "queued" }.forEach { scheduleChatMessage(it) }
    }

    private fun persistChatMessages() {
        val saved = JSONArray()
        chatMessages.forEach { item ->
            saved.put(JSONObject()
                .put("id", item.id)
                .put("text", item.text)
                .put("state", item.state)
                .put("reply", item.reply))
        }
        chatPrefs.edit().putString("messages", saved.toString()).apply()
    }

    private fun renderChatMessages() {
        val content = if (chatMessages.isEmpty()) personaText("Hermes chat will appear here.", "Hermes yahan ready hai. Message bhejo.") else
            chatMessages.joinToString("\n\n") { item ->
                "You: ${item.text}\nStatus: ${item.state}" +
                    if (item.reply.isNotBlank()) "\nHermes: ${item.reply}" else ""
            }
        transcript.text = android.text.Html.fromHtml(markdownToHtml(content), android.text.Html.FROM_HTML_MODE_COMPACT)
    }

    private fun markdownToHtml(input: String): String {
        fun escapeHtml(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        return input.split("\n").joinToString("<br>") { rawLine ->
            var line = escapeHtml(rawLine)
            line = line.replace(Regex("^### (.+)$"), "<h3>$1</h3>")
            line = line.replace(Regex("^## (.+)$"), "<h2>$1</h2>")
            line = line.replace(Regex("^# (.+)$"), "<h1>$1</h1>")
            line = line.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
            line = line.replace(Regex("\\*(.+?)\\*"), "<i>$1</i>")
            line = line.replace(Regex("`(.+?)`"), "<tt>$1</tt>")
            line
        }
    }

    private fun setOrbAppearance(mode: Int) {
        orbMode = mode.coerceIn(0, 3)
        settingsPrefs.edit().putInt("orb_mode", orbMode).apply()
        if (::orb.isInitialized) orb.setOrbMode(orbMode)
        status.text = "VYOM  •  ORB UPDATED"
    }

    private fun setReduceMotion(enabled: Boolean) {
        reduceMotion = enabled
        settingsPrefs.edit().putBoolean("reduce_motion", enabled).apply()
        if (::orb.isInitialized) orb.setReduceMotion(enabled)
        if (::reduceMotionButton.isInitialized) reduceMotionButton.text = if (enabled) "REDUCE MOTION: ON" else "REDUCE MOTION: OFF"
    }

    private fun personaPrompt(message: String): String {
        val instruction = if (hinglishPersona) {
            "Reply in natural Hinglish using Latin script, mixing Hindi and English naturally. Keep code and technical identifiers unchanged."
        } else {
            "Reply in clear English. Keep code and technical identifiers unchanged."
        }
        val prompt = "$instruction\n\nUser message:\n$message"
        return if (prompt.length <= 4000) prompt else message
    }

    private fun setHinglishPersona(enabled: Boolean) {
        hinglishPersona = enabled
        settingsPrefs.edit().putBoolean("hinglish_persona", enabled).apply()
        personaButton.text = if (enabled) "PERSONA: HINGLISH" else "PERSONA: ENGLISH"
        renderChatMessages()
        status.text = personaText("VYOM • PERSONA UPDATED", "VYOM • PERSONA BADAL GAYA")
    }

    private fun personaText(english: String, hinglish: String): String = if (hinglishPersona) hinglish else english

    private fun setVoiceRepliesEnabled(enabled: Boolean) {
        voiceRepliesEnabled = enabled
        settingsPrefs.edit().putBoolean("voice_replies", enabled).apply()
        voiceRepliesButton.text = if (enabled) "VOICE REPLIES: ON" else "VOICE REPLIES: OFF"
        if (!enabled) textToSpeech?.stop()
    }

    private fun speakReply(reply: String) {
        val engine = textToSpeech ?: return
        val spoken = reply.trim().take(4000)
        if (spoken.isNotEmpty()) engine.speak(spoken, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    private fun toggleVoiceInput() {
        if (listening) {
            stopVoiceInput(cancel = true)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            micIndicator.text = "MIC UNAVAILABLE"
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        startVoiceInput()
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            micIndicator.text = "MIC UNAVAILABLE"
            return
        }
        stopVoiceInput(cancel = true, updateStatus = false)
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                micIndicator.text = "MIC LISTENING — SPEAK NOW"
                micIndicator.contentDescription = "Microphone status: listening"
            }
            override fun onBeginningOfSpeech() { micIndicator.text = "MIC HEARING SPEECH" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { micIndicator.text = "MIC PROCESSING" }
            override fun onError(error: Int) {
                listening = false
                micIndicator.text = "MIC OFF — TRY AGAIN"
                micIndicator.contentDescription = "Microphone status: off"
                voiceButton.text = "TALK"
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) chatInput.setText(spoken)
                listening = false
                micIndicator.text = if (spoken.isBlank()) "MIC OFF — NO SPEECH" else "MIC READY — REVIEW TEXT"
                micIndicator.contentDescription = "Microphone status: off"
                voiceButton.text = "TALK"
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (listening) micIndicator.text = "MIC HEARING…"
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        listening = true
        voiceButton.text = "STOP LISTENING"
        micIndicator.text = "MIC STARTING"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { recognizer.startListening(intent) } catch (_: Exception) {
            stopVoiceInput(cancel = true)
            micIndicator.text = "MIC UNAVAILABLE"
        }
    }

    private fun stopVoiceInput(cancel: Boolean, updateStatus: Boolean = true) {
        listening = false
        try {
            if (cancel) speechRecognizer?.cancel() else speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
        if (::voiceButton.isInitialized) voiceButton.text = "TALK"
        if (updateStatus && ::micIndicator.isInitialized) {
            micIndicator.text = "MIC OFF"
            micIndicator.contentDescription = "Microphone status: off"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoiceInput()
            else {
                micIndicator.text = "MIC PERMISSION DENIED"
                micIndicator.contentDescription = "Microphone permission denied"
            }
        }
    }

    private fun checkForUpdate() {
        if (updateCheckInFlight) return
        updateCheckInFlight = true
        updateButton.isEnabled = false
        updateNotice.text = "Checking GitHub releases…"
        io.execute {
            try {
                val manifest = fetchLatestUpdateManifest()
                runOnUiThread {
                    updateCheckInFlight = false
                    updateButton.isEnabled = true
                    val versionCode = manifest.optInt("versionCode", 0)
                    if (versionCode <= BuildConfig.VERSION_CODE) {
                        updateManifest = null
                        updateButton.text = "CHECK FOR UPDATES"
                        updateButton.setOnClickListener { checkForUpdate() }
                        updateNotice.text = "Vyom is up to date (v${BuildConfig.VERSION_NAME})."
                    } else {
                        updateManifest = manifest
                        updateButton.text = "DOWNLOAD v${manifest.optString("versionName")}"
                        updateButton.setOnClickListener { downloadVerifiedUpdate() }
                        updateNotice.text = "Update v${manifest.optString("versionName")} is available. Download and verification require your tap."
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    updateCheckInFlight = false
                    updateButton.isEnabled = true
                    updateButton.text = "CHECK FOR UPDATES"
                    updateButton.setOnClickListener { checkForUpdate() }
                    updateNotice.text = "Update check failed: ${error.message ?: "GitHub release unavailable"}"
                }
            }
        }
    }

    private fun fetchLatestUpdateManifest(): JSONObject {
        val release = JSONObject(readHttpsText("https://api.github.com/repos/riyas433554-dot/hermes/releases/latest", 128 * 1024))
        val assets = release.optJSONArray("assets") ?: throw IllegalStateException("Release assets missing")
        var manifestUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name") == "vyom-update.json") manifestUrl = asset.optString("browser_download_url")
        }
        val url = manifestUrl ?: throw IllegalStateException("Signed update manifest not published")
        validateReleaseUrl(url)
        return JSONObject(readHttpsText(url, 64 * 1024))
    }

    private fun downloadVerifiedUpdate() {
        val manifest = updateManifest ?: run { checkForUpdate(); return }
        updateButton.isEnabled = false
        updateNotice.text = "Downloading update and checking integrity…"
        io.execute {
            try {
                val versionCode = manifest.optInt("versionCode", 0)
                if (versionCode <= BuildConfig.VERSION_CODE) throw IllegalStateException("Not a newer version")
                val apkUrl = manifest.optString("apk")
                validateReleaseUrl(apkUrl)
                val expectedHash = manifest.optString("sha256").lowercase(Locale.ROOT)
                if (!expectedHash.matches(Regex("^[a-f0-9]{64}$"))) throw IllegalStateException("Invalid update checksum")
                val folder = File(cacheDir, "updates").apply { mkdirs() }
                val file = File(folder, "vyom-update.apk")
                downloadHttpsFile(apkUrl, file, 100L * 1024L * 1024L)
                val actualHash = sha256(file)
                if (!MessageDigest.isEqual(expectedHash.toByteArray(), actualHash.toByteArray())) {
                    throw IllegalStateException("Update checksum mismatch")
                }
                val archive = packageManager.getPackageArchiveInfo(file.absolutePath, packageInfoFlags())
                    ?: throw IllegalStateException("Downloaded file is not an APK")
                if (archive.packageName != packageName) throw IllegalStateException("APK package does not match Vyom")
                if (packageVersionCode(archive) != versionCode.toLong()) throw IllegalStateException("APK version does not match manifest")
                if (!sameSigningCertificate(archive)) throw IllegalStateException("APK signing certificate does not match installed Vyom")
                runOnUiThread {
                    pendingUpdateApk = file
                    updateButton.isEnabled = true
                    updateButton.text = "INSTALL VERIFIED UPDATE"
                    updateButton.setOnClickListener { installVerifiedUpdate() }
                    updateNotice.text = "Checksum, package, version and signing certificate verified. Android will ask you to confirm installation."
                }
            } catch (error: Exception) {
                runOnUiThread {
                    updateButton.isEnabled = true
                    updateButton.text = "CHECK FOR UPDATES"
                    updateButton.setOnClickListener { checkForUpdate() }
                    updateNotice.text = "Update rejected: ${error.message ?: "verification failed"}"
                }
            }
        }
    }

    private fun installVerifiedUpdate() {
        val file = pendingUpdateApk?.takeIf { it.isFile && it.length() > 0 } ?: run {
            updateNotice.text = "No verified APK is ready. Check for updates first."
            return
        }
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            updateNotice.text = "Allow Vyom to install this verified update, then tap INSTALL VERIFIED UPDATE again."
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            updateNotice.text = "Android installer opened. Existing Vyom remains installed unless you confirm replacement."
        } catch (_: Exception) {
            updateNotice.text = "Could not open Android installer. Existing Vyom was not replaced."
        }
    }

    private fun validateReleaseUrl(value: String) {
        val uri = Uri.parse(value)
        if (!uri.scheme.equals("https", true) || !uri.host.equals("github.com", true) ||
            !uri.path.orEmpty().startsWith("/riyas433554-dot/hermes/releases/download/")) {
            throw IllegalStateException("Update URL is not an approved GitHub release asset")
        }
    }

    private fun validateReleaseUrlOrApi(value: String) {
        val uri = Uri.parse(value)
        val validReleaseAsset = uri.host.equals("github.com", true) &&
            uri.path.orEmpty().startsWith("/riyas433554-dot/hermes/releases/download/")
        val validApi = uri.host.equals("api.github.com", true) &&
            uri.path == "/repos/riyas433554-dot/hermes/releases/latest"
        if (!uri.scheme.equals("https", true) || (!validReleaseAsset && !validApi)) {
            throw IllegalStateException("Unapproved update endpoint")
        }
    }

    private fun readHttpsText(value: String, maxBytes: Int): String {
        validateReleaseUrlOrApi(value)
        val connection = (URL(value).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Vyom-Android")
            setRequestProperty("Accept", "application/vnd.github+json, application/json")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw IllegalStateException("Response too large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally { connection.disconnect() }
    }

    private fun downloadHttpsFile(value: String, target: File, maxBytes: Long) {
        validateReleaseUrl(value)
        val connection = (URL(value).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 120000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Vyom-Android")
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("APK download HTTP ${connection.responseCode}")
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(16384)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maxBytes) throw IllegalStateException("APK exceeds 100 MiB limit")
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (target.length() == 0L) throw IllegalStateException("Empty APK download")
        } finally { connection.disconnect() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16384)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun packageInfoFlags(): Int {
        if (Build.VERSION.SDK_INT >= 28) return PackageManager.GET_SIGNING_CERTIFICATES
        return legacyPackageInfoFlags()
    }

    @Suppress("DEPRECATION")
    private fun legacyPackageInfoFlags(): Int = PackageManager.GET_SIGNATURES

    private fun packageVersionCode(info: PackageInfo): Long {
        if (Build.VERSION.SDK_INT >= 28) return info.longVersionCode
        return legacyVersionCode(info)
    }

    @Suppress("DEPRECATION")
    private fun legacyVersionCode(info: PackageInfo): Long = info.versionCode.toLong()

    private fun signerSet(info: PackageInfo): List<String> {
        if (Build.VERSION.SDK_INT >= 28) return info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.sorted().orEmpty()
        return legacySignerSet(info)
    }

    @Suppress("DEPRECATION")
    private fun legacySignerSet(info: PackageInfo): List<String> = info.signatures?.map { it.toCharsString() }?.sorted().orEmpty()

    private fun sameSigningCertificate(archive: PackageInfo): Boolean {
        val current = packageManager.getPackageInfo(packageName, packageInfoFlags())
        return signerSet(current).isNotEmpty() && signerSet(current) == signerSet(archive)
    }

    private fun createTask() {
        val prompt = taskInput.text.toString().trim()
        if (prompt.isBlank()) return
        if (prompt.length > 4000) {
            taskNotice.text = "Task description must be 4000 characters or fewer."
            return
        }
        createTaskButton.isEnabled = false
        taskNotice.text = "Submitting task…"
        io.execute {
            try {
                val task = api.createTask(prompt)
                runOnUiThread {
                    taskInput.setText("")
                    tasksById[task.optString("id")] = task
                    renderTasks()
                    renderFiles()
                    taskNotice.text = "Task submitted. Queued is normal; the server runs tasks serially."
                    createTaskButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (error is VyomApiClient.TaskApiException && error.statusCode == 401) {
                        showPairingRequired()
                        taskNotice.text = taskApiErrorMessage(error.statusCode)
                    } else {
                        taskNotice.text = if (error is VyomApiClient.TaskApiException) {
                            taskApiErrorMessage(error.statusCode)
                        } else {
                            "Tasks API unavailable. Chat is still available in the Chat tab."
                        }
                        createTaskButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun refreshTasks() {
        if (!tasksTabVisible || taskRefreshInFlight || isFinishing || isDestroyed) return
        taskRefreshInFlight = true
        io.execute {
            try {
                val ids = api.savedTaskIds()
                val updated = ids.mapNotNull { id ->
                    try { api.getTask(id) } catch (error: Exception) {
                        if (error is VyomApiClient.TaskApiException) throw error
                        null
                    }
                }
                runOnUiThread {
                    updated.forEach { tasksById[it.optString("id")] = it }
                    renderTasks()
                    renderFiles()
                    if (updated.isEmpty() && tasksById.isEmpty()) {
                        taskNotice.text = "No tasks yet. Create one above."
                    } else if (!taskNotice.text.toString().contains("session expired", true)) {
                        taskNotice.text = "Tasks run serially. Queued is normal."
                    }
                    taskRefreshInFlight = false
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (error is VyomApiClient.TaskApiException && error.statusCode == 401) {
                        showPairingRequired()
                        taskNotice.text = taskApiErrorMessage(error.statusCode)
                    } else if (error is VyomApiClient.TaskApiException) {
                        taskNotice.text = taskApiErrorMessage(error.statusCode)
                    } else if (tasksById.isEmpty()) {
                        taskNotice.text = "Tasks API unavailable. Chat still works in the Chat tab."
                    }
                    taskRefreshInFlight = false
                }
            }
        }
    }

    private fun renderSavedTaskPlaceholders() {
        for (id in api.savedTaskIds()) {
            if (!tasksById.containsKey(id)) {
                tasksById[id] = JSONObject().put("id", id).put("prompt", "Loading saved task…").put("status", "queued")
            }
        }
        renderTasks()
    }

    private fun renderTasks() {
        taskList.removeAllViews()
        if (tasksById.isEmpty()) {
            taskList.addView(TextView(this).apply {
                text = "No tasks yet."
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }
        tasksById.values.toList().asReversed().forEach { task ->
            val id = task.optString("id")
            val state = task.optString("status", "queued").lowercase()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.rgb(22, 29, 40))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.argb(80, 0, 190, 255))
            }
            }
            val heading = TextView(this).apply {
                text = "Task $id\nStatus: $state"
                textSize = 12f
                setTextColor(if (state == "failed") Color.rgb(255, 120, 120) else Color.rgb(0, 210, 255))
                setPadding(0, 0, 0, dp(5))
            }
            val prompt = TextView(this).apply {
                text = task.optString("prompt", "")
                textSize = 13f
                setTextColor(Color.WHITE)
                setTextIsSelectable(true)
            }
            val resultValue = task.opt("result")
            val resultText = when (resultValue) {
                null, JSONObject.NULL -> ""
                is JSONObject -> resultValue.toString(2)
                else -> resultValue.toString()
            }
            card.addView(heading)
            card.addView(prompt)
            if (resultText.isNotBlank()) {
                val resultScroll = ScrollView(this).apply {
                    isFillViewport = false
                    addView(TextView(this@MainActivity).apply {
                        text = resultText
                        textSize = 12f
                        setTextColor(Color.LTGRAY)
                        setTextIsSelectable(true)
                        setPadding(0, dp(6), 0, dp(6))
                    }, FrameLayout.LayoutParams(-1, -2))
                }
                card.addView(resultScroll, LinearLayout.LayoutParams(-1, dp(110)))
                extractHttpsLinks(resultText).forEach { link ->
                    card.addView(Button(this).apply {
                        text = "Open artifact: " + link.take(58)
                        setOnClickListener { openHttpsLink(link) }
                    }, LinearLayout.LayoutParams(-1, dp(40)))
                }
            }
            if (state == "queued" || state == "running") {
                card.addView(Button(this).apply {
                    text = "CANCEL TASK"
                    setOnClickListener { cancelTask(id) }
                }, LinearLayout.LayoutParams(-1, dp(40)))
            }
            val errorCode = task.optString("errorCode")
            if (errorCode.isNotBlank() && errorCode != "null") {
                card.addView(TextView(this).apply {
                    text = "Error: $errorCode"
                    setTextColor(Color.rgb(255, 150, 150))
                    textSize = 12f
                })
            }
            taskList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun renderFiles() {
        if (!::filesList.isInitialized) return
        filesList.removeAllViews()
        val links = tasksById.values.flatMap { task ->
            val result = task.opt("result")
            val text = when (result) {
                null, JSONObject.NULL -> ""
                is JSONObject -> result.toString(2)
                else -> result.toString()
            }
            extractHttpsLinks(text).map { link -> Triple(task.optString("id"), task.optString("prompt", "Task artifact"), link) }
        }.distinctBy { it.third }
        if (links.isEmpty()) {
            filesNotice.text = "No HTTPS file or screenshot links found in saved task results yet."
            return
        }
        filesNotice.text = "${links.size} link(s) found. Private artifacts may require GitHub sign-in."
        links.forEach { (taskId, prompt, link) ->
            val screenshot = isScreenshotLink(link)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.rgb(16, 22, 32))
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.argb(80, 0, 190, 255))
                }
            }
            card.addView(TextView(this).apply {
                text = if (screenshot) "SCREENSHOT • $prompt" else "FILE / LINK • $prompt"
                textSize = 11f
                setTextColor(if (screenshot) Color.rgb(100, 220, 255) else Color.WHITE)
            })
            card.addView(TextView(this).apply {
                text = "Task $taskId\n$link"
                textSize = 10f
                setTextColor(Color.LTGRAY)
                setTextIsSelectable(true)
            })
            card.addView(Button(this).apply {
                text = if (screenshot) "OPEN SCREENSHOT" else "OPEN FILE / LINK"
                contentDescription = if (screenshot) "Open screenshot from task $taskId" else "Open file link from task $taskId"
                setOnClickListener { openHttpsLink(link) }
            }, LinearLayout.LayoutParams(-1, dp(38)))
            filesList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    private fun isScreenshotLink(link: String): Boolean {
        val path = try { Uri.parse(link).path.orEmpty().lowercase(Locale.ROOT) } catch (_: Exception) { "" }
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".webp") || path.endsWith(".gif") || link.contains("screenshot", ignoreCase = true)
    }

    private fun cancelTask(id: String) {
        io.execute {
            try {
                val task = api.cancelTask(id)
                runOnUiThread {
                    tasksById[id] = task
                    renderTasks()
                    renderFiles()
                    taskNotice.text = if (task.optString("status") == "cancelled") "Task cancelled." else "Task is no longer cancellable."
                }
            } catch (error: Exception) {
                runOnUiThread {
                    taskNotice.text = if (error is VyomApiClient.TaskApiException) taskApiErrorMessage(error.statusCode)
                    else "Could not cancel task. Chat is still available."
                }
            }
        }
    }

    private fun extractHttpsLinks(text: String): List<String> {
        val pattern = Regex("""https://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        return pattern.findAll(text).map { it.value.trimEnd('.', ',', ')', ']', '}') }.distinct().toList()
    }

    private fun openHttpsLink(value: String) {
        val uri = try { Uri.parse(value) } catch (_: Exception) { return }
        if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {
                taskNotice.text = "Could not open this artifact link."
            }
        }
    }

    private fun taskApiErrorMessage(code: Int): String = when (code) {
        401 -> "Tasks session expired (401). Dobara pair karo. Chat fallback alag se available hai."
        503 -> "Tasks service temporarily unavailable (503). Thodi der baad retry karo; Chat available hai."
        else -> "Tasks API error ($code). Chat available hai."
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        tasksTabVisible = false
        mainHandler.removeCallbacks(refreshTasksRunnable)
        stopVoiceInput(cancel = true)
        if (::orb.isInitialized) orb.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::orb.isInitialized) orb.onResume()
        if (tasksTabVisible) {
            mainHandler.removeCallbacks(refreshTasksRunnable)
            mainHandler.post(refreshTasksRunnable)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        stopVoiceInput(cancel = true)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        io.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 701
        private const val TASK_POLL_MS = 4_000L
        private const val SESSION_EXPIRED_MESSAGE = "Session expired, dobara pair karo."
    }
}
