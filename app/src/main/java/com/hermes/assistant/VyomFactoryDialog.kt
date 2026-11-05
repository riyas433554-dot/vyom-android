package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Live Factory workspace. All visible task data is read from GitHub Issues, never demo fixtures. */
internal class VyomFactoryDialog(
    private val activity: Activity,
    private val token: String,
    private val onOpenSettings: () -> Unit,
    private val onProposeFix: (VyomFactoryGitHubClient.Task) -> Unit = {},
    private val onRunnerFailure: (String) -> Unit = {},
    private val pendingScreenshot: File? = null,
    private val onWorkerFailure: (String, String, String) -> Unit = { _, _, _ -> }
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (::dialog.isInitialized && dialog.isShowing && token.isNotBlank()) {
                refresh()
                main.postDelayed(this, 15_000)
            }
        }
    }
    private val client by lazy { VyomFactoryGitHubClient(token) }
    private lateinit var dialog: Dialog
    private lateinit var status: TextView
    private lateinit var taskList: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var createButton: Button
    private var selectedPriority = "P2"
    private var pendingSourceId: String? = null
    private var busy = false
    private lateinit var claimButton: Button
    private lateinit var releaseClaimButton: Button
    private lateinit var runButton: Button
    private lateinit var stopButton: Button
    private val stopRunRequested = AtomicBoolean(false)
    private var pendingConfirmationDialog: AlertDialog? = null
    private var cancelPendingConfirmation: (() -> Unit)? = null
    private lateinit var claimStatus: TextView
    private lateinit var retryStatus: TextView
    private lateinit var historyStatus: TextView
    private var screenshotAttachButton: Button? = null
    private var activeClaim: VyomFactoryWorkerClaimCoordinator.Claim? = null
    private val workerOwnerId: String by lazy {
        val prefs = activity.getSharedPreferences("vyom_factory_worker_identity", Activity.MODE_PRIVATE)
        prefs.getString("owner_id", null)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().also {
            check(prefs.edit().putString("owner_id", it).commit()) { "Could not persist Factory worker identity" }
        }
    }
    private val workerSession by lazy {
        val store = VyomFactoryTaskStore(activity.applicationContext)
        val leases = VyomFactoryWorkerLeaseStore(activity.applicationContext)
        VyomFactoryWorkerSession(
            store,
            VyomFactoryWorkerClaimCoordinator(store, leases),
            workerOwnerId,
            fetchRemoteTasks = { client.listTasks() }
        )
    }

    fun show() {
        dialog = Dialog(activity)
        val accent = VyomTheme.accent(activity, activity.getSharedPreferences("vyom_settings", Activity.MODE_PRIVATE)
            .getString("theme_accent", "cyan") ?: "cyan")
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), 0xF8070B14.toInt())).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent)))
            }
        }
        val header = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(activity).apply {
            text = "VYOM FACTORY"; textSize = 21f; letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD); setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        heading.addView(TextView(activity).apply {
            text = "LIVE TASK REGISTRY · GITHUB ISSUES"; textSize = 9f
            typeface = Typeface.MONOSPACE; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "↻"; textSize = 23f; gravity = Gravity.CENTER; setTextColor(accent)
            contentDescription = "Refresh Factory tasks"
            setOnClickListener { refresh() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(TextView(activity).apply {
            text = "✕"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(38), dp(42)))
        root.addView(header)

        status = TextView(activity).apply {
            text = "Checking GitHub connection…"; textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(8), 0, dp(10))
        }
        root.addView(status)

        val workerPanel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply { setColor(0x22213A54); cornerRadius = dp(12).toFloat() }
        }
        claimStatus = TextView(activity).apply {
            text = "WORKER IDLE · foreground claim only"
            textSize = 9f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        }
        workerPanel.addView(claimStatus)
        val workerActions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        claimButton = Button(activity).apply {
            text = "CLAIM NEXT"; textSize = 9f
            setOnClickListener { claimNextTask() }
        }
        workerActions.addView(claimButton, LinearLayout.LayoutParams(0, dp(40), 1f))
        releaseClaimButton = Button(activity).apply {
            text = "RELEASE CLAIM"; textSize = 9f
            isEnabled = false
            setOnClickListener { releaseActiveClaim() }
        }
        workerActions.addView(releaseClaimButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
            leftMargin = dp(8)
        })
        workerPanel.addView(workerActions)
        runButton = Button(activity).apply {
            text = "RUN NEXT VERIFIED ACTION"; textSize = 9f
            setOnClickListener { runNextTask() }
        }
        workerPanel.addView(runButton, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(6) })
        stopButton = Button(activity).apply {
            text = "STOP CURRENT RUN"; textSize = 9f; isEnabled = false
            setOnClickListener {
                stopRunRequested.set(true)
                cancelPendingConfirmation?.invoke()
                status.text = "STOP requested. Any action not yet started will be blocked; a completed volume change cannot be undone."
                isEnabled = false
            }
        }
        workerPanel.addView(stopButton, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(4) })
        retryStatus = TextView(activity).apply {
            text = "RETRIES · LOADING"
            textSize = 9f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(6), 0, 0)
            contentDescription = "Foreground retry schedule status"
        }
        workerPanel.addView(retryStatus)
        historyStatus = TextView(activity).apply {
            text = "RUNS · LOADING"
            textSize = 9f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(6), 0, 0)
            contentDescription = "Recent persisted Factory execution attempts"
            maxLines = 6
        }
        workerPanel.addView(historyStatus)
        workerPanel.addView(Button(activity).apply {
            text = "DISCOVER / INSTALL SKILL REPOSITORY"
            textSize = 9f
            setOnClickListener { discoverSkillRepositories() }
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        root.addView(workerPanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        pendingScreenshot?.takeIf { it.isFile }?.let { screenshot ->
            val evidencePanel = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    setColor(0x33213A54)
                    cornerRadius = dp(14).toFloat()
                }
            }
            evidencePanel.addView(TextView(activity).apply {
                text = "SCREENSHOT STORED PRIVATELY · ${screenshot.name} · ${screenshot.length()} bytes"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            })
            val previewOptions = BitmapFactory.Options().apply { inSampleSize = 8 }
            BitmapFactory.decodeFile(screenshot.absolutePath, previewOptions)?.let { bitmap ->
                evidencePanel.addView(ImageView(activity).apply {
                    setImageBitmap(bitmap)
                    contentDescription = "Private screenshot preview"
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }, LinearLayout.LayoutParams(-1, dp(160)).apply { topMargin = dp(6) })
            }
            val receiptState = runCatching {
                VyomFactoryScreenshotEvidenceStore(File(activity.filesDir, "factory-evidence")).uploadReceipt(screenshot)
            }
            val priorUpload = receiptState.getOrNull()
            if (receiptState.isFailure) {
                evidencePanel.addView(TextView(activity).apply {
                    text = "UPLOAD STATE UNKNOWN · ATTACH DISABLED TO PREVENT DUPLICATE"
                    textSize = 9f
                    setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                })
            } else if (priorUpload != null) {
                evidencePanel.addView(TextView(activity).apply {
                    text = "UPLOAD ALREADY COMPLETED · issue comment needs manual repair · ${priorUpload.url}"
                    textSize = 9f
                    setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                })
            } else {
                screenshotAttachButton = Button(activity).apply {
                    text = "ATTACH TO FACTORY ISSUE"
                    textSize = 9f
                    setOnClickListener { chooseScreenshotIssue(screenshot) }
                }
                evidencePanel.addView(screenshotAttachButton, LinearLayout.LayoutParams(-1, dp(40)).apply {
                    topMargin = dp(6)
                })
            }
            root.addView(evidencePanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }

        val composer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(0x33213A54); cornerRadius = dp(16).toFloat() }
        }
        composer.addView(TextView(activity).apply {
            text = "REGISTER A COMPLAINT / TASK"; textSize = 10f
            typeface = Typeface.MONOSPACE; setTextColor(accent)
        })
        titleInput = EditText(activity).apply {
            hint = "Short problem title"; setSingleLine(true); textSize = 14f
            setTextColor(Color.WHITE); setHintTextColor(0xFF8B9BB0.toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        composer.addView(titleInput, LinearLayout.LayoutParams(-1, dp(46)))
        descriptionInput = EditText(activity).apply {
            hint = "What is broken, expected result, steps and evidence…"
            minLines = 2; maxLines = 4; textSize = 13f
            setTextColor(Color.WHITE); setHintTextColor(0xFF8B9BB0.toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        composer.addView(descriptionInput, LinearLayout.LayoutParams(-1, dp(88)))
        val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val priorityButton = Button(activity).apply {
            text = "PRIORITY · $selectedPriority"; textSize = 10f
            setOnClickListener {
                val options = arrayOf("P0 · Critical", "P1 · High", "P2 · Normal", "P3 · Low")
                AlertDialog.Builder(activity).setTitle("Complaint priority")
                    .setSingleChoiceItems(options, selectedPriority.drop(1).toInt(), null)
                    .setPositiveButton("SET") { d, _ ->
                        val index = (d as AlertDialog).listView.checkedItemPosition
                        selectedPriority = "P" + index
                        text = "PRIORITY · $selectedPriority"
                    }.setNegativeButton("CANCEL", null).show()
            }
        }
        actions.addView(priorityButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        createButton = Button(activity).apply {
            text = "REGISTER TASK"; textSize = 10f
            setOnClickListener { createTask() }
        }
        actions.addView(createButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(8) })
        composer.addView(actions)
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        root.addView(TextView(activity).apply {
            text = "TASKS · LIVE FROM REPOSITORY"; textSize = 10f
            letterSpacing = 0.1f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(8), 0, dp(6))
        })
        val scroll = ScrollView(activity).apply { clipToPadding = false }
        taskList = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(taskList, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = TextView(activity).apply {
            text = "GitHub Issues is shared. RUN NEXT supports only explicitly confirmed media-volume changes while Vyom stays foregrounded."
            textSize = 9f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(2), dp(8), dp(2), 0)
        }
        root.addView(footer)

        dialog.setContentView(root)
        dialog.setOnDismissListener {
            main.removeCallbacks(refreshRunnable)
            stopRunRequested.set(true)
            cancelPendingConfirmation?.invoke()
            pendingConfirmationDialog?.dismiss()
            worker.shutdownNow()
        }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.68f)
        }
        dialog.show()
        dialog.window?.setLayout(dp(560).coerceAtMost(activity.resources.displayMetrics.widthPixels - dp(24)),
            activity.resources.displayMetrics.heightPixels - dp(48))
        if (token.isBlank()) {
            status.text = "GitHub token missing. Open Settings and save a token with repository Issues read/write permission."
            taskList.addView(message("No GitHub connection. Tasks are not stored locally as a substitute."))
            taskList.addView(Button(activity).apply {
                text = "OPEN SETTINGS"
                setOnClickListener { dialog.dismiss(); onOpenSettings() }
            })
        } else {
            refreshRetryOverview()
            refresh()
            main.postDelayed(refreshRunnable, 15_000)
        }
    }

    private fun runNextTask() {
        if (busy || token.isBlank()) return
        if (activeClaim != null) {
            status.text = "Release the manually-held claim before starting RUN NEXT."
            return
        }
        busy = true
        stopRunRequested.set(false)
        runButton.isEnabled = false
        stopButton.isEnabled = true
        claimButton.isEnabled = false
        releaseClaimButton.isEnabled = false
        val hostActivity = activity as? MainActivity
        if (hostActivity == null || !hostActivity.factoryForegroundGate.isForeground()) {
            busy = false
            runButton.isEnabled = true
            stopButton.isEnabled = false
            claimButton.isEnabled = true
            status.text = "RUN NEXT requires the active Vyom screen."
            return
        }
        // Capture on the UI thread at the moment the owner taps RUN NEXT.
        // Capturing inside the executor would let a pause+resume that happens
        // while the job is queued adopt a fresh generation and revive old consent.
        val foregroundGeneration = hostActivity.factoryForegroundGate.generation()
        claimStatus.text = "RUNNER ACTIVE · FETCHING FRESH ISSUE AND VALIDATING ACTION…"
        worker.execute {
            val outcomes = try {
                val session = workerSession
                val journal = VyomFactoryExecutionJournal(activity.applicationContext)
                val retrySchedules = VyomFactoryRetryScheduleStore(activity.applicationContext)
                val adapter = VyomFactoryAndroidActionAdapter.forActivity(hostActivity, foregroundGeneration)

                // Run a read-only planner pass over the fresh canonical issue snapshot.
                // Persist each typed triage outcome before execution; a candidate is
                // never treated as executable until the runner independently validates it.
                val triageRegistry = VyomFactoryCapabilityRegistry(adapter.capabilities)
                val triageDecisions = VyomFactoryTaskTriage().evaluate(client.listTasks(), triageRegistry)
                triageDecisions.forEach { decision ->
                    if (decision.eventKey !in decision.task.metadata.events) {
                        client.updateTask(decision.task, additionalEvent = decision.eventKey)
                    }
                }

                // Repository complaints have no executable adapter. Hand the oldest
                // eligible one to the existing owner-reviewed proposal UI instead of
                // letting the Android-only runner silently skip it.
                val ownerReview = VyomFactorySpecialistRouting.nextOwnerReview(triageDecisions)
                if (ownerReview != null) {
                    main.post {
                        if (!dialog.isShowing) return@post
                        activeClaim = null
                        claimStatus.text = "OWNER REVIEW REQUIRED · #${ownerReview.task.number}"
                        status.text = "Repository complaint routed to source discovery. Choose a file and review the exact diff; nothing has been changed."
                        runButton.isEnabled = true
                        stopButton.isEnabled = false
                        claimButton.isEnabled = true
                        releaseClaimButton.isEnabled = false
                        busy = false
                        dialog.dismiss()
                        onProposeFix(ownerReview.task)
                    }
                    return@execute
                }

                val visitedIssues = mutableSetOf<Int>()
                val runner = VyomFactoryExecutionRunner(
                    claimNext = {
                        session.claimNext { remote ->
                            remote.number !in visitedIssues &&
                                isEligibleForRunner(remote, journal, retrySchedules, adapter)
                        }
                    },
                    isClaimCurrent = { session.isCurrent(it) },
                    releaseClaim = { session.release(it) },
                    fetchFreshTask = { number -> client.listTasks().firstOrNull { it.number == number } },
                    journal = journal,
                    adapters = VyomFactoryExecutionAdapterRegistry(listOf(adapter)),
                    retrySchedules = retrySchedules,
                    newAttemptId = { UUID.randomUUID().toString() },
                    prepareExecution = { envelope -> prepareAndroidAction(envelope) },
                    // Activity pause/resume invalidates this run permanently, even if
                    // the user returns before the worker reaches its next STOP check.
                    externalStopRequested = {
                        stopRunRequested.get() ||
                            !hostActivity.factoryForegroundGate.isForeground() ||
                            hostActivity.factoryForegroundGate.generation() != foregroundGeneration
                    }
                )
                VyomFactoryDrainCoordinator.drain(
                    runNext = { runner.runNext() },
                    shouldStop = {
                        stopRunRequested.get() ||
                            !hostActivity.factoryForegroundGate.isForeground() ||
                            hostActivity.factoryForegroundGate.generation() != foregroundGeneration
                    },
                    onOutcome = { next ->
                        val issueNumber = when (next) {
                            is VyomFactoryExecutionRunner.Outcome.Succeeded -> next.issueNumber
                            is VyomFactoryExecutionRunner.Outcome.Failed -> next.issueNumber
                            is VyomFactoryExecutionRunner.Outcome.Skipped -> next.issueNumber
                            is VyomFactoryExecutionRunner.Outcome.Fenced -> next.issueNumber
                            VyomFactoryExecutionRunner.Outcome.Busy,
                            VyomFactoryExecutionRunner.Outcome.NoWork -> null
                        }
                        if (issueNumber != null) visitedIssues.add(issueNumber)
                        val processed = visitedIssues.size
                        val progress = when (next) {
                            is VyomFactoryExecutionRunner.Outcome.Succeeded -> "Verified issue #${next.issueNumber}; checking the next eligible complaint."
                            is VyomFactoryExecutionRunner.Outcome.Failed -> "Issue #${next.issueNumber} failed (${next.category}); evaluating whether the batch can safely continue."
                            is VyomFactoryExecutionRunner.Outcome.Skipped -> "Issue #${next.issueNumber} skipped (${next.reason}); moving past it without claiming success."
                            is VyomFactoryExecutionRunner.Outcome.Fenced -> "Issue #${next.issueNumber} claim was fenced; stopping the batch."
                            VyomFactoryExecutionRunner.Outcome.Busy -> "Another Factory execution is active; stopping this batch."
                            VyomFactoryExecutionRunner.Outcome.NoWork -> "No further eligible issue remains."
                        }
                        main.post {
                            if (::dialog.isInitialized && dialog.isShowing) {
                                claimStatus.text = "DRAINING · $processed / ${VyomFactoryDrainPolicy.MAX_TASKS_PER_FOREGROUND_RUN}"
                                status.text = progress
                            }
                        }
                    }
                ).outcomes
            } catch (error: Exception) {
                main.post { onRunnerFailure(error.javaClass.simpleName) }
                null
            }
            outcomes.orEmpty().forEach { item ->
                val auditComment = VyomFactoryExecutionAudit.comment(item)
                val auditedIssue = when (item) {
                    is VyomFactoryExecutionRunner.Outcome.Succeeded -> item.issueNumber
                    is VyomFactoryExecutionRunner.Outcome.Failed -> item.issueNumber
                    else -> null
                }
                if (auditComment != null && auditedIssue != null) {
                    // Audit persistence is best-effort: it must never rewrite a verified side effect as failure.
                    runCatching {
                        val current = client.listTasks().firstOrNull { it.number == auditedIssue && it.state == "open" }
                        if (current != null) client.addEvent(auditedIssue, auditComment)
                    }
                }
            }
            main.post {
                if (!dialog.isShowing) return@post
                activeClaim = null
                val drained = outcomes.orEmpty()
                val verifiedCount = drained.count { it is VyomFactoryExecutionRunner.Outcome.Succeeded }
                val failedCount = drained.count { it is VyomFactoryExecutionRunner.Outcome.Failed }
                val skippedCount = drained.count { it is VyomFactoryExecutionRunner.Outcome.Skipped }
                if (outcomes == null) {
                    claimStatus.text = "RUNNER ERROR · NO SUCCESS REPORTED"
                    status.text = "Execution could not start: check GitHub connection and foreground state."
                } else if (drained.isEmpty()) {
                    claimStatus.text = "RUN STOPPED · NO TASK STARTED"
                    status.text = "STOP or foreground loss prevented the next task from starting."
                } else {
                    val suffix = if (drained.size == VyomFactoryDrainPolicy.MAX_TASKS_PER_FOREGROUND_RUN) {
                        " · BATCH LIMIT REACHED"
                    } else ""
                    claimStatus.text = "DRAIN FINISHED · $verifiedCount VERIFIED · $failedCount FAILED · $skippedCount SKIPPED$suffix"
                    status.text = when (val last = drained.last()) {
                        is VyomFactoryExecutionRunner.Outcome.Succeeded ->
                            "Last verified task #${last.issueNumber}: ${last.result}"
                        is VyomFactoryExecutionRunner.Outcome.Failed ->
                            "Last task #${last.issueNumber} failed (${last.category}); no success recorded."
                        is VyomFactoryExecutionRunner.Outcome.Skipped ->
                            "Last task #${last.issueNumber} skipped (${last.reason}); no action reported complete."
                        is VyomFactoryExecutionRunner.Outcome.Fenced ->
                            "Stopped at fenced claim #${last.issueNumber}; no successor was started."
                        VyomFactoryExecutionRunner.Outcome.Busy ->
                            "Another Factory run is already active."
                        VyomFactoryExecutionRunner.Outcome.NoWork ->
                            "No further eligible executable task remained."
                    }
                }
                runButton.isEnabled = true
                stopButton.isEnabled = false
                claimButton.isEnabled = true
                releaseClaimButton.isEnabled = false
                refreshRetryOverview()
                busy = false
            }
        }
    }

    /**
     * RUN NEXT must skip manual-review/terminal/not-yet-due tasks during selection,
     * otherwise one high-priority ineligible issue would starve every later task.
     * The task remains in the shared inbox for manual review or a later retry.
     */
    private fun isEligibleForRunner(
        task: VyomFactoryGitHubClient.Task,
        journal: VyomFactoryExecutionJournal,
        retrySchedules: VyomFactoryRetryScheduleStore,
        adapter: VyomFactoryAndroidActionAdapter
    ): Boolean {
        if (task.state != "open" || task.metadata.status !in setOf("open", "in_progress")) return false
        val envelope = VyomFactoryExecutionEnvelope.parse(task.body)
        if (!envelope.isExecutable ||
            envelope.target != VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION ||
            envelope.operation != "set_media_volume" ||
            envelope.arguments.has("confirmation_id")
        ) return false

        val history = journal.all().filter { it.issueNumber == task.number }
        if (history.any { it.operationKey == "legacy:unknown" }) return false
        val operationKey = "android_action:set_media_volume"
        val previous = history.lastOrNull { it.operationKey == operationKey } ?: return true
        return when (previous.state) {
            VyomFactoryExecutionJournal.State.SUCCEEDED,
            VyomFactoryExecutionJournal.State.INTERRUPTED,
            VyomFactoryExecutionJournal.State.CLAIMED,
            VyomFactoryExecutionJournal.State.RUNNING,
            VyomFactoryExecutionJournal.State.STOP_REQUESTED -> false
            VyomFactoryExecutionJournal.State.FAILED -> adapter.isIdempotent &&
                retrySchedules.all().any {
                    it.issueNumber == task.number && it.operationKey == operationKey &&
                        it.nextAttemptAt <= System.currentTimeMillis()
                }
        }
    }

    private fun prepareAndroidAction(
        envelope: VyomFactoryExecutionEnvelope
    ): VyomFactoryExecutionEnvelope? {
        if (envelope.target != VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION ||
            envelope.operation != "set_media_volume"
        ) return envelope
        val percent = envelope.arguments.opt("percent") as? Int ?: return null
        if (percent !in 0..100) return null
        val approvalId = requestVolumeApproval(percent) ?: return null
        return envelope.copy(
            arguments = org.json.JSONObject(envelope.arguments.toString())
                .put("confirmation_id", approvalId)
        )
    }

    /** Runs on the Factory worker thread; the actual consent dialog is shown on Android's UI thread. */
    private fun requestVolumeApproval(percent: Int): String? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val hostActivity = activity as? MainActivity ?: return null
        val gate = hostActivity.factoryForegroundGate
        val epoch = gate.generation()
        if (!gate.isForeground()) return null
        val latch = CountDownLatch(1)
        val result = AtomicReference<String?>(null)
        val finished = AtomicBoolean(false)
        val actionRef = AtomicReference<(() -> Unit)?>(null)
        fun finish(value: String?) {
            if (finished.compareAndSet(false, true)) {
                result.set(value)
                latch.countDown()
            }
        }
        main.post {
            if (!dialog.isShowing || !gate.isForeground() || gate.generation() != epoch) {
                finish(null)
                return@post
            }
            val prompt = AlertDialog.Builder(activity)
                .setTitle("Confirm Android action")
                .setMessage("Set media volume to ${percent}%?\n\nThis changes only the media stream volume. Vyom will verify the resulting level. This approval is one-time.")
                .setNegativeButton("CANCEL") { _, _ -> finish(null) }
                .setPositiveButton("CONFIRM & SET VOLUME") { _, _ ->
                    val approval = if (dialog.isShowing && gate.isForeground() && gate.generation() == epoch) {
                        runCatching { hostActivity.factoryOneTimeApprovals.issue() }.getOrNull()
                    } else null
                    finish(approval)
                }
                .create()
            prompt.setOnCancelListener { finish(null) }
            prompt.setOnDismissListener { finish(null) }
            pendingConfirmationDialog = prompt
            val cancel: () -> Unit = {
                finish(null)
                prompt.dismiss()
            }
            actionRef.set(cancel)
            cancelPendingConfirmation = cancel
            hostActivity.factoryPendingConfirmationCancel = cancel
            prompt.show()
        }
        val completed = try {
            latch.await(30, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!completed) {
            finish(null)
            main.post { pendingConfirmationDialog?.dismiss() }
        }
        main.post {
            val cancel = actionRef.get()
            if (hostActivity.factoryPendingConfirmationCancel === cancel) hostActivity.factoryPendingConfirmationCancel = null
            if (cancelPendingConfirmation === cancel) cancelPendingConfirmation = null
            if (pendingConfirmationDialog?.isShowing != true) pendingConfirmationDialog = null
        }
        return result.get()
    }

    private fun chooseScreenshotIssue(screenshot: File) {
        if (busy || !screenshot.isFile) return
        busy = true
        status.text = "Loading open Factory issues for screenshot attachment…"
        worker.execute {
            try {
                val tasks = client.listTasks().filter { it.state == "open" && it.metadata.status != "paused" }
                main.post {
                    if (!dialog.isShowing) return@post
                    busy = false
                    if (tasks.isEmpty()) {
                        status.text = "No open Factory issue is available. Screenshot remains in app-private storage."
                        return@post
                    }
                    val labels = tasks.map { "#${it.number} · ${it.metadata.priority} · ${it.title}" }.toTypedArray()
                    AlertDialog.Builder(activity)
                        .setTitle("Choose the exact issue")
                        .setItems(labels) { _, which ->
                            tasks.getOrNull(which)?.let { confirmScreenshotAttachment(it, screenshot) }
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    busy = false
                    status.text = "Could not load issues; screenshot remains private."
                }
            }
        }
    }

    private fun confirmScreenshotAttachment(task: VyomFactoryGitHubClient.Task, screenshot: File) {
        AlertDialog.Builder(activity)
            .setTitle("Attach screenshot to #${task.number}?")
            .setMessage("Target: ${task.title}\n\nVyom checks that the repository is private before uploading. Nothing is sent until you tap ATTACH. The screenshot will be linked in this exact issue.")
            .setPositiveButton("ATTACH") { _, _ -> uploadScreenshot(task, screenshot) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun uploadScreenshot(task: VyomFactoryGitHubClient.Task, screenshot: File) {
        if (busy || !screenshot.isFile) return
        busy = true
        screenshotAttachButton?.isEnabled = false
        status.text = "Checking repository privacy before owner-approved upload…"
        worker.execute {
            try {
                val fresh = client.listTasks().firstOrNull { it.number == task.number && it.state == "open" }
                    ?: throw IllegalStateException("Selected issue is no longer open")
                val attachment = client.attachScreenshot(fresh.number, screenshot)
                main.post {
                    if (!dialog.isShowing) return@post
                    if (attachment.issueCommentRecorded) {
                        screenshot.delete()
                        screenshotAttachButton?.text = "ATTACHED TO #${fresh.number}"
                        status.text = "Screenshot attached to #${fresh.number}. SHA-256 ${attachment.sha256}. Local copy removed after confirmed issue comment."
                    } else {
                        runCatching {
                            VyomFactoryScreenshotEvidenceStore(File(activity.filesDir, "factory-evidence"))
                                .recordUploaded(screenshot, attachment.url, attachment.commitSha, attachment.sha256)
                        }
                        screenshotAttachButton?.isEnabled = false
                        status.text = "Image uploaded to the private repository, but the issue comment failed. Do not retry upload; manually add this link to #${fresh.number}: ${attachment.url}"
                    }
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    screenshotAttachButton?.isEnabled = true
                    status.text = "Screenshot not uploaded · ${error.message?.take(180) ?: error.javaClass.simpleName}. Local copy is preserved; no automatic retry."
                    busy = false
                }
            }
        }
    }

    private fun workerFailureTaskId(operation: String, value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val stable = digest.take(12).joinToString("") { "%02x".format(it) }
        return "$operation:$stable"
    }

    private fun shouldReportSkillWorkerFailure(error: Exception): Boolean =
        error is IOException || Regex("HTTP 5[0-9]{2}").containsMatchIn(error.message.orEmpty())

    private fun discoverSkillRepositories() {
        if (busy) return
        val query = EditText(activity).apply {
            hint = "Skill topic or repository keyword"
            setSingleLine(true)
        }
        AlertDialog.Builder(activity)
            .setTitle("Discover public skill repositories")
            .setMessage("Search GitHub repositories. Only root SKILL.md files with an approved SPDX license can be imported.")
            .setView(query)
            .setPositiveButton("SEARCH") { _, _ ->
                val term = query.text.toString().trim()
                busy = true
                status.text = "Searching public GitHub skill repositories…"
                worker.execute {
                    try {
                        val candidates = VyomFactorySkillRepositoryClient().search(term)
                        main.post {
                            if (!dialog.isShowing) return@post
                            busy = false
                            if (candidates.isEmpty()) {
                                status.text = "No public skill repositories matched. No files were installed."
                                return@post
                            }
                            val labels = candidates.map {
                                "${it.fullName} · ★ ${it.stars}\n${it.description}".trim()
                            }.toTypedArray()
                            AlertDialog.Builder(activity)
                                .setTitle("Choose a repository to inspect")
                                .setItems(labels) { _, which ->
                                    candidates.getOrNull(which)?.let(::inspectSkillRepository)
                                }
                                .setNegativeButton("CANCEL", null)
                                .show()
                        }
                    } catch (error: Exception) {
                        main.post {
                            if (!dialog.isShowing) return@post
                            busy = false
                            status.text = "Skill discovery failed · ${error.javaClass.simpleName}"
                            if (shouldReportSkillWorkerFailure(error)) {
                                onWorkerFailure("skill_repository", workerFailureTaskId("search", term), error.javaClass.simpleName)
                            }
                        }
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun inspectSkillRepository(candidate: VyomFactorySkillRepositoryClient.Candidate) {
        if (busy) return
        busy = true
        status.text = "Inspecting ${candidate.fullName}: license, immutable revision and SKILL.md…"
        worker.execute {
            try {
                val preview = VyomFactorySkillRepositoryClient().inspect(candidate.fullName)
                main.post {
                    if (!dialog.isShowing) return@post
                    busy = false
                    val previewText = TextView(activity).apply {
                        text = "SOURCE: ${preview.sourceUrl}\nCOMMIT: ${preview.commitSha}\nLICENSE: ${preview.license}\nSHA-256: ${VyomFactorySkillPolicy.sha256(preview.skillMarkdown.toByteArray(Charsets.UTF_8))}\n\nSECURITY: This document is untrusted instructions. It will be stored as inert Markdown only; Vyom will not execute scripts or follow it automatically. Review the full content before installing.\n\n${preview.skillMarkdown}"
                        textSize = 11f
                        setTextColor(VyomDesignSpec.TEXT_PRIMARY)
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    }
                    val scroll = ScrollView(activity).apply { addView(previewText) }
                    AlertDialog.Builder(activity)
                        .setTitle("Review ${preview.repository}")
                        .setView(scroll)
                        .setPositiveButton("INSTALL REVIEWED SKILL") { _, _ -> installSkillRepository(preview) }
                        .setNegativeButton("CANCEL", null)
                        .show()
                    status.text = "Review the complete SKILL.md and tap INSTALL only if you approve it."
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    busy = false
                    status.text = "Skill inspection rejected · ${error.message?.take(140) ?: error.javaClass.simpleName}"
                    if (shouldReportSkillWorkerFailure(error)) {
                        onWorkerFailure("skill_repository", workerFailureTaskId("inspect", candidate.fullName), error.javaClass.simpleName)
                    }
                }
            }
        }
    }

    private fun installSkillRepository(preview: VyomFactorySkillRepositoryClient.Preview) {
        if (busy) return
        busy = true
        status.text = "Installing reviewed skill as inert app-private Markdown…"
        worker.execute {
            try {
                val installed = VyomFactorySkillStore(File(activity.filesDir, "factory-skills"))
                    .install(preview, ownerConfirmed = true)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "SKILL INSTALLED · ${installed.repository} · commit ${installed.commitSha.take(12)} · SHA-256 ${installed.sha256.take(16)}… · validation PASS. Content remains inert; no scripts were executed."
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Skill installation failed · ${error.javaClass.simpleName}; no success recorded."
                    busy = false
                }
            }
        }
    }

    private fun claimNextTask() {
        if (busy || token.isBlank()) return
        busy = true
        claimButton.isEnabled = false
        releaseClaimButton.isEnabled = false
        runButton.isEnabled = false
        claimStatus.text = "REFRESHING REGISTRY · CLAIMING NEXT OPEN TASK…"
        worker.execute {
            try {
                val claim = workerSession.claimNext()
                main.post {
                    if (!dialog.isShowing) return@post
                    activeClaim = claim
                    if (claim == null) {
                        claimStatus.text = "NO ELIGIBLE OPEN TASK · inbox checked against fresh GitHub data"
                        status.text = "No open queued task is currently claimable."
                    } else {
                        claimStatus.text = "CLAIMED #${claim.task.issueNumber} · DEVICE-LOCAL · NOT EXECUTING"
                        status.text = "Claim acquired for #${claim.task.issueNumber}; execution has not started."
                    }
                    releaseClaimButton.isEnabled = claim != null
                    runButton.isEnabled = claim == null
                    claimButton.isEnabled = true
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    claimStatus.text = "CLAIM FAILED · NO NEW CLAIM REPORTED"
                    status.text = errorMessage(error)
                    claimButton.isEnabled = true
                    releaseClaimButton.isEnabled = activeClaim != null
                    runButton.isEnabled = activeClaim == null
                    busy = false
                }
            }
        }
    }

    private fun releaseActiveClaim() {
        if (busy) return
        val claim = activeClaim ?: return
        busy = true
        claimButton.isEnabled = false
        releaseClaimButton.isEnabled = false
        runButton.isEnabled = false
        worker.execute {
            try {
                val released = workerSession.release(claim)
                main.post {
                    if (!dialog.isShowing) return@post
                    if (released) {
                        activeClaim = null
                        claimStatus.text = "CLAIM RELEASED · TASK REMAINS QUEUED"
                        status.text = "Claim released. Task #${claim.task.issueNumber} remains eligible for retry."
                    } else {
                        claimStatus.text = "CLAIM NO LONGER OWNED · REFRESH REQUIRED"
                        status.text = "Lease token is no longer current; no successor claim was changed."
                    }
                    claimButton.isEnabled = true
                    runButton.isEnabled = activeClaim == null
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    claimButton.isEnabled = true
                    releaseClaimButton.isEnabled = activeClaim != null
                    runButton.isEnabled = activeClaim == null
                    busy = false
                }
            }
        }
    }

    private fun refresh() {
        if (busy || token.isBlank() || !::status.isInitialized) return
        busy = true
        status.text = "Refreshing live tasks from GitHub…"
        worker.execute {
            try {
                val tasks = client.listTasks()
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "GitHub Issues · ${tasks.size} Factory tasks loaded"
                    render(tasks)
                    refreshRetryOverview()
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    taskList.removeAllViews()
                    taskList.addView(message("Could not load the shared registry. No local fake tasks were substituted."))
                    busy = false
                }
            }
        }
    }

    /** Displays persisted retry eligibility only; this is not a scheduler. */
    private fun refreshRetryOverview() {
        if (!::retryStatus.isInitialized) return
        try {
            val summary = VyomFactoryRetryOverview.summarize(
                VyomFactoryRetryScheduleStore(activity.applicationContext).all(),
                System.currentTimeMillis()
            )
            retryStatus.text = summary.label
            retryStatus.contentDescription =
                "Retries total ${summary.total}, due ${summary.due}, waiting ${summary.waiting}. Foreground RUN NEXT is required."
            val history = VyomFactoryExecutionHistory.summarize(
                VyomFactoryExecutionJournal(activity.applicationContext).all()
            )
            historyStatus.text = (listOf(history.label) + history.latest.map { it.label }).joinToString("\\n")
            historyStatus.contentDescription = "${history.total} persisted attempts; ${history.active} active, ${history.succeeded} verified, ${history.failed} failed, ${history.interrupted} interrupted. Latest attempt rows are redacted."
        } catch (error: Exception) {
            retryStatus.text = "RETRY STATUS UNAVAILABLE · REVIEW REQUIRED"
            retryStatus.contentDescription = "Retry schedule could not be read safely."
            historyStatus.text = "RUN HISTORY UNAVAILABLE · REVIEW REQUIRED"
            historyStatus.contentDescription = "Execution journal could not be read safely."
        }
    }

    private fun createTask() {
        if (busy) return
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        if (title.length < 4 || description.isBlank()) {
            status.text = "Enter a title (at least 4 characters) and a description."
            return
        }
        busy = true
        createButton.isEnabled = false
        status.text = "Registering complaint in GitHub Issues…"
        val prefs = activity.getSharedPreferences("vyom_factory", Activity.MODE_PRIVATE)
        val sourceId = pendingSourceId ?: prefs.getString("pending_draft_id", null) ?: UUID.randomUUID().toString()
        pendingSourceId = sourceId
        prefs.edit().putString("pending_draft_id", sourceId).apply()
        worker.execute {
            try {
                val task = client.createTask(title, description, selectedPriority, "factory", sourceId)
                VyomFactoryTaskStore(activity).enqueue(
                    VyomFactoryTaskStore.QueuedTask(task.number, task.title, task.metadata.priority, sourceId, System.currentTimeMillis())
                )
                main.post {
                    if (!dialog.isShowing) return@post
                    titleInput.setText("")
                    descriptionInput.setText("")
                    pendingSourceId = null
                    prefs.edit().remove("pending_draft_id").apply()
                    status.text = "Registered as #${task.number} · saved to shared registry"
                    createButton.isEnabled = true
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    createButton.isEnabled = true
                    busy = false
                }
            }
        }
    }

    private fun render(tasks: List<VyomFactoryGitHubClient.Task>) {
        val orderedTasks = VyomFactoryTaskOrdering.sort(tasks)
        taskList.removeAllViews()
        if (orderedTasks.isEmpty()) {
            taskList.addView(message("No Factory complaints registered yet. Use the form above to create the first shared task."))
            return
        }
        orderedTasks.forEach { task ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    setColor(0x3316263B); cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), 0x333A7891)
                }
            }
            card.addView(TextView(activity).apply {
                text = "#${task.number} · ${task.title}"; textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            })
            card.addView(TextView(activity).apply {
                text = "${task.metadata.priority} · ${task.metadata.status.uppercase()} · UPDATED ${task.updatedAt}"
                textSize = 9f; typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(4), 0, dp(4))
            })
            val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
            if (description.isNotBlank()) card.addView(TextView(activity).apply {
                text = description; textSize = 11f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                maxLines = 3; setPadding(0, 0, 0, dp(6))
            })
            val controls = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            fun action(label: String, next: String) {
                controls.addView(Button(activity).apply {
                    text = label; textSize = 8f; isAllCaps = true
                    setOnClickListener { update(task, next, task.metadata.priority) }
                }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { rightMargin = dp(3) })
            }
            when (task.metadata.status) {
                "open" -> { action("START", "in_progress"); action("PAUSE", "paused"); action("BLOCK", "blocked") }
                "in_progress" -> {
                    action("PAUSE", "paused")
                    action("BLOCK", "blocked")
                    if (VyomFactoryVerificationPolicy.canComplete(
                            task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim(), task.metadata.events
                        )) {
                        action("DONE", "completed")
                    } else {
                        controls.addView(Button(activity).apply {
                            text = "VERIFY"; textSize = 8f
                            setOnClickListener { showVerificationDialog(task) }
                        }, LinearLayout.LayoutParams(dp(72), dp(36)))
                    }
                }
                "paused" -> { action("RESUME", "in_progress"); action("CANCEL", "cancelled") }
                "blocked" -> { action("REOPEN", "open"); action("CANCEL", "cancelled") }
                else -> Unit
            }
            val priority = Button(activity).apply {
                text = "PRIORITY"; textSize = 8f
                setOnClickListener { choosePriority(task) }
            }
            controls.addView(priority, LinearLayout.LayoutParams(dp(78), dp(36)))
            controls.addView(Button(activity).apply {
                text = "EDIT"; textSize = 8f
                setOnClickListener { editTask(task) }
            }, LinearLayout.LayoutParams(dp(64), dp(36)).apply { leftMargin = dp(3) })
            card.addView(controls)
            if (task.state == "open" && task.metadata.status in setOf("open", "in_progress")) {
                card.addView(Button(activity).apply {
                    text = "PROPOSE REVIEWED CODE FIX"
                    textSize = 9f
                    setOnClickListener {
                        dialog.dismiss()
                        onProposeFix(task)
                    }
                }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(4) })
            }
            card.setOnClickListener { showTaskHistory(task) }
            taskList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun showVerificationDialog(task: VyomFactoryGitHubClient.Task) {
        val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
        val probe = VyomFactoryVerificationPolicy.assignedProbe(description)
        if (probe == null) {
            AlertDialog.Builder(activity)
                .setTitle("Verification probe required")
                .setMessage("Edit this complaint and replace the UNASSIGNED Verification probe with one exact, observable device check before recording a result.")
                .setPositiveButton("OK", null).show()
            return
        }
        val evidence = EditText(activity).apply {
            hint = "What did you observe on the device? Do not include private data."
            minLines = 3
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        AlertDialog.Builder(activity)
            .setTitle("OWNER DEVICE VERIFICATION")
            .setMessage("Assigned probe:\n$probe\n\nOnly choose PASS after checking the real device. Evidence is added to the shared GitHub issue.")
            .setView(evidence)
            .setPositiveButton("PROBE PASSED") { _, _ -> recordVerification(task, true, evidence.text.toString()) }
            .setNegativeButton("PROBE FAILED") { _, _ -> recordVerification(task, false, evidence.text.toString()) }
            .setNeutralButton("CANCEL", null)
            .show()
    }

    private fun recordVerification(task: VyomFactoryGitHubClient.Task, passed: Boolean, evidence: String) {
        if (busy) return
        busy = true
        status.text = "Recording device probe result for #${task.number}…"
        worker.execute {
            try {
                val fresh = client.listTasks().firstOrNull { it.number == task.number }
                    ?: throw IllegalArgumentException("Complaint #${task.number} is no longer in the shared registry")
                client.recordVerificationResult(fresh, passed, evidence)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = if (passed) "Probe PASS recorded. DONE is now available for #${task.number}."
                        else "Probe FAIL recorded. Complaint #${task.number} remains open."
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    busy = false
                }
            }
        }
    }

    private fun showTaskHistory(task: VyomFactoryGitHubClient.Task) {
        status.text = "Loading history for task #${task.number}…"
        worker.execute {
            try {
                val comments = client.listEvents(task.number)
                val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
                val history = if (comments.isEmpty()) "No comments or event notes recorded yet."
                    else comments.joinToString("\\n\\n")
                val details = "Status: ${task.metadata.status} · Priority: ${task.metadata.priority}\\n\\n" +
                    description + "\\n\\nEVENT HISTORY\\n" + history
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Task #${task.number} history loaded."
                    AlertDialog.Builder(activity).setTitle("Factory task #${task.number}")
                        .setMessage(details)
                        .setPositiveButton("OPEN GITHUB") { _, _ ->
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.url)))
                        }.setNegativeButton("CLOSE", null).show()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                }
            }
        }
    }

    private fun editTask(task: VyomFactoryGitHubClient.Task) {
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        val title = EditText(activity).apply {
            setSingleLine(true)
            setText(task.title.replace(Regex("^\\[VYOM FACTORY\\]\\[[Pp][0-3]\\]\\s*"), ""))
            hint = "Task title"
        }
        val description = EditText(activity).apply {
            minLines = 3
            maxLines = 8
            setText(task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim())
            hint = "Complaint details"
        }
        form.addView(title)
        form.addView(description)
        AlertDialog.Builder(activity).setTitle("Edit Factory task #${task.number}")
            .setView(form)
            .setPositiveButton("SAVE") { _, _ ->
                val newTitle = title.text.toString().trim()
                val newDescription = description.text.toString().trim()
                if (newTitle.length < 4 || newDescription.isBlank()) {
                    status.text = "Edit not saved: title and description are required."
                } else {
                    saveEdit(task, newTitle, newDescription)
                }
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun saveEdit(task: VyomFactoryGitHubClient.Task, title: String, description: String) {
        if (busy) return
        busy = true
        status.text = "Saving edits to task #${task.number}…"
        worker.execute {
            try {
                client.updateTask(task, newTitle = title, newDescription = description)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Task #${task.number} edited and saved."
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    busy = false
                }
            }
        }
    }

    private fun choosePriority(task: VyomFactoryGitHubClient.Task) {
        val values = arrayOf("P0", "P1", "P2", "P3")
        AlertDialog.Builder(activity).setTitle("Set task priority")
            .setSingleChoiceItems(values, values.indexOf(task.metadata.priority), null)
            .setPositiveButton("SAVE") { d, _ ->
                val index = (d as AlertDialog).listView.checkedItemPosition
                if (index in values.indices) update(task, task.metadata.status, values[index])
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun update(task: VyomFactoryGitHubClient.Task, nextStatus: String, priority: String) {
        if (busy) return
        busy = true
        status.text = "Saving task #${task.number}…"
        worker.execute {
            try {
                client.updateTask(task, newPriority = priority, newStatus = nextStatus)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Task #${task.number} updated and saved."
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    busy = false
                }
            }
        }
    }

    private fun message(value: String) = TextView(activity).apply {
        text = value; textSize = 12f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        setPadding(dp(12), dp(18), dp(12), dp(18))
    }

    private fun errorMessage(error: Exception): String {
        val api = error as? VyomFactoryGitHubClient.FactoryApiException
        return when (api?.code) {
            401 -> "GitHub token rejected (401). Replace it in Settings."
            403 -> "GitHub denied access (403). Token needs Issues read/write permission for this repository."
            404 -> "Repository or issue endpoint not found (404). Check repository access."
            422 -> "GitHub rejected the task data (422). Check title, labels and issue permissions."
            else -> error.message?.take(180) ?: "Factory request failed."
        }
    }
}
