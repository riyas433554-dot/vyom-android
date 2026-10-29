package com.hermes.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.ContentValues
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.MediaStore
import android.provider.DocumentsContract
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
import android.view.WindowManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.media.AudioRecord
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
    private lateinit var chatMessageList: LinearLayout
    private lateinit var transcriptScroll: ScrollView
    private lateinit var loadOlderMessagesButton: Button
    private lateinit var jumpToLatestButton: Button
    private var renderedMessageCount = 50
    private var loadingOlderMessages = false
    private var preserveOlderScroll = false
    private var chatWasAtBottom = true
    private lateinit var memoryStore: VyomMemoryStore
    private lateinit var taskInput: EditText
    private lateinit var taskList: LinearLayout
    private lateinit var taskScroll: ScrollView
    private lateinit var taskNotice: TextView
    private lateinit var homePanel: LinearLayout
    private lateinit var pairButton: Button
    private lateinit var sendButton: Button
    private lateinit var createTaskButton: Button
    private lateinit var chatTab: TextView
    private lateinit var tasksTab: TextView
    private lateinit var settingsTab: TextView
    private lateinit var filesTab: TextView
    private lateinit var controlTab: TextView
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
    private lateinit var rollbackButton: Button
    private lateinit var personaButton: Button
    private lateinit var settingsContent: LinearLayout
    private var updateManifest: JSONObject? = null
    private var pendingUpdateApk: File? = null
    private var pendingDocumentSource = "app"
    private var pendingDocumentCallback: ((String) -> Unit)? = null
    private var updateCheckInFlight = false
    private lateinit var voiceButton: Button
    private lateinit var micIndicator: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var liveSttCapture: LiveSttCapture? = null
    private var textToSpeech: TextToSpeech? = null
    private var voiceRepliesEnabled = false
    private var hinglishPersona = true
    private var listening = false
    private var voiceLoopEnabled = false
    private var controlKillSwitch = false
    private var voiceActionAckInFlight = false
    private var voiceStopReceiverRegistered = false
    private val voiceStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VoiceForegroundService.ACTION_SESSION_STOP -> if (voiceLoopEnabled) toggleVoiceInput()
                VoiceForegroundService.ACTION_SESSION_MUTE -> if (voiceLoopEnabled) toggleVoiceMuteFromNotification()
                VoiceForegroundService.ACTION_SESSION_CONTROL_STOP -> {
                    controlKillSwitch = true
                    if (::controlNotice.isInitialized) controlNotice.text = "STOP CONTROL is ON. Device actions are disabled."
                }
            }
        }
    }
    private var voiceMuted = false
    private var latestPartialSpeech = ""
    private var silenceSubmitRunnable: Runnable? = null
    private var silenceSubmissionHandled = false
    private var voiceDialog: AlertDialog? = null
    private var liveVoiceOrb: OrbSurfaceView? = null
    private var liveStateLabel: TextView? = null
    private var liveCaptionView: TextView? = null
    private var liveCaptionsEnabled = false
    private lateinit var voiceTranscript: TextView
    private lateinit var voiceStateMachine: VoiceStateMachine
    private var voiceTtsProvider: TtsProvider? = null
    private var voiceUiSounds: VoiceUiSounds? = null
    private var liveLaneEnabled = false
    private var liveGenerationActive = false
    private var liveReplySpeaking = false
    private val liveSentenceQueue = java.util.ArrayDeque<String>()
    private var liveSentenceTtsPlaying = false
    private var liveStreamFinished = false
    private lateinit var elevenLabsKeyInput: EditText
    private lateinit var elevenLabsVoiceIdInput: EditText
    private lateinit var nvidiaApiKeyInput: EditText
    private lateinit var liveModelInput: EditText
    private lateinit var liveModelNotice: TextView
    private lateinit var sttLanguageInput: EditText
    private var nemotronLiveClient: NemotronLiveClient? = null
    private lateinit var ttsConfigStatus: TextView
    private lateinit var api: VyomApiClient
    private val io = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val settingsPrefs by lazy { getSharedPreferences("vyom_settings", MODE_PRIVATE) }
    private val controlPrefs by lazy { getSharedPreferences("vyom_control_audit", MODE_PRIVATE) }
    private var tasksTabVisible = false
    private var orbMode = 0
    private var reduceMotion = false
    private var liteMode = false
    private var mainPanel: LinearLayout? = null
    private var dockBar: LinearLayout? = null
    private var taskRefreshInFlight = false
    private val tasksById = linkedMapOf<String, JSONObject>()
    private var orbitHomeView: OrbitHomeView? = null
    private data class ChatMessage(val id: String, val text: String, var state: String, var reply: String = "", val createdAt: Long = System.currentTimeMillis())
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
        DeviceCommandConfig.initialize(applicationContext)
        val voiceStopFilter = IntentFilter().apply { addAction(VoiceForegroundService.ACTION_SESSION_STOP); addAction(VoiceForegroundService.ACTION_SESSION_MUTE); addAction(VoiceForegroundService.ACTION_SESSION_CONTROL_STOP) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(voiceStopReceiver, voiceStopFilter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(voiceStopReceiver, voiceStopFilter)
        voiceStopReceiverRegistered = true
        api = VyomApiClient(applicationContext)
        memoryStore = VyomMemoryStore(applicationContext)
        controlKillSwitch = controlPrefs.getBoolean("kill_switch", false)
        voiceStateMachine = VoiceStateMachine()
        orbMode = settingsPrefs.getInt("orb_mode", 0).coerceIn(0, 3)
        reduceMotion = settingsPrefs.getBoolean("reduce_motion", false)
        liteMode = settingsPrefs.getBoolean("lite_mode", false)
        voiceRepliesEnabled = settingsPrefs.getBoolean("voice_replies", false)
        hinglishPersona = settingsPrefs.getBoolean("hinglish_persona", true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this).apply {
            setBackgroundColor(VyomTheme.BACKGROUND)
            fitsSystemWindows = true
        }
        orb = OrbSurfaceView(this).apply {
            isClickable = false
            isFocusable = false
            setOrbMode(orbMode)
            setReduceMotion(reduceMotion)
            setLiteMode(liteMode)
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
                setColor(Color.argb(238, 11, 15, 26))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.argb(150, 0, 190, 255))
            }
            elevation = dp(8).toFloat()
        }
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(224, 16, 25, 43))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
            }
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }
        dockBar = tabBar
        mainPanel = panel
        fun dockItem(label: String, action: () -> Unit): TextView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label == "◉") 22f else 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
            setOnClickListener { action() }
        }
        chatTab = dockItem("Chat") { showTab(false) }
        tasksTab = dockItem("Tasks") { showTab(true) }
        settingsTab = dockItem("Profile") { showProfileMenu() }
        filesTab = dockItem("Files") { showTab(false, false, true) }
        controlTab = dockItem("Control") { showTab(false, false, false, true) }
        val homeDock = dockItem("Home") { showHome() }
        val voiceDock = dockItem("◉") { toggleVoiceInput() }
        listOf(homeDock, chatTab, voiceDock, tasksTab, settingsTab).forEachIndexed { index, item ->
            tabBar.addView(item, LinearLayout.LayoutParams(0, dp(if (index == 2) 48 else 42), 1f))
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

        chatPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        transcript = TextView(this).apply {
            text = "Hermes chat will appear here."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
            setTextIsSelectable(true)
        }
        chatMessageList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(12))
        }
        transcriptScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(chatMessageList, FrameLayout.LayoutParams(-1, -2))
        }
        chatMessageList.addView(transcript, LinearLayout.LayoutParams(-1, -2))
        val chatInputLabel = TextView(this).apply { text = "CHAT MESSAGE"; setTextColor(Color.LTGRAY); textSize = 10f }
        chatInput = EditText(this).apply {
            id = View.generateViewId()
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            hint = "Message Vyom…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 15f
            minLines = 1
            maxLines = 5
            isSingleLine = false
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.rgb(20, 29, 45))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.argb(100, 120, 150, 190))
            }
        }
        sendButton = Button(this).apply {
            text = "SEND"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        loadOlderMessagesButton = Button(this).apply {
            text = "LOAD OLDER MESSAGES"
            visibility = View.GONE
            setOnClickListener { loadOlderChatMessages() }
        }
        jumpToLatestButton = Button(this).apply {
            text = "↓ JUMP TO LATEST"
            visibility = View.GONE
            setOnClickListener { transcriptScroll.post { transcriptScroll.fullScroll(View.FOCUS_DOWN) } }
        }
        transcriptScroll.setOnScrollChangeListener { _, scrollY, _, _, _ ->
            val child = transcriptScroll.getChildAt(0)
            val atBottom = child == null || scrollY + transcriptScroll.height >= child.height - dp(32)
            chatWasAtBottom = atBottom
            jumpToLatestButton.visibility = if (atBottom) View.GONE else View.VISIBLE
            if (scrollY <= dp(24) && !loadingOlderMessages && chatMessages.size > renderedMessageCount) loadOlderChatMessages()
        }
        chatPanel.addView(loadOlderMessagesButton, LinearLayout.LayoutParams(-1, dp(38)))
        chatPanel.addView(jumpToLatestButton, LinearLayout.LayoutParams(-1, dp(38)))
        chatPanel.addView(transcriptScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        chatPanel.addView(chatInput, LinearLayout.LayoutParams(-1, -2))
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
        val liteModeButton = Button(this).apply {
            text = if (liteMode) "LITE MODE: ON" else "LITE MODE: OFF"
            contentDescription = "Toggle Lite mode to reduce particle and glow effects"
            setOnClickListener {
                liteMode = !liteMode
                settingsPrefs.edit().putBoolean("lite_mode", liteMode).apply()
                text = if (liteMode) "LITE MODE: ON" else "LITE MODE: OFF"
                if (::orb.isInitialized) orb.setLiteMode(liteMode)
                orbitHomeView?.setLiteMode(liteMode)
                status.text = if (liteMode) "VYOM  •  LITE MODE ON" else "VYOM  •  LITE MODE OFF"
            }
        }
        settingsContent.addView(liteModeButton, LinearLayout.LayoutParams(-1, dp(44)))
        voiceRepliesButton = Button(this).apply {
            text = if (voiceRepliesEnabled) "VOICE REPLIES: ON" else "VOICE REPLIES: OFF"
            contentDescription = "Toggle spoken replies"
            setOnClickListener { setVoiceRepliesEnabled(!voiceRepliesEnabled) }
        }
        settingsContent.addView(voiceRepliesButton, LinearLayout.LayoutParams(-1, dp(44)))
        settingsContent.addView(TextView(this).apply { text = "ELEVENLABS VOICE (OPTIONAL)"; textSize = 11f; setTextColor(Color.LTGRAY) })
        elevenLabsKeyInput = EditText(this).apply {
            hint = "ElevenLabs API key"; setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f
            setText(secureVoicePrefs().getString("elevenlabs_api_key", "") ?: "")
        }
        elevenLabsVoiceIdInput = EditText(this).apply {
            hint = "ElevenLabs Voice ID"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f
            setText(secureVoicePrefs().getString("elevenlabs_voice_id", "") ?: "")
        }
        settingsContent.addView(elevenLabsKeyInput, LinearLayout.LayoutParams(-1, dp(44)))
        settingsContent.addView(elevenLabsVoiceIdInput, LinearLayout.LayoutParams(-1, dp(44)))
        nvidiaApiKeyInput = EditText(this).apply {
            hint = "NVIDIA API key (encrypted on device; LIVE lane)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f
            setText(secureVoicePrefs().getString("nvidia_api_key", "") ?: "")
        }
        settingsContent.addView(nvidiaApiKeyInput, LinearLayout.LayoutParams(-1, dp(44)))
        liveModelInput = EditText(this).apply {
            hint = "LIVE model ID (load and choose below)"
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f
            setText(LiveModelPolicy.selectedOrDefault(secureVoicePrefs().getString("nvidia_live_model_id", "")))
        }
        settingsContent.addView(liveModelInput, LinearLayout.LayoutParams(-1, dp(44)))
        settingsContent.addView(Button(this).apply {
            text = "LOAD MODELS"
            setOnClickListener { loadNvidiaModels() }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        liveModelNotice = TextView(this).apply {
            text = "Load models using your NVIDIA API key, then tap a model ID to select it."
            textSize = 11f; setTextColor(Color.LTGRAY)
        }
        settingsContent.addView(liveModelNotice, LinearLayout.LayoutParams(-1, -2))
        sttLanguageInput = EditText(this).apply {
            hint = "STT language code (blank = Auto, e.g. en)"
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f
            setText(secureVoicePrefs().getString("stt_language_code", "") ?: "")
        }
        settingsContent.addView(sttLanguageInput, LinearLayout.LayoutParams(-1, dp(44)))
        settingsContent.addView(Button(this).apply {
            text = "SAVE VOICE SETTINGS"
            setOnClickListener {
                secureVoicePrefs().edit().putString("elevenlabs_api_key", elevenLabsKeyInput.text.toString().trim())
                    .putString("elevenlabs_voice_id", elevenLabsVoiceIdInput.text.toString().trim()).apply()
                secureVoicePrefs().edit().putString("nvidia_api_key", nvidiaApiKeyInput.text.toString().trim())
                    .putString("nvidia_live_model_id", liveModelInput.text.toString().trim()).apply()
                secureVoicePrefs().edit().putString("stt_language_code", sttLanguageInput.text.toString().trim()).apply()
                liveModelNotice.text = if (liveModelInput.text.toString().trim().isBlank()) "Choose a LIVE model before enabling LIVE lane." else "Selected LIVE model: " + liveModelInput.text.toString().trim()
                ttsConfigStatus.text = voiceProvider().status()
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        ttsConfigStatus = TextView(this).apply {
            text = if ((secureVoicePrefs().getString("elevenlabs_api_key", "") ?: "").isBlank() ||
                (secureVoicePrefs().getString("elevenlabs_voice_id", "") ?: "").isBlank()) "ElevenLabs not configured — using Android TTS" else "ElevenLabs voice configured"
            textSize = 11f; setTextColor(Color.LTGRAY)
        }
        settingsContent.addView(ttsConfigStatus, LinearLayout.LayoutParams(-1, -2))
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

        settingsContent.addView(TextView(this).apply {
            text = "CONTROLLED SELF-MODIFICATION"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }, LinearLayout.LayoutParams(-1, dp(24)))
        val githubProposalTokenInput = EditText(this).apply {
            hint = "GitHub token: Contents + Pull requests write (encrypted on device)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 13f
            setText(secureVoicePrefs().getString("github_proposal_token", "").orEmpty())
        }
        settingsContent.addView(githubProposalTokenInput, LinearLayout.LayoutParams(-1, dp(44)))
        val selfChangeNotice = TextView(this).apply {
            text = "Self-change is proposal-only: allowed Kotlin file → separate branch → PR. No auto-merge or auto-install."
            textSize = 10f
            setTextColor(Color.LTGRAY)
        }
        settingsContent.addView(Button(this).apply {
            text = "SAVE GITHUB TOKEN"
            setOnClickListener {
                secureVoicePrefs().edit().putString("github_proposal_token", githubProposalTokenInput.text.toString().trim()).apply()
                selfChangeNotice.text = "GitHub token saved encrypted on this device. It is never written to the repository."
            }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        settingsContent.addView(selfChangeNotice, LinearLayout.LayoutParams(-1, -2))
        settingsContent.addView(Button(this).apply {
            text = "PROPOSE SELF-CHANGE"
            setOnClickListener {
                val form = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(8), dp(18), dp(4))
                }
                val proposalTitle = EditText(this@MainActivity).apply {
                    hint = "Change title (8–120 characters)"
                    setSingleLine(true)
                }
                val proposalPath = EditText(this@MainActivity).apply {
                    hint = "Kotlin source path"
                    setSingleLine(true)
                    setText("app/src/main/java/com/hermes/assistant/NemotronLiveClient.kt")
                }
                val proposalContent = EditText(this@MainActivity).apply {
                    hint = "Complete proposed Kotlin file content"
                    minLines = 6
                    maxLines = 12
                    gravity = Gravity.TOP or Gravity.START
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }
                form.addView(proposalTitle, LinearLayout.LayoutParams(-1, dp(48)))
                form.addView(proposalPath, LinearLayout.LayoutParams(-1, dp(48)))
                form.addView(proposalContent, LinearLayout.LayoutParams(-1, dp(180)))
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Create review-only proposal")
                    .setMessage("Only non-policy Kotlin source files are accepted. GitHub will run CI on the PR; you must review and install manually.")
                    .setView(form)
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("CREATE PR") { _, _ ->
                        val title = proposalTitle.text.toString().trim()
                        val path = proposalPath.text.toString().trim()
                        val source = proposalContent.text.toString()
                        val token = secureVoicePrefs().getString("github_proposal_token", "").orEmpty()
                        selfChangeNotice.text = "Validating proposal and opening a separate review branch…"
                        io.execute {
                            try {
                                val proposal = SelfModificationClient().createProposal(token, title, path, source)
                                runOnUiThread {
                                    logControlAction("Self-change proposal: $path", "PR created: ${proposal.pullRequestUrl}", "app")
                                    selfChangeNotice.text = "PR created; waiting for CI and your manual review: ${proposal.pullRequestUrl}"
                                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(proposal.pullRequestUrl))) } catch (_: Exception) {}
                                }
                            } catch (error: Exception) {
                                runOnUiThread {
                                    logControlAction("Self-change proposal: $path", "rejected/failed: ${error.message ?: "unknown error"}", "app")
                                    selfChangeNotice.text = "Proposal failed: ${error.message ?: "GitHub error"}"
                                }
                            }
                        }
                    }
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        rollbackButton = Button(this).apply {
            text = "OPEN PREVIOUS APK (ROLLBACK)"
            visibility = if (settingsPrefs.getString("previous_apk_uri", null).isNullOrBlank()) View.GONE else View.VISIBLE
            setOnClickListener {
                val savedUri = settingsPrefs.getString("previous_apk_uri", null)?.let { Uri.parse(it) }
                if (savedUri == null) {
                    updateNotice.text = "No previous APK backup is available."
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Rollback to previous Vyom APK?")
                        .setMessage("The previous APK is retained in Downloads/VyomRollback. Android may reject a downgrade over the newer version. If it does, save any data, uninstall Vyom manually, then open the retained APK from Downloads and install it. Vyom will not uninstall itself.")
                        .setNegativeButton("CANCEL", null)
                        .setPositiveButton("VERIFY & OPEN INSTALLER") { _, _ ->
                            updateNotice.text = "Verifying previous APK checksum…"
                            io.execute {
                                try {
                                    val expected = settingsPrefs.getString("previous_apk_sha256", "").orEmpty()
                                    if (!expected.matches(Regex("^[a-f0-9]{64}$"))) throw IllegalStateException("Saved rollback checksum is missing")
                                    val actual = sha256Uri(savedUri)
                                    if (!MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())) {
                                        throw IllegalStateException("Rollback APK checksum mismatch; do not install it")
                                    }
                                    runOnUiThread {
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(savedUri, "application/vnd.android.package-archive")
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                                            logControlAction("Rollback to previous APK", "SHA-256 verified; Android installer opened; user must confirm", "app")
                                            updateNotice.text = "Previous APK checksum verified. Android installer opened; approve manually."
                                        } catch (error: Exception) {
                                            updateNotice.text = "Could not open rollback APK: ${error.message ?: "installer error"}"
                                        }
                                    }
                                } catch (error: Exception) {
                                    runOnUiThread {
                                        logControlAction("Rollback to previous APK", "rejected: ${error.message ?: "verification failed"}", "app")
                                        updateNotice.text = "Rollback rejected: ${error.message ?: "verification failed"}"
                                    }
                                }
                            }
                        }.show()
                }
            }
        }
        settingsContent.addView(rollbackButton, LinearLayout.LayoutParams(-1, dp(44)))
        val settingsScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(settingsContent, FrameLayout.LayoutParams(-1, -2))
        }
        settingsPanel.addView(settingsScroll, LinearLayout.LayoutParams(-1, 0, 1f))


        // Build metadata remains available outside the primary navigation.
        panel.addView(tabBar, LinearLayout.LayoutParams(-1, dp(52)))
        panel.addView(pairingInput, LinearLayout.LayoutParams(-1, dp(46)))
        panel.addView(pairButton, LinearLayout.LayoutParams(-1, dp(44)))
        homePanel = buildHomePanel()
        panel.addView(homePanel, LinearLayout.LayoutParams(-1, dp(250)))
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
        controlPanel.addView(Button(this).apply {
            text = "STOP CONTROL"
            contentDescription = "Emergency stop for all Vyom device actions"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(170, 25, 35))
            setOnClickListener {
                controlKillSwitch = true
                controlPrefs.edit().putBoolean("kill_switch", true).apply()
                logControlAction("STOP CONTROL", "enabled", "app")
                controlNotice.text = "STOP CONTROL is ON. Device actions are disabled until resumed."
                AlertDialog.Builder(this@MainActivity).setTitle("Control stopped")
                    .setMessage("All device actions are disabled. Resume only when safe.")
                    .setPositiveButton("RESUME CONTROL") { _, _ ->
                        controlKillSwitch = false
                        controlPrefs.edit().putBoolean("kill_switch", false).apply()
                        logControlAction("RESUME CONTROL", "enabled", "app")
                        controlNotice.text = "Device actions resumed."
                    }.setNegativeButton("Keep stopped", null).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        controlPanel.addView(controlNotice, LinearLayout.LayoutParams(-1, -2))
        controlPanel.addView(controlButtons, LinearLayout.LayoutParams(-1, dp(48)))
        controlPanel.addView(Button(this).apply {
            text = "VYOM APP SETTINGS"
            setOnClickListener { confirmControlAction("Open Vyom app settings", "app-details") }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        controlPanel.addView(Button(this).apply {
            text = "ACCESSIBILITY SETTINGS"
            contentDescription = "Open Android accessibility settings; enabling Vyom is optional and manual"
            setOnClickListener { performControlAction("Open Android accessibility settings", Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        controlPanel.addView(TextView(this).apply {
            text = "Accessibility is optional and must be enabled by you in Android Settings. Vyom's service does not read screen text or tap controls."
            textSize = 10f
            setTextColor(Color.LTGRAY)
        }, LinearLayout.LayoutParams(-1, -2))
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

        val orbitView = OrbitHomeView(this)
        orbitHomeView = orbitView
        orbitView.setAccentColor(VyomTheme.accent(this, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
        orbitView.setLiteMode(liteMode)
        root.addView(orbitView, 0, FrameLayout.LayoutParams(-1, -1))
        root.addView(orb, FrameLayout.LayoutParams(1, 1, Gravity.CENTER))
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(12)
        })
        root.addView(buildInfo, FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(30)
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
        applyThemeAccent()
        showHome()
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

    private fun buildHomePanel(): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(8))
        }
        content.addView(TextView(this).apply {
            text = "Good to see you"
            textSize = 21f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "What should we work on?"
            textSize = 12f
            setTextColor(Color.rgb(160, 181, 207))
            setPadding(0, dp(2), 0, dp(8))
        }, LinearLayout.LayoutParams(-1, -2))

        val askRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.rgb(18, 29, 47))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(150, 56, 189, 248))
            }
            setPadding(dp(10), dp(2), dp(4), dp(2))
        }
        val askInput = EditText(this).apply {
            hint = "Ask Vyom anything…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(130, 150, 176))
            textSize = 13f
            maxLines = 2
            minLines = 1
            isSingleLine = false
            background = null
            setPadding(0, dp(4), dp(4), dp(4))
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        }
        val askButton = TextView(this).apply {
            text = "➤"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
            contentDescription = "Send prompt to Vyom chat"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                val prompt = askInput.text.toString().trim()
                if (prompt.isNotEmpty()) {
                    chatInput.setText(prompt)
                    askInput.setText("")
                    showTab(false)
                    sendMessage()
                }
            }
        }
        askRow.addView(askInput, LinearLayout.LayoutParams(0, -2, 1f))
        askRow.addView(askButton, LinearLayout.LayoutParams(-2, -2))
        content.addView(askRow, LinearLayout.LayoutParams(-1, -2))

        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        listOf("Build something", "Explain this", "Plan a task").forEach { label ->
            val chip = TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(194, 218, 243))
                setPadding(dp(9), dp(8), dp(9), dp(8))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.rgb(19, 34, 54))
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.rgb(42, 66, 94))
                }
                setOnClickListener {
                    askInput.setText(when (label) {
                        "Build something" -> "Help me build an Android feature"
                        "Explain this" -> "Explain this step by step"
                        else -> "Plan a task and break it into steps"
                    })
                    askInput.setSelection(askInput.text.length)
                    askInput.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(askInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
            chipRow.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginEnd = dp(4)
            })
        }
        content.addView(chipRow, LinearLayout.LayoutParams(-1, -2))

        content.addView(TextView(this).apply {
            text = "QUICK ACCESS"
            textSize = 9f
            letterSpacing = 0.14f
            setTextColor(Color.rgb(122, 148, 180))
            setPadding(0, dp(3), 0, dp(5))
        }, LinearLayout.LayoutParams(-1, -2))
        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun quickCard(title: String, subtitle: String, action: () -> Unit): TextView =
            TextView(this).apply {
                text = "$title\n$subtitle"
                textSize = 11f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                setPadding(dp(10), dp(8), dp(8), dp(8))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.rgb(15, 25, 42))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.rgb(35, 54, 78))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        quickRow.addView(quickCard("Tasks", "View progress") { showTab(true) },
            LinearLayout.LayoutParams(0, dp(54), 1f).apply { marginEnd = dp(5) })
        quickRow.addView(quickCard("Chat history", "Continue a reply") { showTab(false) },
            LinearLayout.LayoutParams(0, dp(54), 1f).apply { marginEnd = dp(5) })
        quickRow.addView(quickCard("Profile", "Themes & settings") { showProfileMenu() },
            LinearLayout.LayoutParams(0, dp(54), 1f))
        content.addView(quickRow, LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content, FrameLayout.LayoutParams(-1, -2))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
    }

    private fun showHome() {
        setChatLayout(fullHeight = false)
        tasksTabVisible = false
        homePanel.visibility = View.VISIBLE
        chatPanel.visibility = View.GONE
        tasksPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        filesPanel.visibility = View.GONE
        controlPanel.visibility = View.GONE
        orbitHomeView?.visibility = View.VISIBLE
        chatTab.alpha = 0.65f
        tasksTab.alpha = 0.65f
        settingsTab.alpha = 0.65f
        mainHandler.removeCallbacks(refreshTasksRunnable)
    }

    private fun setChatLayout(fullHeight: Boolean) {
        val panel = mainPanel ?: return
        val frameParams = panel.layoutParams as? FrameLayout.LayoutParams ?: return
        frameParams.height = if (fullHeight) FrameLayout.LayoutParams.MATCH_PARENT else dp(440)
        frameParams.gravity = Gravity.BOTTOM
        panel.layoutParams = frameParams
        chatPanel.layoutParams = if (fullHeight) {
            LinearLayout.LayoutParams(-1, 0, 1f)
        } else {
            LinearLayout.LayoutParams(-1, dp(250))
        }
        panel.requestLayout()
    }

    private fun showProfileMenu() {
        AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(arrayOf("Settings", "Files", "Control", "Skills", "Fix suggestions", "Memory")) { _, which ->
                when (which) {
                    0 -> showTab(false, true)
                    1 -> showTab(false, false, true)
                    2 -> showTab(false, false, false, true)
                    3 -> showSkillsRegistry()
                    4 -> showFixSuggestions()
                    5 -> showMemoryManager()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMemoryManager() {
        val enabled = memoryStore.isEnabled()
        val editor = EditText(this).apply {
            setText(memoryStore.facts().joinToString("\n"))
            hint = "One safe fact per line (name, preferences)"
            minLines = 5; maxLines = 12
            gravity = Gravity.TOP or Gravity.START
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(TextView(this@MainActivity).apply {
                text = "Encrypted on this device. Passwords, OTPs, payment-card data, API keys, and tokens are never saved."
                textSize = 12f; setTextColor(Color.LTGRAY)
            })
            addView(editor, LinearLayout.LayoutParams(-1, dp(180)))
        }
        AlertDialog.Builder(this).setTitle("Profile • Memory").setView(wrap)
            .setNeutralButton(if (enabled) "Turn OFF" else "Turn ON") { _, _ ->
                memoryStore.setEnabled(!enabled); showMemoryManager()
            }
            .setPositiveButton("Save") { _, _ ->
                memoryStore.replaceFacts(editor.text.toString())
                logControlAction("Memory facts", "updated", "profile")
            }
            .setNegativeButton("Delete all") { _, _ ->
                memoryStore.clearFacts()
                logControlAction("Memory facts", "deleted", "profile")
            }.show()
    }

    private fun loadOlderChatMessages() {
        if (loadingOlderMessages || renderedMessageCount >= chatMessages.size) return
        loadingOlderMessages = true
        val oldHeight = chatMessageList.height
        renderedMessageCount = minOf(chatMessages.size, renderedMessageCount + 50)
        preserveOlderScroll = true
        renderChatMessages()
        transcriptScroll.post {
            transcriptScroll.scrollTo(0, (chatMessageList.height - oldHeight).coerceAtLeast(0))
            loadingOlderMessages = false
            preserveOlderScroll = false
        }
    }

    private fun showSkillsRegistry() {
        val ids = arrayOf("open_app", "volume", "torch", "settings")
        val labels = arrayOf("Open apps", "Media volume", "Torch", "System settings panels")
        val prefs = getSharedPreferences("vyom_skill_registry", MODE_PRIVATE)
        val checked = BooleanArray(ids.size) { i -> prefs.getBoolean(ids[i], true) }
        AlertDialog.Builder(this)
            .setTitle("Skills • On this device")
            .setMultiChoiceItems(labels, checked) { _, which, enabled ->
                prefs.edit().putBoolean(ids[which], enabled).apply()
                logControlAction("Skill: " + labels[which], if (enabled) "enabled" else "disabled", "profile")
            }
            .setPositiveButton("Done", null)
            .setNeutralButton("Teach Vyom") { _, _ -> showTeachCommandDialog("new taught command", "profile") { message ->
                AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show()
            } }
            .show()
    }

    private fun showFixSuggestions() {
        val rows = try { JSONArray(controlPrefs.getString("events", "[]")) } catch (_: Exception) { JSONArray() }
        val suggestions = ArrayList<String>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val result = row.optString("result")
            if (result.contains("unrecognized", true) || result.contains("couldn't map", true) ||
                result.contains("no matching", true) || result.contains("could not", true) ||
                result.contains("error", true) || result.contains("failed", true)) {
                suggestions.add("• " + row.optString("action") + " — " + result)
            }
        }
        val message = if (suggestions.isEmpty()) "No failed command mappings recorded yet. Unknown commands will appear here when logged." else suggestions.take(12).joinToString("\n")
        AlertDialog.Builder(this).setTitle("Fix suggestions • proposals only")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isSkillEnabled(action: ActionEngine.Intent): Boolean {
        val id = when (action.operation) {
            ActionEngine.Operation.OPEN_APP -> "open_app"
            ActionEngine.Operation.VOLUME_DELTA, ActionEngine.Operation.VOLUME_SET,
            ActionEngine.Operation.VOLUME_MUTE, ActionEngine.Operation.VOLUME_UNMUTE -> "volume"
            ActionEngine.Operation.TORCH -> "torch"
            ActionEngine.Operation.OPEN_SETTINGS -> "settings"
            else -> return true
        }
        return getSharedPreferences("vyom_skill_registry", MODE_PRIVATE).getBoolean(id, true)
    }

    private fun showTab(showTasks: Boolean, showSettings: Boolean = false, showFiles: Boolean = false, showControl: Boolean = false) {
        val anyOtherPanel = showTasks || showSettings || showFiles || showControl
        setChatLayout(fullHeight = !anyOtherPanel)
        tasksTabVisible = showTasks || showFiles
        homePanel.visibility = View.GONE
        orbitHomeView?.visibility = View.GONE
        chatPanel.visibility = if (anyOtherPanel) View.GONE else View.VISIBLE
        tasksPanel.visibility = if (showTasks) View.VISIBLE else View.GONE
        settingsPanel.visibility = if (showSettings) View.VISIBLE else View.GONE
        filesPanel.visibility = if (showFiles) View.VISIBLE else View.GONE
        controlPanel.visibility = if (showControl) View.VISIBLE else View.GONE
        chatTab.alpha = if (anyOtherPanel) 0.65f else 1f
        tasksTab.alpha = if (showTasks) 1f else 0.65f
        settingsTab.alpha = if (showSettings) 1f else 0.65f
        filesTab.alpha = 0f
        controlTab.alpha = 0f
        mainHandler.removeCallbacks(refreshTasksRunnable)
        if (tasksTabVisible) {
            refreshTasks()
            mainHandler.postDelayed(refreshTasksRunnable, TASK_POLL_MS)
        }
        if (showControl) renderControlAudit()
    }

    private fun confirmControlAction(label: String, action: String) {
        if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
            controlNotice.text = "STOP CONTROL is ON. Action blocked."
            logControlAction(label, "blocked-by-kill-switch", "app")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm device action")
            .setMessage("Allow Vyom to $label? This opens an Android Settings screen; it does not change settings automatically.")
            .setNegativeButton("CANCEL") { _, _ -> logControlAction(label, "cancelled") }
            .setPositiveButton("OPEN") { _, _ -> performControlAction(label, action) }
            .show()
    }

    private fun performControlAction(label: String, action: String) {
        if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
            logControlAction(label, "blocked-by-kill-switch", "app")
            return
        }
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

    private fun logControlAction(action: String, result: String, source: String = "app", elapsedMs: Long = 0L) {
        val rows = try { JSONArray(controlPrefs.getString("events", "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        next.put(JSONObject().put("at", java.time.Instant.now().toString()).put("elapsedMs", elapsedMs).put("source", source).put("action", action).put("result", result))
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
                text = "${row.optString("at")} • ${row.optString("source", "app")} • ${row.optString("action")} — ${row.optString("result")} (${row.optLong("elapsedMs")} ms)"
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
                    showChatNotice("Could not connect to Hermes. Check the pairing code and try again.")
                    pairButton.isEnabled = true
                }
            }
        }
    }

    private fun sendMessage() {
        val message = chatInput.text.toString().trim()
        if (message.isBlank() || !chatReady) return
        memoryStore.learnFrom(message)
        chatInput.setText("")
        val item = ChatMessage(UUID.randomUUID().toString(), message, "queued")
        chatMessages.add(item)
        persistChatMessages()
        renderChatMessages()
        if (dispatchDeviceAction(message, "chat") { reply ->
                item.state = "done"; item.reply = reply
                persistChatMessages(); renderChatMessages()
                status.text = if (chatMessages.any { it.state == "queued" || it.state == "running" }) "VYOM  •  THINKING" else "VYOM  •  READY"
                if (voiceRepliesEnabled && !voiceLoopEnabled) speakReply(reply)
            }) return
        status.text = "VYOM  •  QUEUED"
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
                    if (voiceLoopEnabled) { voiceStateMachine.replyReady(); if (::voiceTranscript.isInitialized) voiceTranscript.text = ""; speakReply(reply) { if (voiceLoopEnabled) { if (voiceStateMachine.state == VoiceState.SPEAKING) voiceStateMachine.speechFinished(); if (!listening) startVoiceInput() } } } else if (voiceRepliesEnabled && !voiceLoopEnabled) speakReply(reply)
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
                    value.optString("reply"),
                    value.optLong("createdAt", System.currentTimeMillis())
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
                .put("reply", item.reply)
                .put("createdAt", item.createdAt))
        }
        chatPrefs.edit().putString("messages", saved.toString()).apply()
    }

    private fun showChatNotice(message: String) {
        if (!::chatMessageList.isInitialized) return
        chatMessageList.removeAllViews()
        transcript.text = message
        transcript.visibility = View.VISIBLE
        chatMessageList.addView(transcript, LinearLayout.LayoutParams(-1, -2))
    }

    private fun renderChatMessages() {
        if (!::chatMessageList.isInitialized) return
        chatMessageList.removeAllViews()
        if (chatMessages.isEmpty()) {
            transcript.text = personaText("Hermes chat will appear here. Send a message to start.", "Hermes yahan ready hai. Chat shuru karne ke liye message bhejo.")
            transcript.visibility = View.VISIBLE
            chatMessageList.addView(transcript, LinearLayout.LayoutParams(-1, -2))
            return
        }
        val activeTask = tasksById.values.lastOrNull { task ->
            task.optString("status", "").lowercase() in setOf("running", "in_progress", "processing", "working")
        }
        if (activeTask != null) {
            val progressCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.rgb(18, 42, 59))
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
                }
            }
            progressCard.addView(TextView(this).apply {
                text = "TASK IN PROGRESS"
                textSize = 10f
                setTextColor(VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
            })
            progressCard.addView(TextView(this).apply {
                text = activeTask.optString("prompt", "Working on your task").lineSequence().firstOrNull().orEmpty()
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(0, dp(4), 0, dp(2))
            })
            progressCard.addView(TextView(this).apply {
                text = "Status: " + activeTask.optString("status", "running")
                textSize = 11f
                setTextColor(Color.LTGRAY)
            })
            chatMessageList.addView(progressCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
        val visibleMessages = chatMessages.takeLast(renderedMessageCount)
        loadOlderMessagesButton.visibility = if (chatMessages.size > visibleMessages.size) View.VISIBLE else View.GONE
        visibleMessages.forEach { item ->
            val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(item.createdAt))
            val userLabel = TextView(this).apply {
                text = "YOU  ·  $time"
                textSize = 10f
                setTextColor(Color.LTGRAY)
                setPadding(dp(5), dp(4), dp(5), dp(3))
            }
            val userBubble = TextView(this).apply {
                text = android.text.Html.fromHtml(markdownToHtml(item.text), android.text.Html.FROM_HTML_MODE_COMPACT)
                textSize = 15f
                setTextColor(Color.WHITE)
                setTextIsSelectable(true)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.rgb(31, 55, 82))
                    cornerRadius = dp(16).toFloat()
                }
            }
            chatMessageList.addView(userLabel, LinearLayout.LayoutParams(-1, -2))
            chatMessageList.addView(userBubble, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
            val stateLabel = TextView(this).apply {
                text = when (item.state) {
                    "queued" -> "QUEUED"
                    "running" -> "RUNNING · Vyom is thinking…"
                    "done" -> "VYOM · $time"
                    else -> "ERROR · $time"
                }
                textSize = 10f
                setTextColor(when (item.state) {
                    "error" -> Color.rgb(255, 130, 130)
                    "done" -> VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan")
                    else -> Color.LTGRAY
                })
                setPadding(dp(5), dp(5), dp(5), dp(3))
            }
            chatMessageList.addView(stateLabel, LinearLayout.LayoutParams(-1, -2))
            if (item.reply.isNotBlank()) {
                val replyBubble = TextView(this).apply {
                    text = android.text.Html.fromHtml(markdownToHtml(item.reply), android.text.Html.FROM_HTML_MODE_COMPACT)
                    textSize = 15f
                    setTextColor(Color.rgb(230, 237, 247))
                    setTextIsSelectable(true)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.rgb(17, 25, 39))
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(1), Color.argb(100, 100, 130, 170))
                    }
                }
                chatMessageList.addView(replyBubble, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
        if (!preserveOlderScroll) {
            val stickToBottom = chatWasAtBottom
            transcriptScroll.post {
                if (stickToBottom) transcriptScroll.fullScroll(View.FOCUS_DOWN)
                else jumpToLatestButton.visibility = View.VISIBLE
            }
        }
    }

    private fun markdownToHtml(input: String): String {
        fun escapeHtml(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        val output = StringBuilder()
        var inCodeBlock = false
        input.split("\n").forEachIndexed { index, rawLine ->
            if (rawLine.trimStart().startsWith("```")) {
                if (inCodeBlock) output.append("</tt></pre>") else output.append("<pre><tt>")
                inCodeBlock = !inCodeBlock
            } else if (inCodeBlock) {
                output.append(escapeHtml(rawLine)).append("\n")
            } else {
                var line = escapeHtml(rawLine)
                line = line.replace(Regex("^### (.+)$"), "<h3>$1</h3>")
                line = line.replace(Regex("^## (.+)$"), "<h2>$1</h2>")
                line = line.replace(Regex("^# (.+)$"), "<h1>$1</h1>")
                line = line.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
                line = line.replace(Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)"), "<i>$1</i>")
                line = line.replace(Regex("`([^`]+?)`"), "<tt>$1</tt>")
                output.append(line)
                if (index < input.split("\n").lastIndex) output.append("<br>")
            }
        }
        if (inCodeBlock) output.append("</tt></pre>")
        return output.toString()
    }

    private fun setOrbAppearance(mode: Int) {
        orbMode = mode.coerceIn(0, 3)
        val accent = when (orbMode) {
            1 -> "amber"
            2 -> "violet"
            3 -> "green"
            else -> "cyan"
        }
        settingsPrefs.edit()
            .putInt("orb_mode", orbMode)
            .putString("theme_accent", accent)
            .apply()
        if (::orb.isInitialized) orb.setOrbMode(orbMode)
        applyThemeAccent()
        status.text = "VYOM  •  THEME UPDATED"
    }

    private fun applyThemeAccent() {
        val accent = VyomTheme.accent(this, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan")
        if (::status.isInitialized) status.setTextColor(accent)
        (mainPanel?.background as? GradientDrawable)?.setStroke(dp(1), Color.argb(180, Color.red(accent), Color.green(accent), Color.blue(accent)))
        (dockBar?.background as? GradientDrawable)?.setStroke(dp(1), accent)
        if (::chatTab.isInitialized) listOf(chatTab, tasksTab, settingsTab).forEach { it.setTextColor(accent) }
        orbitHomeView?.setAccentColor(accent)
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
        val history = chatMessages
            .filterNot { it.text == message && it.createdAt >= System.currentTimeMillis() - 15_000L }
            .takeLast(20)
            .joinToString("\n") { item ->
                "User: ${item.text.take(900)}" + if (item.reply.isBlank()) "" else "\nVyom: ${item.reply.take(900)}"
            }.takeLast(9000)
        val facts = memoryStore.facts().take(30).joinToString("\n")
        val memoryBlock = if (facts.isBlank()) "" else "\n\nOn-device memory facts (user-controlled):\n$facts"
        val historyBlock = if (history.isBlank()) "" else "\n\nRecent conversation context (up to 20 messages):\n$history"
        return "$instruction$memoryBlock$historyBlock\n\nCurrent user message:\n$message".take(14000)
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

    private fun secureVoicePrefs(): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(this, "vyom_voice_secrets", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    private fun voiceProvider(): TtsProvider {
        val prefs = secureVoicePrefs()
        return ElevenLabsTtsProvider(this, prefs.getString("elevenlabs_api_key", "").orEmpty(),
            prefs.getString("elevenlabs_voice_id", "").orEmpty(), AndroidTtsProvider(textToSpeech))
    }

    private fun speakReply(reply: String, onComplete: (() -> Unit)? = null) {
        if (voiceMuted) { onComplete?.invoke(); return }
        voiceTtsProvider?.stop()
        voiceTtsProvider = voiceProvider()
        if (::ttsConfigStatus.isInitialized) ttsConfigStatus.text = voiceTtsProvider?.status()
        voiceTtsProvider?.speak(reply.trim(), { runOnUiThread { onComplete?.invoke() } }, { error ->
            runOnUiThread { if (::micIndicator.isInitialized) micIndicator.text = "VOICE ERROR"; onComplete?.invoke() }
        })
    }
    private fun toggleVoiceInput() {
        if (voiceLoopEnabled) {
            voiceLoopEnabled = false
            liveLaneEnabled = false
            voiceStateMachine.stop()
            stopVoiceForegroundService()
            voiceTtsProvider?.stop()
            nemotronLiveClient?.cancel()
            nemotronLiveClient = null
            liveGenerationActive = false
            liveReplySpeaking = false
            liveStreamFinished = false
            liveSentenceTtsPlaying = false
            liveSentenceQueue.clear()
            voiceUiSounds?.close()
            voiceUiSounds = null
            stopVoiceInput(cancel = true)
            voiceDialog?.dismiss()
            voiceDialog = null
            stopVoiceForegroundService()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        showVoiceMode()
        voiceLoopEnabled = true
        liveLaneEnabled = true
        voiceStateMachine.start()
        startVoiceForegroundService()
        if (::voiceTranscript.isInitialized) voiceTranscript.text = ""
        speakReply("Haan, main sun raha hoon. Boliye.") {
            if (voiceLoopEnabled && !voiceMuted) startVoiceInput()
        }
    }

    private fun toggleVoiceMuteFromNotification() {
        voiceMuted = !voiceMuted
        if (voiceMuted) {
            stopVoiceInput(cancel = true)
            voiceTtsProvider?.stop()
            nemotronLiveClient?.cancel()
            liveGenerationActive = false
            liveReplySpeaking = false
            liveStreamFinished = false
            liveSentenceTtsPlaying = false
            liveSentenceQueue.clear()
            setLiveStatus("muted")
        } else if (voiceLoopEnabled) {
            setLiveStatus("listening")
            startVoiceInput()
        }
    }

    private fun showVoiceMode() {
        if (voiceUiSounds == null) voiceUiSounds = VoiceUiSounds(this)
        liveCaptionsEnabled = settingsPrefs.getBoolean("live_captions", false)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(18))
            setBackgroundColor(Color.TRANSPARENT)
        }
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        topBar.addView(Button(this).apply {
            text = if (liveCaptionsEnabled) "CAPTIONS ON" else "CAPTIONS OFF"
            contentDescription = "Toggle live captions"
            setOnClickListener {
                liveCaptionsEnabled = !liveCaptionsEnabled
                settingsPrefs.edit().putBoolean("live_captions", liveCaptionsEnabled).apply()
                text = if (liveCaptionsEnabled) "CAPTIONS ON" else "CAPTIONS OFF"
                liveCaptionView?.visibility = if (liveCaptionsEnabled) View.VISIBLE else View.GONE
            }
        }, LinearLayout.LayoutParams(dp(128), dp(42)))
        topBar.addView(Button(this).apply {
            text = "⋮"
            contentDescription = "Live voice menu"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Live voice")
                    .setItems(arrayOf("Voice settings", "Diagnostics", "End live session")) { _, which ->
                        when (which) {
                            0 -> showTab(false, true)
                            1 -> AlertDialog.Builder(this@MainActivity).setTitle("Voice diagnostics")
                                .setMessage("Stage timing collection is not wired yet. See the final implementation report for blocked diagnostics.")
                                .setPositiveButton("OK", null).show()
                            2 -> endLiveVoiceSession()
                        }
                    }.setNegativeButton("Close", null).show()
            }
        }, LinearLayout.LayoutParams(dp(48), dp(42)))
        panel.addView(topBar, LinearLayout.LayoutParams(-1, dp(48)))

        val voiceOrb = OrbSurfaceView(this).apply {
            setZOrderMediaOverlay(false)
            setBackgroundColor(Color.TRANSPARENT)
            setOrbMode(orbMode)
            setLiteMode(liteMode)
            setReduceMotion(reduceMotion)
            contentDescription = "Live voice orb reacts to microphone level"
        }
        liveVoiceOrb = voiceOrb
        panel.addView(voiceOrb, LinearLayout.LayoutParams(-1, dp(250)))
        liveStateLabel = TextView(this).apply {
            text = if (voiceMuted) "MUTED" else if (liveGenerationActive) "THINKING" else "LISTENING"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(VyomTheme.accent(this@MainActivity, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan"))
            contentDescription = "Live voice state"
        }
        panel.addView(liveStateLabel, LinearLayout.LayoutParams(-1, dp(38)))
        liveCaptionView = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            visibility = if (liveCaptionsEnabled) View.VISIBLE else View.GONE
        }
        panel.addView(liveCaptionView, LinearLayout.LayoutParams(-1, dp(76)))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        controls.addView(Button(this).apply {
            text = if (voiceMuted) "UNMUTE" else "MUTE"
            contentDescription = if (voiceMuted) "Unmute microphone" else "Mute microphone"
            setOnClickListener {
                voiceMuted = !voiceMuted
                text = if (voiceMuted) "UNMUTE" else "MUTE"
                contentDescription = if (voiceMuted) "Unmute microphone" else "Mute microphone"
                if (voiceMuted) {
                    stopVoiceInput(cancel = true)
                    voiceUiSounds?.playMuted()
                    silenceSubmitRunnable?.let { mainHandler.removeCallbacks(it) }
                    silenceSubmitRunnable = null
                    voiceTtsProvider?.stop()
                    nemotronLiveClient?.cancel()
                    liveGenerationActive = false
                    liveReplySpeaking = false
                    liveStreamFinished = false
                    liveSentenceTtsPlaying = false
                    liveSentenceQueue.clear()
                    setLiveStatus("muted")
                } else if (voiceLoopEnabled) {
                    voiceUiSounds?.playUnmuted()
                    setLiveStatus("listening")
                    startVoiceInput()
                } else voiceUiSounds?.playUnmuted()
            }
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        controls.addView(Button(this).apply {
            text = "END"
            contentDescription = "End live voice session"
            setOnClickListener { endLiveVoiceSession() }
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        panel.addView(controls, LinearLayout.LayoutParams(-1, dp(62)))
        voiceDialog = AlertDialog.Builder(this).setView(panel).setOnCancelListener {
            endLiveVoiceSession(dismiss = false)
        }.create()
        voiceDialog?.setOnShowListener {
            voiceDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            voiceDialog?.window?.setLayout(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            voiceOrb.onResume()
        }
        voiceDialog?.setOnDismissListener {
            voiceOrb.onPause()
            liveVoiceOrb = null
            liveStateLabel = null
            liveCaptionView = null
        }
        voiceDialog?.show()
        voiceDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        voiceDialog?.window?.setLayout(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    }

    private fun endLiveVoiceSession(dismiss: Boolean = true) {
        voiceLoopEnabled = false
        voiceStateMachine.stop()
        voiceTtsProvider?.stop()
        nemotronLiveClient?.cancel()
        stopVoiceInput(cancel = true)
        stopVoiceForegroundService()
        if (dismiss) voiceDialog?.dismiss()
        setLiveStatus("idle")
    }

    private fun startVoiceInput() {
        if (voiceMuted || !voiceLoopEnabled) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        stopVoiceInput(cancel = true, updateStatus = false)
        val apiKey = secureVoicePrefs().getString("elevenlabs_api_key", "").orEmpty()
        voiceButton.text = "STOP LISTENING"
        micIndicator.text = "MIC STARTING — LIVE STT"
        liveSttCapture = LiveSttCapture(
            context = this,
            apiKey = apiKey,
            languageCode = secureVoicePrefs().getString("stt_language_code", "").orEmpty(),
            onPartial = { partial ->
                runOnUiThread {
                    if (liveCaptionsEnabled) liveCaptionView?.text = partial
                    if (voiceLoopEnabled && !voiceMuted && partial.isNotBlank()) {
                        if (liveLaneEnabled && (liveGenerationActive || liveReplySpeaking)) {
                            liveGenerationActive = false
                            liveReplySpeaking = false
                            voiceTtsProvider?.stop()
                            nemotronLiveClient?.cancel()
                            liveSentenceQueue.clear()
                            liveSentenceTtsPlaying = false
                            liveStreamFinished = false
                            if (::micIndicator.isInitialized) micIndicator.text = "LIVE • INTERRUPTED — LISTENING"
                        }
                    }
                }
            },
            onCommitted = { recognized ->
                runOnUiThread {
                    if (liveCaptionsEnabled) liveCaptionView?.text = recognized
                    if (voiceLoopEnabled && !voiceMuted && recognized.isNotBlank()) {
                        if (liveLaneEnabled) {
                            val item = ChatMessage(UUID.randomUUID().toString(), recognized, "running")
                            chatMessages.add(item)
                            persistChatMessages()
                            renderChatMessages()
                            if (dispatchDeviceAction(recognized, "voice") { reply ->
                                    item.state = "done"; item.reply = reply
                                    persistChatMessages(); renderChatMessages()
                                    if (voiceLoopEnabled && !voiceMuted && !voiceActionAckInFlight) speakReply(reply) {
                                        if (voiceLoopEnabled && !voiceMuted) {
                                            if (voiceStateMachine.state == VoiceState.SPEAKING) voiceStateMachine.speechFinished()
                                            startVoiceInput()
                                        }
                                    }
                                }) return@runOnUiThread
                            val normalized = recognized.trim().lowercase().replace(Regex("[.!?]+$"), "").trim()
                            val instantReply = when (normalized) {
                                "hi", "hello", "hey vyom", "hi vyom", "hello vyom", "hey" -> "Haan, bolo. Main sun raha hoon."
                                else -> null
                            }
                            if (instantReply != null) {
                                item.state = "done"
                                item.reply = instantReply
                                persistChatMessages()
                                renderChatMessages()
                                liveStreamFinished = true
                                liveGenerationActive = false
                                liveSentenceQueue.clear()
                                liveSentenceQueue.addLast(instantReply)
                                pumpLiveSentenceTts()
                            } else {
                                runLiveLane(recognized, item)
                            }
                        } else {
                            chatInput.setText(recognized)
                            chatInput.setSelection(chatInput.text.length)
                            micIndicator.text = "STT TEXT READY — REVIEW OR SEND"
                        }
                    }
                }
            },
            onStatus = { message ->
                runOnUiThread {
                    if (::micIndicator.isInitialized && voiceLoopEnabled && !voiceMuted) micIndicator.text = message
                    val level = Regex("MIC LEVEL (\\d+)%").find(message)?.groupValues?.getOrNull(1)?.toFloatOrNull()
                    if (level != null) liveVoiceOrb?.setAudioLevel(level / 100f)
                }
            }
        ).also { it.start() }
    }

    private fun loadNvidiaModels() {
        val key = nvidiaApiKeyInput.text.toString().trim()
        if (key.isBlank()) {
            liveModelNotice.text = "ERROR: NVIDIA API key is empty. Enter it above and save settings first."
            return
        }
        liveModelNotice.text = "Loading models from NVIDIA…"
        io.execute {
            try {
                val conn = (URL("https://integrate.api.nvidia.com/v1/models").openConnection() as HttpURLConnection).also {
                    it.requestMethod = "GET"
                    it.connectTimeout = 15000
                    it.readTimeout = 30000
                    it.setRequestProperty("Authorization", "Bearer $key")
                    it.setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                val responseBody = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                if (code !in 200..299) throw IllegalStateException("NVIDIA /v1/models returned HTTP " + code + ": " + responseBody.take(240))
                val data = JSONObject(responseBody).optJSONArray("data")
                    ?: throw IllegalStateException("NVIDIA /v1/models response has no 'data' model list.")
                val ids = (0 until data.length()).mapNotNull { index ->
                    data.optJSONObject(index)?.optString("id")?.trim()?.takeIf { it.isNotEmpty() }
                }.distinct().sorted()
                if (ids.isEmpty()) throw IllegalStateException("NVIDIA returned an empty model list. Check API key/account access.")
                runOnUiThread {
                    liveModelNotice.text = "Loaded " + ids.size + " models. Tap one to save it on this device."
                    AlertDialog.Builder(this)
                        .setTitle("Choose LIVE model")
                        .setItems(ids.toTypedArray()) { _, which ->
                            val chosen = ids[which]
                            liveModelInput.setText(chosen)
                            secureVoicePrefs().edit().putString("nvidia_live_model_id", chosen).apply()
                            liveModelNotice.text = "Selected LIVE model: " + chosen
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    liveModelNotice.text = "ERROR loading NVIDIA models: " + (error.message ?: "Unknown network error")
                }
            }
        }
    }

    private fun runLiveLane(prompt: String, historyItem: ChatMessage? = null) {
        val key = secureVoicePrefs().getString("nvidia_api_key", "").orEmpty()
        val modelId = LiveModelPolicy.selectedOrDefault(secureVoicePrefs().getString("nvidia_live_model_id", ""))
        if (key.isBlank()) {
            historyItem?.let { it.state = "error"; it.reply = "LIVE lane needs an NVIDIA API key. Save it in Profile > Settings."; persistChatMessages(); renderChatMessages() }
            micIndicator.text = "LIVE KEY MISSING"
            return
        }
        if (modelId.isBlank()) {
            historyItem?.let { it.state = "error"; it.reply = "LIVE model ID missing. Open Profile > Settings, tap LOAD MODELS, then choose a model."; persistChatMessages(); renderChatMessages() }
            micIndicator.text = "LIVE MODEL MISSING"
            return
        }
        memoryStore.learnFrom(prompt)
        val livePrompt = personaPrompt(prompt)
        voiceTtsProvider?.stop()
        nemotronLiveClient?.cancel()
        val client = NemotronLiveClient(key, modelId)
        nemotronLiveClient = client
        liveGenerationActive = true
        setLiveStatus("thinking")
        liveStreamFinished = false
        liveSentenceTtsPlaying = false
        liveSentenceQueue.clear()
        val accumulated = StringBuilder()
        setLiveStatus("thinking")
        if (::micIndicator.isInitialized) micIndicator.text = ""
        client.stream(livePrompt, { delta ->
            runOnUiThread {
                if (liveLaneEnabled && voiceLoopEnabled && nemotronLiveClient === client && !voiceMuted) {
                    accumulated.append(delta)
                }
            }
        }, { sentence ->
            runOnUiThread {
                if (liveLaneEnabled && voiceLoopEnabled && nemotronLiveClient === client && !voiceMuted && sentence.isNotBlank()) {
                    liveSentenceQueue.addLast(sentence)
                    pumpLiveSentenceTts()
                }
            }
        }, { reply ->
            runOnUiThread {
                liveGenerationActive = false
                liveStreamFinished = true
                if (liveLaneEnabled && voiceLoopEnabled && nemotronLiveClient === client && !voiceMuted && reply.isNotBlank()) {
                    historyItem?.let { it.state = "done"; it.reply = reply; persistChatMessages(); renderChatMessages() }
                    pumpLiveSentenceTts()
                }
            }
        }, { error ->
            runOnUiThread {
                liveGenerationActive = false
                liveReplySpeaking = false
                liveStreamFinished = true
                liveSentenceQueue.clear()
                if (nemotronLiveClient === client) {
                    micIndicator.text = "LIVE ERROR"
                    historyItem?.let { it.state = "error"; it.reply = error; persistChatMessages(); renderChatMessages() }
                }
            }
        })
    }

    private fun pumpLiveSentenceTts() {
        if (liveSentenceTtsPlaying || voiceMuted || !voiceLoopEnabled) return
        val sentence = liveSentenceQueue.pollFirst()
        if (sentence == null) {
            if (liveStreamFinished && !liveGenerationActive) {
                liveReplySpeaking = false
                setLiveStatus("listening")
                if (::micIndicator.isInitialized) micIndicator.text = ""
            }
            return
        }
        if (voiceTtsProvider == null) voiceTtsProvider = voiceProvider()
        liveSentenceTtsPlaying = true
        liveReplySpeaking = true
        setLiveStatus("speaking")
        voiceTtsProvider?.speak(sentence, {
            runOnUiThread {
                liveSentenceTtsPlaying = false
                liveReplySpeaking = false
                pumpLiveSentenceTts()
            }
        }, {
            runOnUiThread {
                liveSentenceTtsPlaying = false
                liveReplySpeaking = false
                pumpLiveSentenceTts()
            }
        })
    }

    private fun setLiveStatus(state: String) {
        val color = when (state) {
            "thinking" -> Color.rgb(255, 190, 60)
            "speaking" -> Color.rgb(80, 150, 255)
            "muted" -> Color.GRAY
            else -> VyomTheme.accent(this, settingsPrefs.getString("theme_accent", "cyan") ?: "cyan")
        }
        liveStateLabel?.text = state.uppercase()
        liveStateLabel?.setTextColor(color)
        liveStateLabel?.contentDescription = "Live voice state: $state"
        liveVoiceOrb?.setOrbMode(when (state) {
            "thinking" -> 1
            "speaking" -> 2
            "muted" -> 3
            else -> orbMode
        })
    }

    private fun startVoiceForegroundService() {
        val intent = Intent(this, VoiceForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } catch (error: Exception) {
            if (::micIndicator.isInitialized) micIndicator.text = "FOREGROUND SERVICE ERROR: " + (error.message ?: "start failed")
        }
    }

    private fun stopVoiceForegroundService() {
        try { stopService(Intent(this, VoiceForegroundService::class.java)) } catch (_: Exception) {}
    }

    private fun stopVoiceInput(cancel: Boolean, updateStatus: Boolean = true) {
        listening = false
        liveSttCapture?.stop()
        liveSttCapture = null
        silenceSubmitRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceSubmitRunnable = null
        try {
            if (cancel) speechRecognizer?.cancel() else speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
        if (::voiceButton.isInitialized) voiceButton.text = "TALK"
        if (updateStatus && ::micIndicator.isInitialized) {
            micIndicator.text = if (voiceMuted) "MIC MUTED" else "MIC OFF"
            micIndicator.contentDescription = if (voiceMuted) "Microphone status: muted" else "Microphone status: off"
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
        val release = JSONObject(readHttpsText("https://api.github.com/repos/riyas433554-dot/vyom-android/releases/latest", 128 * 1024))
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
                val rollbackUri = backupCurrentApkForRollback()
                runOnUiThread {
                    settingsPrefs.edit().putString("previous_apk_uri", rollbackUri.toString()).apply()
                    rollbackButton.visibility = View.VISIBLE
                    logControlAction("Self-update rollback backup", "saved Downloads/VyomRollback", "app")
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

    private fun backupCurrentApkForRollback(): Uri {
        if (Build.VERSION.SDK_INT < 29) {
            throw IllegalStateException("Safe persistent rollback backup requires Android 10 or newer; update was not staged")
        }
        val source = File(applicationInfo.sourceDir)
        if (!source.isFile || source.length() <= 0L) throw IllegalStateException("Installed APK source is unavailable")
        val displayName = "Vyom-rollback-v${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}.apk"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VyomRollback")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create rollback APK in Downloads")
        try {
            contentResolver.openOutputStream(uri, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not write rollback APK")
            val backupLength = contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(16384)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                }
                total
            } ?: throw IllegalStateException("Could not verify rollback APK")
            if (backupLength != source.length()) throw IllegalStateException("Rollback APK size verification failed")
            val sourceHash = sha256(source)
            val backupHash = sha256Uri(uri)
            if (!MessageDigest.isEqual(sourceHash.toByteArray(), backupHash.toByteArray())) {
                throw IllegalStateException("Rollback APK SHA-256 verification failed")
            }
            settingsPrefs.edit()
                .putString("previous_apk_sha256", backupHash)
                .putString("previous_apk_version_name", BuildConfig.VERSION_NAME)
                .putInt("previous_apk_version_code", BuildConfig.VERSION_CODE)
                .apply()
            val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            contentResolver.update(uri, ready, null, null)
            return uri
        } catch (error: Exception) {
            try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            throw error
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
            logControlAction("Self-update", "installer opened after SHA-256 and signing-certificate verification", "app")
            updateNotice.text = "Android installer opened. Existing Vyom remains installed unless you confirm replacement."
        } catch (error: Exception) {
            logControlAction("Self-update", "installer failed: ${error.message ?: "unknown error"}", "app")
            updateNotice.text = "Could not open Android installer. Existing Vyom was not replaced."
        }
    }

    private fun validateReleaseUrl(value: String) {
        val uri = Uri.parse(value)
        if (!uri.scheme.equals("https", true) || !uri.host.equals("github.com", true) ||
            !uri.path.orEmpty().startsWith("/riyas433554-dot/vyom-android/releases/download/")) {
            throw IllegalStateException("Update URL is not an approved GitHub release asset")
        }
    }

    private fun validateReleaseUrlOrApi(value: String) {
        val uri = Uri.parse(value)
        val validReleaseAsset = uri.host.equals("github.com", true) &&
            uri.path.orEmpty().startsWith("/riyas433554-dot/vyom-android/releases/download/")
        val validApi = uri.host.equals("api.github.com", true) &&
            uri.path == "/repos/riyas433554-dot/vyom-android/releases/latest"
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

    private fun sha256Uri(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(16384)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        } ?: throw IllegalStateException("Could not read APK backup for SHA-256 verification")
        return digest.digest().joinToString("") { "%02x".format(it) }
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
        createTaskButton.isEnabled = false
        if (dispatchDeviceAction(prompt, "task") { reply ->
                taskNotice.text = reply; taskInput.setText(""); createTaskButton.isEnabled = true
            }) return
        createTaskButton.isEnabled = true
        val maxTasks = settingsPrefs.getInt("max_tasks", 5).coerceIn(5, 20)
        val activeStates = setOf("queued", "pending", "running", "in_progress", "processing", "working")
        val activeCount = tasksById.values.count { it.optString("status", "queued").lowercase() in activeStates }
        if (activeCount >= maxTasks) {
            taskNotice.text = "Active task limit reached ($maxTasks). Change QUEUE LIMIT or wait for a task to finish."
            return
        }
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
        val allTasks = tasksById.values.toList().asReversed()
        orbitHomeView?.setTaskSnapshots(allTasks.take(5).map { task ->
            val title = task.optString("prompt", "Task ${task.optString("id")}")
                .lineSequence().firstOrNull().orEmpty().ifBlank { "Task ${task.optString("id")}" }
            title to task.optString("status", "queued")
        })
        taskList.removeAllViews()
        val activeStates = setOf("queued", "pending", "running", "in_progress", "processing", "working")
        val active = allTasks.filter { it.optString("status", "queued").lowercase() in activeStates }
        val maxTasks = settingsPrefs.getInt("max_tasks", 5).coerceIn(5, 20)
        val dashboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.rgb(17, 34, 51))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.argb(130, 0, 190, 255))
            }
        }
        dashboard.addView(TextView(this).apply {
            text = "TASKS / AGENTS"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(0, 210, 255))
        })
        dashboard.addView(TextView(this).apply {
            text = "Active tasks: ${active.size}  •  Queue limit: $maxTasks"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, dp(5), 0, dp(3))
        })
        dashboard.addView(Button(this).apply {
            text = "QUEUE LIMIT: $maxTasks (CHANGE)"
            contentDescription = "Change local active task submission limit"
            setOnClickListener {
                val choices = arrayOf("5 tasks", "10 tasks", "15 tasks", "20 tasks")
                val values = intArrayOf(5, 10, 15, 20)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Maximum active tasks")
                    .setMessage("This is a client-side submission guard only. It does not change server concurrency or GitHub Actions limits.")
                    .setSingleChoiceItems(choices, values.indexOf(maxTasks).coerceAtLeast(0)) { dialog, which ->
                        settingsPrefs.edit().putInt("max_tasks", values[which]).apply()
                        dialog.dismiss()
                        renderTasks()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, dp(42)))
        val current = active.firstOrNull { it.optString("status", "").lowercase() in setOf("running", "in_progress", "processing", "working") }
        dashboard.addView(TextView(this).apply {
            text = if (current == null) "No task currently marked running" else
                "CURRENT TASK  •  ${current.optString("prompt", "Working").lineSequence().firstOrNull().orEmpty()}\nStatus: ${current.optString("status", "running")}"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(7), 0, dp(5))
        })
        val agentArray = current?.optJSONArray("agents")
        dashboard.addView(TextView(this).apply {
            text = "AGENTS"
            textSize = 10f
            setTextColor(Color.rgb(150, 190, 220))
            setPadding(0, dp(4), 0, dp(3))
        })
        if (agentArray != null && agentArray.length() > 0) {
            for (i in 0 until agentArray.length()) {
                val agent = agentArray.optJSONObject(i)
                dashboard.addView(TextView(this).apply {
                    text = "• ${agent?.optString("name", "Agent ${i + 1}") ?: "Agent ${i + 1}"} — ${agent?.optString("status", "status unavailable") ?: "status unavailable"}"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setPadding(dp(4), dp(2), 0, dp(2))
                })
            }
        } else {
            dashboard.addView(TextView(this).apply {
                text = "Agent-level details are not present in the current task API response."
                textSize = 11f
                setTextColor(Color.LTGRAY)
            })
        }
        taskList.addView(dashboard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        if (allTasks.isEmpty()) {
            taskList.addView(TextView(this).apply {
                text = "No tasks yet. Create one above."
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }
        taskList.addView(TextView(this).apply {
            text = "OTHER TASKS"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(150, 190, 220))
            setPadding(0, dp(2), 0, dp(6))
        })
        allTasks.forEach { task ->
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
            card.addView(TextView(this).apply {
                text = "Task $id  •  ${state.uppercase()}"
                textSize = 12f
                setTextColor(if (state in setOf("failed", "error")) Color.rgb(255, 120, 120) else Color.rgb(0, 210, 255))
                setPadding(0, 0, 0, dp(5))
            })
            card.addView(TextView(this).apply {
                text = task.optString("prompt", "")
                textSize = 13f
                setTextColor(Color.WHITE)
                setTextIsSelectable(true)
            })
            val resultValue = task.opt("result")
            val resultText = when (resultValue) {
                null, JSONObject.NULL -> ""
                is JSONObject -> resultValue.toString(2)
                else -> resultValue.toString()
            }
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
            if (state in activeStates) {
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

    private fun openDeletePicker(source: String, callback: (String) -> Unit) {
        pendingDocumentSource = source
        pendingDocumentCallback = callback
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try { startActivityForResult(picker, REQUEST_DELETE_DOCUMENT) }
        catch (_: Exception) { pendingDocumentCallback = null; callback("Android file picker is unavailable.") }
    }

    private fun openInstallPicker(source: String, callback: (String) -> Unit) {
        pendingDocumentSource = source
        pendingDocumentCallback = callback
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivityForResult(picker, REQUEST_INSTALL_DOCUMENT) }
        catch (_: Exception) { pendingDocumentCallback = null; callback("Android APK picker is unavailable.") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_DELETE_DOCUMENT && requestCode != REQUEST_INSTALL_DOCUMENT) return
        val callback = pendingDocumentCallback
        val source = pendingDocumentSource
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            pendingDocumentCallback = null
            logControlAction(if (requestCode == REQUEST_DELETE_DOCUMENT) "Delete document" else "Install APK", "picker-cancelled", source)
            callback?.invoke("Cancelled. No file was changed or installed.")
            return
        }
        if (requestCode == REQUEST_DELETE_DOCUMENT) {
            AlertDialog.Builder(this).setTitle("Permanently delete selected file?")
                .setMessage("This deletes the selected document using its Android document provider. This cannot be undone.")
                .setNegativeButton("CANCEL") { _, _ ->
                    pendingDocumentCallback = null
                    logControlAction("Delete selected document", "cancelled", source)
                    callback?.invoke("Delete cancelled. The file was not changed.")
                }
                .setPositiveButton("DELETE FILE") { _, _ ->
                    val result = try {
                        if (DocumentsContract.deleteDocument(contentResolver, uri)) "Selected document deleted."
                        else "The document provider refused deletion."
                    } catch (error: Exception) { "Could not delete selected document: " + (error.message ?: "provider error") }
                    logControlAction("Delete selected document", if (result.startsWith("Selected")) "deleted" else "failed", source)
                    pendingDocumentCallback = null
                    callback?.invoke(result)
                }.show()
            return
        }
        try {
            val folder = File(cacheDir, "updates").apply { mkdirs() }
            val apk = File(folder, "user-selected-install.apk")
            val input = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Could not read selected APK")
            input.use { stream ->
                FileOutputStream(apk).use { output ->
                    val buffer = ByteArray(16384)
                    var total = 0L
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 100L * 1024L * 1024L) throw IllegalStateException("APK exceeds 100 MiB limit")
                        output.write(buffer, 0, count)
                    }
                }
            }
            val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, packageInfoFlags())
                ?: throw IllegalStateException("Selected file is not a valid APK")
            if (archive.packageName == packageName) {
                apk.delete()
                throw IllegalStateException("Vyom itself can only be updated through the signed SHA-256-verified update flow")
            }
            AlertDialog.Builder(this).setTitle("Install selected app?")
                .setMessage("Package: " + archive.packageName + ". Android will show its own final installation prompt. Only continue if you trust this APK.")
                .setNegativeButton("CANCEL") { _, _ ->
                    apk.delete()
                    pendingDocumentCallback = null
                    logControlAction("Install APK " + archive.packageName, "cancelled", source)
                    callback?.invoke("Installation cancelled.")
                }
                .setPositiveButton("OPEN ANDROID INSTALLER") { _, _ ->
                    try {
                        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + packageName)))
                            logControlAction("Install APK " + archive.packageName, "awaiting-unknown-sources-permission", source)
                            callback?.invoke("Allow Vyom to install apps in Android Settings, then repeat the install command.")
                        } else {
                            val apkUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
                            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                            logControlAction("Install APK " + archive.packageName, "android-installer-opened", source)
                            callback?.invoke("Android installer opened. Approve installation there if you trust this APK.")
                        }
                    } catch (error: Exception) {
                        logControlAction("Install APK " + archive.packageName, "failed", source)
                        callback?.invoke("Could not open Android installer: " + (error.message ?: "installer error"))
                    } finally { pendingDocumentCallback = null }
                }.show()
        } catch (error: Exception) {
            pendingDocumentCallback = null
            logControlAction("Install APK", "rejected", source)
            callback?.invoke("APK rejected: " + (error.message ?: "validation failed"))
        }
    }

    private fun dispatchDeviceAction(text: String, source: String, onResult: (String) -> Unit): Boolean {
        val direct = ActionEngine.classify(text)
        val learned = if (direct == null) DeviceCommandConfig.resolveLearnedCommand(this, text) else null
        val action = direct ?: learned?.let { ActionEngine.classify(it) }
        if (action != null) { executeClassifiedDeviceAction(action, source, onResult); return true }
        if (!ActionEngine.looksLikeDeviceAction(text) && learned == null) return false
        if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
            logControlAction(text, "blocked-by-kill-switch", source); onResult("STOP CONTROL is ON. Device action blocked."); return true
        }
        logControlAction(text, "llm-fallback-requested", source)
        io.execute {
            val normalized = try { normalizeDeviceActionWithModel(text) } catch (_: Exception) { null }
            runOnUiThread {
                val fallback = normalized?.takeIf { ActionEngine.fallbackMatchesIntent(text, it) }?.let { ActionEngine.classify(it) }
                if (fallback == null) {
                    logControlAction(text, "unrecognized-command-teach-offered", source)
                    showTeachCommandDialog(text, source, onResult)
                } else executeClassifiedDeviceAction(fallback, source, onResult)
            }
        }
        return true
    }

    private fun showTeachCommandDialog(original: String, source: String, onResult: (String) -> Unit) {
        val answer = EditText(this).apply {
            hint = "e.g. open YouTube / volume badhao / torch chalu"
            setSingleLine(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(TextView(this@MainActivity).apply {
                text = "Ye kya karna chahiye? Ek supported command likho. Vyom ise sirf isi phone par save karega."
                setTextColor(Color.WHITE)
                textSize = 14f
            })
            addView(answer, LinearLayout.LayoutParams(-1, dp(52)))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Teach Vyom").setView(body)
            .setNegativeButton("CANCEL") { _, _ -> onResult("Okay, command save nahi kiya.") }
            .setPositiveButton("SAVE & RUN", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val canonical = answer.text.toString().trim()
                if (canonical.isBlank() || canonical.length > 120) {
                    answer.error = "Short command likho (1–120 characters)."; return@setOnClickListener
                }
                val taught = ActionEngine.classify(canonical)
                if (taught == null || taught.risk == ActionEngine.Risk.BLOCKED) {
                    answer.error = "Supported command do, jaise 'open YouTube', 'volume up', 'torch chalu'. Safety rules badle nahi ja sakte."
                    return@setOnClickListener
                }
                if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
                    logControlAction(original, "teaching-blocked-by-kill-switch", source)
                    dialog.dismiss(); onResult("STOP CONTROL is ON. Device action blocked; teaching was not run."); return@setOnClickListener
                }
                DeviceCommandConfig.saveTeaching(this, original, canonical)
                logControlAction("Teach command: $original", "saved-locally-to=$canonical", source)
                dialog.dismiss()
                executeClassifiedDeviceAction(taught, source, onResult)
            }
        }
        dialog.show()
    }

    private fun executeClassifiedDeviceAction(action: ActionEngine.Intent, source: String, onResult: (String) -> Unit) {
        if (!isSkillEnabled(action)) {
            logControlAction(action.name, "blocked-skill-disabled", source)
            onResult("Ye skill Profile > Skills mein disabled hai. Pehle enable karo.")
            return
        }
        if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
            logControlAction(action.name, "blocked-by-kill-switch", source); onResult("STOP CONTROL is ON. Device action blocked."); return
        }
        if (action.risk == ActionEngine.Risk.BLOCKED) {
            logControlAction(action.name, "blocked-by-policy", source)
            onResult("Blocked by Vyom safety policy. Banking/payment apps, passwords/OTPs, uninstall and security settings are never controlled."); return
        }
        if (action.risk == ActionEngine.Risk.CONFIRM) {
            logControlAction(action.name, "awaiting-explicit-screen-confirmation", source)
            AlertDialog.Builder(this).setTitle("Confirm device action")
                .setMessage("Vyom wants to: " + action.name + ". This requires your explicit screen tap. Voice alone cannot approve it.")
                .setNegativeButton("CANCEL") { _, _ -> logControlAction(action.name, "cancelled", source); onResult("Cancelled. Nothing was sent or changed.") }
                .setPositiveButton("CONFIRM & CONTINUE") { _, _ ->
                    if (controlKillSwitch || controlPrefs.getBoolean("kill_switch", false)) {
                        logControlAction(action.name, "blocked-by-kill-switch", source); onResult("STOP CONTROL is ON. Device action blocked.")
                    } else if (action.operation == ActionEngine.Operation.INSTALL && Regex("""\b(update|vyom)\b""", RegexOption.IGNORE_CASE).containsMatchIn(action.target)) {
                        logControlAction(action.name, "verified-update-flow-opened", source)
                        onResult("Checking for a signed Vyom update. Download and Android installation still require separate taps."); checkForUpdate()
                    } else if (action.operation == ActionEngine.Operation.DELETE) {
                        openDeletePicker(source, onResult)
                    } else if (action.operation == ActionEngine.Operation.INSTALL) {
                        openInstallPicker(source, onResult)
                    } else {
                        val reply = DeviceActionExecutor.execute(this, action)
                        logControlAction(action.name, if (reply.startsWith("Could not") || reply.startsWith("Blocked")) "failed" else "confirmed", source)
                        onResult(reply)
                    }
                }.show()
            return
        }
        if (source == "voice" && action.operation in setOf(ActionEngine.Operation.VOLUME_DELTA, ActionEngine.Operation.VOLUME_SET, ActionEngine.Operation.BRIGHTNESS_SET, ActionEngine.Operation.TORCH)) {
            voiceActionAckInFlight = true
            speakReply("Okay, " + action.name) {
                voiceActionAckInFlight = false
                if (voiceLoopEnabled && !voiceMuted) startVoiceInput()
            }
        }
        val reply = DeviceActionExecutor.execute(this, action)
        logControlAction(action.name, if (reply.startsWith("Could not") || reply.startsWith("No Android") || reply.startsWith("Couldn't")) "failed" else "executed", source)
        onResult(reply)
    }

    private fun normalizeDeviceActionWithModel(original: String): String? {
        val key = secureVoicePrefs().getString("nvidia_api_key", "").orEmpty()
        if (key.isBlank()) return null
        val model = LiveModelPolicy.selectedOrDefault(secureVoicePrefs().getString("nvidia_live_model_id", ""))
        val c = (URL("https://integrate.api.nvidia.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 12000; readTimeout = 20000; doOutput = true
            setRequestProperty("Authorization", "Bearer " + key); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject().put("model", model).put("stream", false).put("temperature", 0).put("max_tokens", 120)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "Normalize a direct Android device command into one short plain command without adding targets, numbers, recipients, or intent. Output JSON only with key normalized_command as a string or null. This is classification only, never authorization. If unclear, return null."))
                    .put(JSONObject().put("role", "user").put("content", original)))
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (c.responseCode !in 200..299) return null
            val content = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
            val start = content.indexOf('{'); val end = content.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val normalized = JSONObject(content.substring(start, end + 1)).optString("normalized_command", "").trim()
            return normalized.takeIf { it.isNotBlank() && it.length <= 200 }
        } finally { c.disconnect() }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        tasksTabVisible = false
        mainHandler.removeCallbacks(refreshTasksRunnable)
        if (!voiceLoopEnabled) stopVoiceInput(cancel = true)
        if (::orb.isInitialized) orb.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::orb.isInitialized) orb.onResume()
        if (voiceLoopEnabled && voiceDialog?.isShowing != true) showVoiceMode()
        if (tasksTabVisible) {
            mainHandler.removeCallbacks(refreshTasksRunnable)
            mainHandler.post(refreshTasksRunnable)
        }
    }

    override fun onDestroy() {
        if (voiceStopReceiverRegistered) {
            try { unregisterReceiver(voiceStopReceiver) } catch (_: Exception) {}
            voiceStopReceiverRegistered = false
        }
        mainHandler.removeCallbacksAndMessages(null)
        stopVoiceInput(cancel = true)
        stopVoiceForegroundService()
        voiceTtsProvider?.stop()
        nemotronLiveClient?.cancel()
        nemotronLiveClient = null
        voiceUiSounds?.close()
        voiceUiSounds = null
        voiceTtsProvider = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        io.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 701
        private const val REQUEST_DELETE_DOCUMENT = 705
        private const val REQUEST_INSTALL_DOCUMENT = 706
        private const val TASK_POLL_MS = 4_000L
        private const val SESSION_EXPIRED_MESSAGE = "Session expired, dobara pair karo."
    }
}
