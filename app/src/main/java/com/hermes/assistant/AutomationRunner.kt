package com.hermes.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import java.io.File
import java.util.concurrent.Executor

internal class AutomationRunner(
    private val activity: Activity,
    private val apiClient: VyomApiClient?,
    private val ioExecutor: Executor
) {
    data class StepResult(val index: Int, val success: Boolean, val message: String, val artifactPath: String? = null)
    data class RunSummary(val success: Boolean, val completedSteps: Int, val totalSteps: Int, val message: String)

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile private var stopped = false
    private var completion: ((RunSummary) -> Unit)? = null
    private var completed = 0
    private var totalSteps = 0

    fun run(
        macro: AutomationEngine.Macro,
        ownerApproved: Boolean,
        onProgress: (StepResult) -> Unit,
        onComplete: (RunSummary) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { run(macro, ownerApproved, onProgress, onComplete) }
            return
        }
        if (running) {
            onComplete(RunSummary(false, 0, macro.steps.size, "Another automation is already running."))
            return
        }
        val validation = AutomationEngine.validate(macro)
        if (!validation.valid) {
            onComplete(RunSummary(false, 0, macro.steps.size, "Invalid automation: " + validation.errors.joinToString(", ")))
            return
        }
        if (!macro.enabled) {
            onComplete(RunSummary(false, 0, macro.steps.size, "This automation is disabled."))
            return
        }
        when (AutomationEngine.decision(macro, ownerApproved)) {
            AutomationEngine.Decision.INVALID -> {
                onComplete(RunSummary(false, 0, macro.steps.size, "Automation validation failed."))
                return
            }
            AutomationEngine.Decision.BLOCKED -> {
                onComplete(RunSummary(false, 0, macro.steps.size, "Blocked by Vyom safety policy."))
                return
            }
            AutomationEngine.Decision.APPROVAL_REQUIRED -> {
                onComplete(RunSummary(false, 0, macro.steps.size, "Owner approval is required on screen."))
                return
            }
            AutomationEngine.Decision.ALLOW -> Unit
        }
        running = true
        stopped = false
        completed = 0
        totalSteps = macro.steps.size
        completion = onComplete
        runStep(macro, 0, onProgress)
    }

    fun cancel() {
        if (!running) return
        stopped = true
        handler.removeCallbacksAndMessages(null)
        finish(false, "Automation stopped by owner.")
    }

    fun isRunning(): Boolean = running

    private fun runStep(
        macro: AutomationEngine.Macro,
        index: Int,
        onProgress: (StepResult) -> Unit
    ) {
        if (stopped) return finish(false, "Automation stopped by owner.")
        if (index >= macro.steps.size) return finish(true, "Automation completed.")
        val step = macro.steps[index]
        executeStep(step) { success, message, artifact ->
            if (stopped) return@executeStep finish(false, "Automation stopped by owner.")
            val result = StepResult(index + 1, success, message, artifact)
            onProgress(result)
            if (!success) return@executeStep finish(false, "Stopped at step ${index + 1}: ${message}")
            completed += 1
            runStep(macro, index + 1, onProgress)
        }
    }

    private fun executeStep(
        step: AutomationEngine.Step,
        callback: (Boolean, String, String?) -> Unit
    ) {
        when (step.type) {
            "open_app" -> {
                val intent = ActionEngine.classify("open " + step.target)
                if (intent == null || intent.operation != ActionEngine.Operation.OPEN_APP) {
                    callback(false, "App target was not recognized.", null)
                } else {
                    val message = DeviceActionExecutor.execute(activity, intent)
                    val success = !message.startsWith("No matching") && !message.startsWith("App not installed") &&
                        !message.startsWith("Could not") && !message.startsWith("App is installed but")
                    callback(success, message, null)
                }
            }
            "open_url" -> try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(step.target)))
                callback(true, "Opened URL.", null)
            } catch (_: Exception) {
                callback(false, "Could not open URL.", null)
            }
            "tap_text" -> {
                val service = VyomAccessibilityService.current()
                callback(service?.clickVisibleText(step.target) == true, "Tap text: " + step.target, null)
            }
            "type_text" -> {
                val service = VyomAccessibilityService.current()
                callback(service?.typeIntoFocusedField(step.text) == true, "Entered text into the focused field.", null)
            }
            "scroll" -> {
                val service = VyomAccessibilityService.current()
                callback(service?.scroll(step.target) == true, "Scrolled " + step.target + ".", null)
            }
            "swipe" -> {
                val service = VyomAccessibilityService.current()
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(metrics)
                val success = service?.swipe(
                    step.x1 * metrics.widthPixels, step.y1 * metrics.heightPixels,
                    step.x2 * metrics.widthPixels, step.y2 * metrics.heightPixels,
                    step.durationMs.toLong()
                ) == true
                callback(success, "Swipe gesture executed.", null)
            }
            "wait" -> handler.postDelayed({ callback(true, "Wait completed.", null) }, step.durationMs.toLong())
            "back", "home" -> {
                val service = VyomAccessibilityService.current()
                callback(service?.global(step.type) == true, "Android " + step.type + " action.", null)
            }
            "screenshot" -> {
                val service = VyomAccessibilityService.current()
                if (service == null) callback(false, "Enable Vyom Accessibility Service first.", null)
                else service.captureLocalScreenshot { file: File?, error: String? ->
                    callback(file != null, error ?: "Screenshot saved locally.", file?.absolutePath)
                }
            }
            "open_settings" -> {
                val phrase = when (step.target.lowercase()) {
                    "wifi" -> "open wifi settings"
                    "bluetooth" -> "open bluetooth settings"
                    "display" -> "open display settings"
                    "sound" -> "open sound settings"
                    "apps" -> "open app settings"
                    "notifications" -> "open settings"
                    "location" -> null
                    "accessibility" -> null
                    else -> "open settings"
                }
                val intent = when (step.target.lowercase()) {
                    "location" -> ActionEngine.Intent("Open location settings", ActionEngine.Risk.SAFE, ActionEngine.Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    "accessibility" -> ActionEngine.Intent("Open accessibility settings", ActionEngine.Risk.SAFE, ActionEngine.Operation.OPEN_SETTINGS, settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    else -> phrase?.let(ActionEngine::classify)
                }
                if (intent == null || intent.operation != ActionEngine.Operation.OPEN_SETTINGS) {
                    callback(false, "Settings panel is not supported.", null)
                } else {
                    callback(true, DeviceActionExecutor.execute(activity, intent), null)
                }
            }
            "create_task" -> {
                if (apiClient == null) callback(false, "Connected task service is unavailable.", null)
                else ioExecutor.execute {
                    try {
                        val task = apiClient.createTask(step.text)
                        val id = task.optString("id")
                        handler.post { callback(id.isNotBlank(), if (id.isBlank()) "Task was not queued." else "Task queued: " + id, null) }
                    } catch (_: Exception) {
                        handler.post { callback(false, "Could not create connected task.", null) }
                    }
                }
            }
            else -> callback(false, "Unsupported automation step.", null)
        }
    }

    private fun finish(success: Boolean, message: String) {
        if (!running) return
        running = false
        stopped = false
        val callback = completion
        completion = null
        callback?.invoke(RunSummary(success, completed, totalSteps, message))
    }
}
