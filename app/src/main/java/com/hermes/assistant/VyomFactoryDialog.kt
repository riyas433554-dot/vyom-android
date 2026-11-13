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
import java.util.concurrent.ConcurrentHashMap

/** Live Factory workspace. All visible task data is read from GitHub Issues, never demo fixtures. */
internal class VyomFactoryDialog(
    private val activity: Activity,
    private val token: String,
    private val onOpenSettings: () -> Unit,
    private val onProposeFix: (VyomFactoryGitHubClient.Task) -> Unit = {},
    private val onRunnerFailure: (String) -> Unit = {},
    private val pendingScreenshot: File? = null,
    private val onWorkerFailure: (String, String, String) -> Unit = { _, _, _ -> },
    private val onUpdateApk: () -> Unit = {}
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
    private val client by lazy {
        VyomFactoryGitHubClient(
            token,
            VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
        )
    }
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
    private val pendingRepositoryApprovals = ConcurrentHashMap<Int, VyomFactoryRepositoryChangeAdapter.ApprovedPlan>()
    private val dismissedRepositoryReviews = ConcurrentHashMap<Int, Boolean>()
    private var pendingConfirmationDialog: AlertDialog? = null
    private var cancelPendingConfirmation: (() -> Unit)? = null
    private lateinit var claimStatus: TextView
    private lateinit var retryStatus: TextView
    private lateinit var historyStatus: TextView
    private lateinit var laneMetricsStatus: TextView
    private var dispatchObserver: androidx.lifecycle.Observer<List<androidx.work.WorkInfo>>? = null
    private val progressViews = mutableMapOf<Int, Pair<TextView, android.widget.ProgressBar>>()
    private var screenshotAttachButton: Button? = null
    private var activeClaim: VyomFactoryWorkerClaimCoordinator.Claim? = null
    private val workerPool: VyomFactoryWorkerPool by lazy {
        val store = VyomFactoryTaskStore(activity.applicationContext)
        val leases = VyomFactoryWorkerLeaseStore(activity.applicationContext)
        VyomFactoryWorkerPool(
            identityStore = VyomFactoryWorkerIdentityStore(activity.applicationContext),
            taskStore = store,
            coordinator = VyomFactoryWorkerClaimCoordinator(store, leases),
            fetchRemoteTasks = { client.listTasks() }
        )
    }
    private val workerSessions: List<VyomFactoryWorkerSession>
        get() = workerPool.sessions
    private val workerProposalStore: VyomFactoryWorkerProposalStore by lazy {
        VyomFactoryWorkerProposalStore(
            File(activity.filesDir, VyomFactoryWorkerDispatchWorker.PROPOSAL_DIRECTORY)
        )
    }
    private val workerStopStore by lazy { VyomFactoryWorkerStopStore(activity.applicationContext) }

    private fun sessionFor(claim: VyomFactoryWorkerClaimCoordinator.Claim): VyomFactoryWorkerSession =
        workerSessions.firstOrNull { it.workerIdentity.workerId == claim.lease.workerId &&
            it.workerIdentity.lane == claim.lease.lane &&
            it.workerIdentity.slotIndex == claim.lease.workerSlot }
            ?: throw IllegalStateException("Claim worker identity is not in the persisted pool")

    private fun claimNextFromAnyLane(
        eligible: (VyomFactoryGitHubClient.Task) -> Boolean = { true }
    ): VyomFactoryWorkerClaimCoordinator.Claim? {
        if (workerStopStore.isStopped()) return null
        val snapshot = client.listTasks()
        val orderedSessions = workerSessions.sortedWith(
            compareBy<VyomFactoryWorkerSession> { it.workerIdentity.lane.priorityRank }
                .thenBy { it.workerIdentity.slotIndex }
        )
        for (session in orderedSessions) {
            val lane = session.workerIdentity.lane
            val claim = session.claimNext(snapshot) { remote ->
                val origin = if (remote.metadata.source in setOf("vyom_chat", "vyom_voice")) {
                    VyomFactoryLaneRouter.Origin.DIRECT_OWNER_COMMAND
                } else {
                    VyomFactoryLaneRouter.Origin.COMPLAINT
                }
                val decision = VyomFactoryLaneRouter.route(
                    VyomFactoryLaneRouter.Request(origin, remote.title + " " + remote.body)
                )
                decision.lane == lane &&
                    (lane == VyomFactoryLaneRouter.Lane.INSTANT ||
                        lane == VyomFactoryLaneRouter.Lane.CUSTOM || decision.mayStartPlanning) &&
                    eligible(remote)
            }
            if (claim != null) return claim
        }
        return null
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
        root.addView(Button(activity).apply {
            text = "CHECK SIGNED APK UPDATE"
            textSize = 10f
            setOnClickListener { onUpdateApk() }
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(8) })

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

        workerPanel.addView(Button(activity).apply {
            text = "BIND TEST WORKER REPOSITORY"
            textSize = 9f
            setOnClickListener {
                VyomFactoryWorkerBindingDialog(
                    activity,
                    onStatus = { message ->
                        if (::dialog.isInitialized && dialog.isShowing) status.text = message
                    }
                ).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        runButton = Button(activity).apply {
            text = "RUN NEXT VERIFIED ACTION"; textSize = 9f
            setOnClickListener { runNextTask() }
        }
        workerPanel.addView(runButton, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(6) })
        stopButton = Button(activity).apply {
            text = "STOP ALL WORKERS"; textSize = 9f; isEnabled = true
            setOnClickListener {
                workerStopStore.stopAll()
                stopRunRequested.set(true)
                cancelPendingConfirmation?.invoke()
                status.text = "GLOBAL STOP is active. Foreground and background workers will not claim or start another task."
                isEnabled = false
            }
        }
        workerPanel.addView(stopButton, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(4) })
        workerPanel.addView(Button(activity).apply {
            text = "RESUME WORKERS"; textSize = 9f
            setOnClickListener {
                workerStopStore.resumeAll()
                stopRunRequested.set(false)
                stopButton.isEnabled = true
                status.text = "Global STOP cleared. Worker dispatch may resume under the existing owner approvals."
            }
        }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(4) })
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
        laneMetricsStatus = TextView(activity).apply {
            text = "LANES · LOADING"
            textSize = 9f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(0, dp(6), 0, 0)
            contentDescription = "Lane-wise active workers, execution runs and failures"
            maxLines = VyomFactoryLaneRouter.Lane.values().size + 1
        }
        workerPanel.addView(laneMetricsStatus)
        workerPanel.addView(Button(activity).apply {
            text = "DISCOVER / INSTALL SKILL REPOSITORY"
            textSize = 9f
            setOnClickListener { discoverSkillRepositories() }
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        workerPanel.addView(Button(activity).apply {
            text = "INSPECT / REGISTER ANY GITHUB REPOSITORY"
            textSize = 9f
            setOnClickListener {
                VyomFactoryRepositoryReviewDialog(
                    activity,
                    onStatus = { message ->
                        if (::dialog.isInitialized && dialog.isShowing) status.text = message
                    }
                ).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        val workerToggle = Button(activity).apply {
            text = "WORKER CONTROLS · SHOW"
            textSize = 9f
            setOnClickListener {
                val expanded = workerPanel.visibility != android.view.View.VISIBLE
                workerPanel.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE
                text = if (expanded) "WORKER CONTROLS · HIDE" else "WORKER CONTROLS · SHOW"
            }
        }
        root.addView(workerToggle, LinearLayout.LayoutParams(-1, dp(38)).apply { bottomMargin = dp(4) })
        workerPanel.visibility = android.view.View.GONE
        root.addView(workerPanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

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
        val composerToggle = Button(activity).apply {
            text = "NEW TASK / COMPLAINT · SHOW"
            textSize = 9f
            setOnClickListener {
                val expanded = composer.visibility != android.view.View.VISIBLE
                composer.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE
                text = if (expanded) "NEW TASK / COMPLAINT · HIDE" else "NEW TASK / COMPLAINT · SHOW"
            }
        }
        root.addView(composerToggle, LinearLayout.LayoutParams(-1, dp(38)).apply { bottomMargin = dp(4) })
        composer.visibility = android.view.View.GONE
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })

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
            (activity as? androidx.lifecycle.LifecycleOwner)?.let {
                dispatchObserver?.let { observer ->
                    androidx.work.WorkManager.getInstance(activity)
                        .getWorkInfosForUniqueWorkLiveData(VyomFactoryWorkerDispatchScheduler.WORK_NAME)
                        .removeObserver(observer)
                }
            }
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
        observeWorkerDispatch()
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

    private fun observeWorkerDispatch() {
        val owner = activity as? androidx.lifecycle.LifecycleOwner ?: return
        val liveData = androidx.work.WorkManager.getInstance(activity)
            .getWorkInfosForUniqueWorkLiveData(VyomFactoryWorkerDispatchScheduler.WORK_NAME)
        val observer = androidx.lifecycle.Observer<List<androidx.work.WorkInfo>> { infos ->
            if (!::claimStatus.isInitialized || !dialog.isShowing || busy || activeClaim != null) return@Observer
            val state = infos.firstOrNull()?.state
            claimStatus.text = when (state) {
                androidx.work.WorkInfo.State.ENQUEUED -> "WORKER QUEUED · waiting for network/constraints"
                androidx.work.WorkInfo.State.RUNNING -> "WORKER RUNNING · dispatch active; task completion not yet proven"
                androidx.work.WorkInfo.State.SUCCEEDED -> "DISPATCH FINISHED · inspect RUNS/proposals; this is not task completion"
                androidx.work.WorkInfo.State.FAILED -> "WORKER FAILED · authorization, binding or execution error"
                androidx.work.WorkInfo.State.CANCELLED -> "WORKER CANCELLED"
                else -> "WORKER IDLE · no active background dispatch"
            }
        }
        dispatchObserver = observer
        liveData.observe(owner, observer)
    }

    private fun runNextTask() {
        if (workerStopStore.isStopped()) {
            status.text = "GLOBAL STOP is active. Tap RESUME WORKERS before starting a run."
            return
        }
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
                val sessions = workerSessions
                val journal = VyomFactoryExecutionJournal(activity.applicationContext)
                val retrySchedules = VyomFactoryRetryScheduleStore(activity.applicationContext)
                val adapter = VyomFactoryAndroidActionAdapter.forActivity(hostActivity, foregroundGeneration)
                val detectorJournal = VyomFactoryStageDetectorJournal(activity.applicationContext)
                val stageDetector = createStageDetector(detectorJournal)

                // Planning hook: inspect every fresh open task before triage/claim selection.
                VyomFactoryPlanningStageHook(stageDetector::onPlanning).inspect(client.listTasks())

                // This is the live RUN NEXT planning path: detector and triage share
                // one fresh GitHub snapshot. Active device-local leases are excluded.
                val planningSnapshot = client.listTasks()
                VyomFactoryLivePlanningStage(stageDetector::onPlanning).inspect(
                    planningSnapshot,
                    sessions.first().activeClaimedIssueNumbers()
                )

                // Run a read-only planner pass over that same canonical issue snapshot.
                // Persist each typed triage outcome before execution; a candidate is
                // never treated as executable until the runner independently validates it.
                val triageRegistry = VyomFactoryCapabilityRegistry(adapter.capabilities)
                val triageDecisions = VyomFactoryTaskTriage().evaluate(planningSnapshot, triageRegistry)
                // Repository model review is strictly read-only until the owner approves.
                // Do not append even a triage event to those issues before approval/rejection.
                val repositoryReviewIssues = triageDecisions.filter { decision ->
                    val plan = decision.plan
                    plan is VyomFactoryTaskPlanner.Plan.NeedsOwnerReview &&
                        plan.domain == VyomFactoryCapabilityRegistry.Domain.REPOSITORY
                }.map { it.task.number }.toSet()
                triageDecisions.forEach { decision ->
                    if (decision.task.number in repositoryReviewIssues) return@forEach
                    if (decision.eventKey !in decision.task.metadata.events) {
                        client.updateTask(decision.task, additionalEvent = decision.eventKey)
                    }
                }

                // Repository proposals are reviewed locally before the repository
                // adapter becomes eligible. The legacy self-modification UI is not used here.
                val ownerReview = VyomFactorySpecialistRouting.nextOwnerReview(
                    decisions = triageDecisions,
                    isRunnerEligible = { task -> isEligibleForRunner(task, journal, retrySchedules, adapter) },
                    isLocallyApproved = { issueNumber ->
                        pendingRepositoryApprovals.containsKey(issueNumber) ||
                            dismissedRepositoryReviews.containsKey(issueNumber) ||
                            journal.all().any {
                                it.issueNumber == issueNumber && it.operationKey.startsWith("repository_change:")
                            }
                    }
                )
                if (ownerReview != null) {
                    main.post {
                        if (!dialog.isShowing) return@post
                        activeClaim = null
                        claimStatus.text = "OWNER REVIEW REQUIRED · #${ownerReview.task.number}"
                        status.text = "Review the exact repository, immutable base, paths and proposed content. No repository write occurs before local approval."
                        runButton.isEnabled = false
                        stopButton.isEnabled = true
                        claimButton.isEnabled = false
                        releaseClaimButton.isEnabled = false
                        busy = true
                        val registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
                        val approvalStore = VyomFactoryRepositoryApprovalStore(activity.applicationContext)
                        val modelControl = object : VyomFactoryExecutionControl {
                            override fun shouldStop(): Boolean =
                                stopRunRequested.get() || workerStopStore.isStopped() ||
                                    !hostActivity.factoryForegroundGate.isForeground() ||
                                    hostActivity.factoryForegroundGate.generation() != foregroundGeneration
                        }
                        worker.execute {
                            try {
                                val review = VyomFactoryRepositoryChangeReviewDialog.fromIssue(
                                    activity = activity,
                                    task = ownerReview.task,
                                    registry = registry,
                                    approvalStore = approvalStore,
                                    workerModel = VyomFactoryWorkerModelRuntime.adapter(activity.applicationContext),
                                    control = modelControl,
                                    onApproved = { plan, approval ->
                                        worker.execute {
                                            try {
                                                pendingRepositoryApprovals[plan.issueNumber] =
                                                    VyomFactoryRepositoryChangeAdapter.ApprovedPlan(plan, approval)
                                                main.post { runNextTask() }
                                            } catch (error: Exception) {
                                                main.post { onRunnerFailure(error.javaClass.simpleName) }
                                            }
                                        }
                                    },
                                    onRejected = { dismissedRepositoryReviews[ownerReview.task.number] = true }
                                )
                                main.post {
                                    if (!dialog.isShowing) return@post
                                    if (modelControl.shouldStop()) {
                                        busy = false
                                        runButton.isEnabled = true
                                        stopButton.isEnabled = false
                                        claimButton.isEnabled = true
                                        status.text = "Worker stopped before owner review; no plan was shown."
                                        return@post
                                    }
                                    busy = false
                                    runButton.isEnabled = true
                                    stopButton.isEnabled = false
                                    claimButton.isEnabled = true
                                    review.show()
                                }
                            } catch (error: Exception) {
                                main.post {
                                    if (!dialog.isShowing) return@post
                                    busy = false
                                    runButton.isEnabled = true
                                    stopButton.isEnabled = false
                                    claimButton.isEnabled = true
                                    status.text = "Worker plan unavailable: ${error.javaClass.simpleName}"
                                }
                            }
                        }
                    }
                    return@execute
                }

                val visitedIssues = mutableSetOf<Int>()
                val runner = VyomFactoryExecutionRunner(
                    claimNext = {
                        claimNextFromAnyLane { remote ->
                            remote.number !in visitedIssues &&
                                isEligibleForRunner(remote, journal, retrySchedules, adapter)
                        }
                    },
                    isClaimCurrent = { sessionFor(it).renewAndCheck(it) },
                    releaseClaim = { sessionFor(it).release(it) },
                    fetchFreshTask = { number ->
                        val remote = client.listTasks().firstOrNull { it.number == number }
                        val approved = pendingRepositoryApprovals[number]
                        if (remote != null && approved != null) {
                            remote.copy(body = VyomFactoryLocalApprovedPlanEnvelope.body(approved.plan))
                        } else remote
                    },
                    journal = journal,
                    adapters = VyomFactoryExecutionAdapterRegistry(listOf(
                        adapter,
                        VyomFactoryRepositoryChangeAdapter(
                            registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry")),
                            githubToken = token,
                            approvalStore = VyomFactoryRepositoryApprovalStore(activity.applicationContext),
                            approvedPlans = pendingRepositoryApprovals
                        )
                    )),
                    retrySchedules = retrySchedules,
                    newAttemptId = { UUID.randomUUID().toString() },
                    prepareExecution = { envelope ->
                        if (envelope.target == VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE) envelope
                        else prepareAndroidAction(envelope)
                    },
                    // Activity pause/resume invalidates this run permanently, even if
                    // the user returns before the worker reaches its next STOP check.
                    externalStopRequested = {
                        stopRunRequested.get() || workerStopStore.isStopped() ||
                            !hostActivity.factoryForegroundGate.isForeground() ||
                            hostActivity.factoryForegroundGate.generation() != foregroundGeneration
                    },
                    onCheckpoint = { task, checkpoint, output ->
                        stageDetector.onImplementationCheckpoint(task, checkpoint, output)
                        main.post {
                            if (::dialog.isInitialized && dialog.isShowing) {
                                refreshTaskProgress(task)
                                status.text = "Issue #${task.number} · checkpoint ${checkpoint.replace('_', ' ')} recorded."
                            }
                        }
                    },
                    onVerificationResult = { task, passed, evidence ->
                        stageDetector.onVerificationResult(task, passed, evidence)
                        main.post {
                            if (::dialog.isInitialized && dialog.isShowing) {
                                refreshTaskProgress(task)
                                status.text = if (passed) {
                                    "Issue #${task.number} · adapter postcondition verified; owner probe remains separate."
                                } else {
                                    "Issue #${task.number} · adapter postcondition failed; no completion progress awarded."
                                }
                            }
                        }
                    }
                )
                VyomFactoryDrainCoordinator.drain(
                    runNext = { runner.runNext() },
                    shouldStop = {
                        stopRunRequested.get() || workerStopStore.isStopped() ||
                            !hostActivity.factoryForegroundGate.isForeground() ||
                            hostActivity.factoryForegroundGate.generation() != foregroundGeneration
                    },
                    onOutcome = { next ->
                        if (next is VyomFactoryExecutionRunner.Outcome.Succeeded &&
                            next.result.startsWith("repository_proposal_verified:")
                        ) {
                            // Only a durably succeeded runner outcome closes the local proposal lifecycle.
                            runCatching { workerProposalStore.markSubmitted(next.issueNumber) }
                        }
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
                refresh()
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
        if (pendingRepositoryApprovals.containsKey(task.number)) return true
        val envelope = VyomFactoryExecutionEnvelope.parse(task.body)
        if (!envelope.isExecutable) return false
        if (envelope.target == VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE) {
            return pendingRepositoryApprovals.containsKey(task.number)
        }
        if (envelope.target != VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION ||
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
        status.text = "Checking owner-approved factory-evidence binding and repository privacy…"
        worker.execute {
            try {
                val fresh = client.listTasks().firstOrNull { it.number == task.number && it.state == "open" && it.metadata.status != "paused" }
                    ?: throw IllegalStateException("Selected issue is no longer open")
                val registry = VyomFactorySystemRegistry(
                    File(activity.filesDir, "factory-system-registry")
                )
                val binding = registry.get(VyomFactoryEvidencePublisher.SYSTEM_ID)
                    ?: throw SecurityException(
                        "No owner-approved factory-evidence binding. Inspect/register this repository with system ID " +
                            "'factory-evidence' and allowed path root '.factory-evidence' first."
                    )
                require(screenshot.name.matches(Regex("^[a-f0-9-]{36}\\.(png|jpg|webp)$"))) {
                    "Stored screenshot filename is not a trusted generated name"
                }
                val relativePath = ".factory-evidence/issue-${fresh.number}/${screenshot.name}"
                val store = VyomFactoryScreenshotEvidenceStore(
                    File(activity.filesDir, "factory-evidence")
                )
                val evidenceRepository = VyomFactoryRepositoryIntake.parseUrl(binding.repositoryUrl)
                    ?: throw SecurityException("Factory evidence repository binding is invalid")
                val publisher = VyomFactoryEvidencePublisher(
                    registry,
                    VyomFactoryGitHubEvidenceTransport(
                        token = token,
                        repository = "${evidenceRepository.owner}/${evidenceRepository.name}"
                    ),
                    store
                )
                val attachment = publisher.publish(
                    VyomFactoryEvidencePublisher.SYSTEM_ID,
                    binding.repositoryUrl,
                    binding.commitSha,
                    fresh.number,
                    relativePath,
                    screenshot
                )
                main.post {
                    if (!dialog.isShowing) return@post
                    if (attachment.issueCommentRecorded) {
                        screenshot.delete()
                        screenshotAttachButton?.text = "ATTACHED TO #${fresh.number}"
                        status.text = "Screenshot attached to #${fresh.number}. SHA-256 ${attachment.sha256}. " +
                            "Commit ${attachment.commitSha.take(12)} verified; local copy removed."
                    } else {
                        screenshotAttachButton?.isEnabled = false
                        status.text = "Evidence commit ${attachment.commitSha.take(12)} and SHA-256 ${attachment.sha256} " +
                            "were verified, but the issue comment was not confirmed. Receipt is saved; do not upload again. Check the issue before manually retrying. " +
                            "Manually add this link to #${fresh.number}: ${attachment.url}"
                    }
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    screenshotAttachButton?.isEnabled = true
                    status.text = "Screenshot not uploaded · ${error.message?.take(180) ?: error.javaClass.simpleName}. " +
                        "Local copy is preserved; no automatic retry."
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
            .setMessage("Search GitHub repositories. Vyom scans the repository tree for root or nested SKILL.md files; only approved SPDX licenses can be imported.")
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
                if (workerStopStore.isStopped()) throw IllegalStateException("GLOBAL STOP is active")
                val claim = claimNextFromAnyLane()
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
                val released = sessionFor(claim).release(claim)
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
                val proposals = (workerProposalStore.pending() + workerProposalStore.approved())
                    .associateBy { it.issueNumber }
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "GitHub Issues · ${tasks.size} Factory tasks loaded"
                    render(tasks, proposals)
                    refreshRetryOverview()
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    taskList.removeAllViews()
        progressViews.clear()
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
            historyStatus.text = (listOf(history.label) + history.latest.map { it.label }).joinToString("\n")
            historyStatus.contentDescription = "${history.total} persisted attempts; ${history.active} active, ${history.succeeded} verified, ${history.failed} failed, ${history.interrupted} interrupted. Latest attempt rows are redacted."
            val laneSummaries = VyomFactoryLaneMetrics.summarize(
                VyomFactoryExecutionJournal(activity.applicationContext).all()
            )
            laneMetricsStatus.text = buildString {
                append("LANE WORKERS · RUNS · FAILURES")
                VyomFactoryLaneRouter.Lane.values().forEach { lane ->
                    val laneSummary = laneSummaries.getValue(lane)
                    append("\n").append(lane.name)
                        .append(" · ").append(laneSummary.activeWorkers)
                        .append(" · ").append(laneSummary.runs)
                        .append(" · ").append(laneSummary.failures)
                }
            }
            laneMetricsStatus.contentDescription = VyomFactoryLaneRouter.Lane.values().joinToString("; ") { lane ->
                val value = laneSummaries.getValue(lane)
                "${lane.name}: ${value.activeWorkers} active workers, ${value.runs} runs, ${value.failures} failures"
            }
        } catch (error: Exception) {
            retryStatus.text = "RETRY STATUS UNAVAILABLE · REVIEW REQUIRED"
            retryStatus.contentDescription = "Retry schedule could not be read safely."
            historyStatus.text = "RUN HISTORY UNAVAILABLE · REVIEW REQUIRED"
            historyStatus.contentDescription = "Execution journal could not be read safely."
            laneMetricsStatus.text = "LANE METRICS UNAVAILABLE · REVIEW REQUIRED"
            laneMetricsStatus.contentDescription = "Lane execution metrics could not be read safely."
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
        val laneDecision = VyomFactoryLaneRouter.route(
            VyomFactoryLaneRouter.Request(
                origin = VyomFactoryLaneRouter.Origin.COMPLAINT,
                text = "$title\\n$description"
            )
        )
        worker.execute {
            try {
                val task = client.createTask(
                    title, description, selectedPriority, "factory", sourceId,
                    lane = laneDecision.lane,
                    lanePlanningAllowed = false
                )
                VyomFactoryTaskStore(activity).enqueue(
                    VyomFactoryTaskStore.QueuedTask(
                        task.number, task.title, task.metadata.priority, sourceId,
                        System.currentTimeMillis(), task.metadata.lane, task.metadata.lanePlanningAllowed
                    )
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

    private fun progressText(progress: VyomFactoryVerifiedProgress.Snapshot): String =
        "PROGRESS ${progress.percent}% · ${progress.label}\n" +
            "Stage: ${progress.stage.name} · Commit: ${progress.commitSha?.take(8) ?: "PENDING"}\n" +
            "CI ${if (progress.ciVerified) "PASS" else "PENDING"} · " +
            "Criteria ${progress.checkedCriteria}/${progress.totalCriteria} " +
            "${if (progress.criteriaVerified) "VERIFIED" else "PENDING"} · " +
            "Device ${if (!progress.probeRequired) "N/A" else if (progress.probeVerified) "PASS" else "PHONE-PENDING"}"

    private fun refreshTaskProgress(task: VyomFactoryGitHubClient.Task) {
        val views = progressViews[task.number] ?: return
        val snapshot = runCatching {
            VyomFactoryVerifiedProgress.calculate(
                task,
                VyomFactoryExecutionJournal(activity.applicationContext).all()
            )
        }.getOrNull() ?: return
        views.first.text = progressText(snapshot)
        views.first.contentDescription = progressText(snapshot)
        views.second.progress = snapshot.percent
    }

    private fun render(
        tasks: List<VyomFactoryGitHubClient.Task>,
        workerProposals: Map<Int, VyomFactoryWorkerProposalStore.StoredProposal> = emptyMap()
    ) {
        val orderedTasks = VyomFactoryTaskOrdering.sort(tasks)
        taskList.removeAllViews()
        if (orderedTasks.isEmpty()) {
            taskList.addView(message("No Factory complaints registered yet. Use the form above to create the first shared task."))
            return
        }
        val executionEntries = runCatching {
            VyomFactoryExecutionJournal(activity.applicationContext).all()
        }.getOrNull()
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
                text = "${task.metadata.lane.name} · ${task.metadata.priority} · ${task.metadata.status.uppercase()} · UPDATED ${task.updatedAt}"
                textSize = 9f; typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(4), 0, dp(4))
            })
            val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
            if (description.isNotBlank()) card.addView(TextView(activity).apply {
                text = description; textSize = 11f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                maxLines = 12; setPadding(0, 0, 0, dp(6))
            })

            val verifiedProgress = executionEntries?.let {
                VyomFactoryVerifiedProgress.calculate(task, it)
            }
            val workerProposal = workerProposals[task.number]
                ?.takeIf {
                    it.status in setOf("awaiting_review", "approved") &&
                        it.sourceId == task.metadata.sourceId &&
                        it.updatedAt == task.updatedAt
                }
            val progressLabel = TextView(activity).apply {
                text = verifiedProgress?.let(::progressText)
                    ?: "PROGRESS UNAVAILABLE · EXECUTION JOURNAL UNREADABLE"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                setPadding(0, dp(3), 0, dp(3))
                contentDescription = text.toString()
            }
            card.addView(progressLabel)
            verifiedProgress?.let { progress ->
                val bar = android.widget.ProgressBar(
                    activity, null, android.R.attr.progressBarStyleHorizontal
                ).apply {
                    max = 100
                    this.progress = progress.percent
                    isIndeterminate = false
                    contentDescription = "Verified task progress ${progress.percent} percent"
                }
                progressViews[task.number] = progressLabel to bar
                card.addView(bar, LinearLayout.LayoutParams(-1, dp(6)).apply {
                    bottomMargin = dp(7)
                })
            }
            val controls = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            fun action(label: String, next: String) {
                controls.addView(Button(activity).apply {
                    text = label; textSize = 8f; isAllCaps = true
                    setOnClickListener {
                        update(
                            task,
                            next,
                            task.metadata.priority,
                            ownerApprovedStart = next == "in_progress"
                        )
                    }
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
            if (workerProposal != null) {
                card.addView(Button(activity).apply {
                    text = if (workerProposal.status == "approved") {
                        "REAUTHORIZE APPROVED WORKER PROPOSAL"
                    } else {
                        "REVIEW WORKER PROPOSAL"
                    }
                    textSize = 9f
                    setOnClickListener { showWorkerProposalPreview(task, workerProposal) }
                }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(4) })
            }
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

    private fun showWorkerProposalPreview(
        task: VyomFactoryGitHubClient.Task,
        stored: VyomFactoryWorkerProposalStore.StoredProposal
    ) {
        if (busy) return
        busy = true
        status.text = "Revalidating worker proposal against the canonical issue and immutable binding…"
        worker.execute {
            try {
                val fresh = client.listTasks().firstOrNull {
                    it.number == task.number && it.state == "open" &&
                        it.metadata.status in setOf("open", "in_progress") &&
                        it.metadata.sourceId == stored.sourceId && it.updatedAt == stored.updatedAt
                }
                val registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
                val binding = registry.get(stored.candidate.binding.systemId)
                val validatedBinding = runCatching {
                    VyomFactoryRepositoryChangePolicy.validateCandidate(stored.candidate.plan, registry)
                }.getOrNull()
                val sourceVerificationFailure = runCatching {
                    VyomFactoryProposalSourceVerifier.verify(
                        stored.candidate, registry, VyomFactoryRepositoryClient()::readBoundSystemFile
                    )
                }.exceptionOrNull()
                main.post {
                    busy = false
                    if (!dialog.isShowing) return@post
                    if (fresh == null) {
                        worker.execute { workerProposalStore.remove(task.number) }
                        status.text = "Worker proposal is stale because the canonical issue changed; it was removed from review."
                        refresh()
                        return@post
                    }
                    if (binding != stored.candidate.binding || validatedBinding != binding) {
                        status.text = "Worker proposal binding/path validation failed; review is blocked."
                        return@post
                    }
                    if (sourceVerificationFailure != null) {
                        status.text = "Worker proposal source SHA-256 verification failed; review is blocked · " +
                            (sourceVerificationFailure.message?.take(140) ?: sourceVerificationFailure.javaClass.simpleName)
                        return@post
                    }
                    val plan = stored.candidate.plan
                    val preview = TextView(activity).apply {
                        text = "WORKER PROPOSAL · OWNER REVIEW REQUIRED\n" +
                            "Issue: #${fresh.number}\nWorker: ${stored.workerId}\n" +
                            "Repository: ${binding.repositoryUrl}\nBase: ${plan.baseBranch} · ${plan.baseCommitSha}\n" +
                            "Paths: ${plan.changes.joinToString { it.path }}\n" +
                            "VERIFIED BASE SHA-256:\n" +
                            stored.candidate.sourceFileSha256.toSortedMap().entries.joinToString("\n") { "${it.key} · ${it.value}" } +
                            "\n\n" +
                            plan.changes.joinToString("\n\n") { "PATH: ${it.path}\n\n${it.content}" } +
                            "\n\nNo repository write occurs until you approve. Approval uses the existing one-time plan-bound token, immutable base, draft PR and exact-SHA CI gate."
                        textSize = 11f
                        setTextColor(VyomDesignSpec.TEXT_PRIMARY)
                        setTextIsSelectable(true)
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    }
                    val scroll = ScrollView(activity).apply { addView(preview) }
                    val positiveLabel = if (stored.status == "approved") "REAUTHORIZE & QUEUE" else "APPROVE & QUEUE"
                    val negativeLabel = if (stored.status == "awaiting_review") "REJECT PROPOSAL" else "CLOSE"
                    val reviewDialog = AlertDialog.Builder(activity)
                        .setTitle("Worker proposal · #${fresh.number}")
                        .setView(scroll)
                        .setNegativeButton(negativeLabel) { _, _ ->
                            if (stored.status == "awaiting_review") {
                                worker.execute {
                                    workerProposalStore.markRejected(fresh.number)
                                    main.post {
                                        status.text = "Proposal rejected locally. No repository change was approved or written."
                                        refresh()
                                    }
                                }
                            }
                        }
                        .setPositiveButton(positiveLabel, null)
                        .create()
                    reviewDialog.setOnShowListener {
                        val approveButton = reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        approveButton.setOnClickListener {
                            if (busy) return@setOnClickListener
                            busy = true
                            approveButton.isEnabled = false
                            status.text = "Rechecking issue and immutable repository binding before owner approval…"
                            worker.execute {
                                try {
                                    val current = client.listTasks().firstOrNull {
                                        it.number == fresh.number && it.state == "open" &&
                                            it.metadata.status in setOf("open", "in_progress") &&
                                            it.metadata.sourceId == stored.sourceId &&
                                            it.updatedAt == stored.updatedAt
                                    } ?: throw SecurityException("Factory issue changed after preview; approval refused")
                                    val currentBinding = registry.get(plan.systemId)
                                        ?: throw SecurityException("Registered repository binding is missing")
                                    val validated = VyomFactoryRepositoryChangePolicy.validateCandidate(plan, registry)
                                    VyomFactoryProposalSourceVerifier.verify(
                                        stored.candidate, registry, VyomFactoryRepositoryClient()::readBoundSystemFile
                                    )
                                    if (current.number != plan.issueNumber || currentBinding != binding || validated != binding) {
                                        throw SecurityException("Worker proposal no longer matches the registered repository or exact base")
                                    }
                                    if (pendingRepositoryApprovals.containsKey(fresh.number)) {
                                        throw IllegalStateException("This proposal already has an active owner approval in this session")
                                    }
                                    val approval = VyomFactoryRepositoryApprovalStore(activity.applicationContext)
                                        .issueFromOwnerReview(plan, binding)
                                    check(workerProposalStore.markApproved(fresh.number)) {
                                        "Proposal is missing, rejected, or no longer eligible for owner approval"
                                    }
                                    val previous = pendingRepositoryApprovals.putIfAbsent(
                                        fresh.number,
                                        VyomFactoryRepositoryChangeAdapter.ApprovedPlan(plan, approval)
                                    )
                                    check(previous == null) { "A proposal approval is already queued in this session" }
                                    main.post {
                                        busy = false
                                        reviewDialog.dismiss()
                                        status.text = "Owner approval persisted. Starting the existing token-bound draft-PR and CI path…"
                                        runNextTask()
                                    }
                                } catch (error: Exception) {
                                    main.post {
                                        busy = false
                                        approveButton.isEnabled = true
                                        status.text = "Approval failed closed: ${error.message?.take(180) ?: error.javaClass.simpleName}"
                                    }
                                }
                            }
                        }
                    }
                    reviewDialog.show()
                }
            } catch (_: Exception) {
                main.post {
                    busy = false
                    if (::dialog.isInitialized && dialog.isShowing) {
                        status.text = "Worker proposal could not be revalidated; review failed closed."
                    }
                }
            }
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

    private fun createStageDetector(journal: VyomFactoryStageDetectorJournal) = VyomFactoryStageDetector(
        loadTasks = { client.listTasks() },
        createTask = { parentIssueNumber, finding, existing ->
            val proposal = requireNotNull(
                VyomFactoryTaskIntelligence.toSeparateTaskProposal(parentIssueNumber, finding, existing)
            )
            client.createTask(
                title = proposal.title,
                description = proposal.description + "\n\nMax iterations: ${VyomFactoryTaskIntelligence.DEFAULT_MAX_ITERATIONS}",
                priority = "P2",
                source = "task_failure",
                sourceId = proposal.sourceId
            )
        },
        recordEvent = journal::record
    )

    private fun recordVerification(task: VyomFactoryGitHubClient.Task, passed: Boolean, evidence: String) {
        if (busy) return
        busy = true
        status.text = "Recording device probe result for #${task.number}…"
        worker.execute {
            try {
                val fresh = client.listTasks().firstOrNull { it.number == task.number }
                    ?: throw IllegalArgumentException("Complaint #${task.number} is no longer in the shared registry")
                val verified = client.recordVerificationResult(fresh, passed, evidence)
                createStageDetector(VyomFactoryStageDetectorJournal(activity.applicationContext))
                    .onVerificationResult(fresh, passed, evidence)
                main.post {
                    if (!dialog.isShowing) return@post
                    refreshTaskProgress(verified)
                    status.text = if (passed) "Probe PASS recorded for #${task.number}; progress now reflects the persisted evidence."
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
                    else comments.joinToString("\n\n")
                val details = "Status: ${task.metadata.status} · Priority: ${task.metadata.priority}\n\n" +
                    description + "\n\nEVENT HISTORY\n" + history
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

    private fun update(
        task: VyomFactoryGitHubClient.Task,
        nextStatus: String,
        priority: String,
        ownerApprovedStart: Boolean = false
    ) {
        if (busy) return
        busy = true
        status.text = "Saving task #${task.number}…"
        worker.execute {
            try {
                val planningAllowed = VyomFactoryPlanningApprovalPolicy.requestedLanePlanningAllowed(
                    currentStatus = task.metadata.status,
                    nextStatus = nextStatus,
                    ownerApprovedStart = ownerApprovedStart
                )
                val persisted = client.updateTask(
                    task,
                    newPriority = priority,
                    newStatus = nextStatus,
                    newLanePlanningAllowed = planningAllowed
                )
                check(persisted.metadata.status == nextStatus &&
                    persisted.metadata.lanePlanningAllowed == (planningAllowed ?: task.metadata.lanePlanningAllowed)) {
                    "GitHub did not confirm the requested task state; refresh before retrying"
                }
                if (ownerApprovedStart) {
                    VyomFactoryWorkerDispatchScheduler.enqueue(activity.applicationContext)
                }
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "GitHub confirmed #${task.number} · ${persisted.metadata.status.uppercase()}"
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
            403 -> "GitHub 403 · this token cannot perform the requested Issues operation. Grant this repository's token Issues: Read and write, then save the token in Settings. No task state was assumed changed."
            404 -> "Repository or issue endpoint not found (404). Check repository access."
            422 -> "GitHub rejected the task data (422). Check title, labels and issue permissions."
            else -> error.message?.take(180) ?: "Factory request failed."
        }
    }


}
