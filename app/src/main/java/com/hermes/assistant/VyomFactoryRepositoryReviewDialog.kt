package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executors

/** Owner-reviewed, read-only registration of any public GitHub repository type. */
internal class VyomFactoryRepositoryReviewDialog(
    private val activity: Activity,
    private val onStatus: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var finished = false

    fun show() {
        val url = EditText(activity).apply {
            hint = "https://github.com/owner/repository"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("INSPECT ANY GITHUB REPOSITORY")
            .setMessage(
                "Accepts source code, API specs, workflows, documentation and skills. " +
                    "Vyom reads metadata and a bounded file tree at an immutable commit. " +
                    "It does not download or execute repository code. Registration stores only a pinned source reference."
            )
            .setView(url)
            .setPositiveButton("INSPECT", null)
            .setNegativeButton("CANCEL") { _, _ -> finish() }
            .create()
        dialog.setOnCancelListener { finish() }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = url.text.toString().trim()
                if (VyomFactoryRepositoryIntake.parseUrl(value) == null) {
                    url.error = "Enter an exact https://github.com/owner/repository URL"
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                onStatus("Inspecting repository metadata and immutable source tree…")
                worker.execute {
                    try {
                        val preview = VyomFactoryRepositoryClient().inspect(value)
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            dialog.dismiss()
                            showPreview(preview)
                        }
                    } catch (error: Exception) {
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            url.error = error.message?.take(180) ?: "Repository inspection failed"
                            onStatus("Repository inspection failed · ${error.javaClass.simpleName}")
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showPreview(preview: VyomFactoryRepositoryClient.Preview) {
        val assessment = preview.assessment
        val detail = TextView(activity).apply {
            text = buildString {
                appendLine("SOURCE  ${preview.snapshot.source.canonicalUrl}")
                appendLine("COMMIT  ${preview.snapshot.commitSha}")
                appendLine("BRANCH  ${preview.snapshot.defaultBranch}")
                appendLine("TYPE    ${assessment.kind}")
                appendLine("LICENSE ${preview.snapshot.licenseSpdx.ifBlank { "UNKNOWN" }}")
                appendLine("STARS   ${preview.stars}")
                appendLine("SIZE    ${preview.sizeKb} KiB")
                appendLine("UPDATED ${preview.updatedAt}")
                appendLine("GATE    ${assessment.decision}")
                if (assessment.reasons.isNotEmpty()) {
                    appendLine()
                    appendLine("REVIEW FLAGS")
                    assessment.reasons.forEach { appendLine("• $it") }
                }
                appendLine()
                appendLine("No source files were downloaded. No scripts, workflows, prompts or binaries were executed.")
                append("Registration saves only the immutable repository reference. It does not install code or grant a capability.")
            }
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setPadding(24, 20, 24, 20)
        }
        val scroll = ScrollView(activity).apply {
            addView(detail, ViewGroup.LayoutParams(-1, -2))
        }
        val canRegister = assessment.decision != VyomFactoryRepositoryIntake.Decision.BLOCKED
        val positiveLabel = when (assessment.decision) {
            VyomFactoryRepositoryIntake.Decision.READY_FOR_REVIEW -> "REGISTER REVIEWED SOURCE"
            VyomFactoryRepositoryIntake.Decision.NEEDS_OWNER_REVIEW -> "REGISTER PENDING REVIEW"
            VyomFactoryRepositoryIntake.Decision.BLOCKED -> "BLOCKED"
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("REPOSITORY INTAKE REVIEW")
            .setView(scroll)
            .setPositiveButton(positiveLabel) { _, _ ->
                if (!canRegister) return@setPositiveButton
                worker.execute {
                    try {
                        val entry = VyomFactoryRepositoryRegistry(
                            File(activity.filesDir, "factory-repository-registry")
                        ).register(preview, ownerConfirmed = true)
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            onStatus(
                                "Repository reference registered · ${entry.kind} · ${entry.commitSha.take(12)}. " +
                                    "Execution remains disabled."
                            )
                            finish()
                        }
                    } catch (error: Exception) {
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            onStatus("Repository registration failed · ${error.message?.take(180) ?: error.javaClass.simpleName}")
                            finish()
                        }
                    }
                }
            }
            .setNegativeButton("CANCEL") { _, _ -> finish() }
            .create()
        if (!canRegister) dialog.setButton(AlertDialog.BUTTON_POSITIVE, "BLOCKED", null, null)
        dialog.setOnCancelListener { finish() }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = canRegister
    }

    private fun finish() {
        if (finished) return
        finished = true
        worker.shutdownNow()
    }
}
