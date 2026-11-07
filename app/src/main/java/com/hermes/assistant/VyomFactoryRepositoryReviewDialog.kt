package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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
                        val client = VyomFactoryRepositoryClient()
                        val preview = client.inspect(value)
                        val files = runCatching { client.previewFiles(preview) }.getOrDefault(emptyList())
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            dialog.dismiss()
                            showPreview(preview, files)
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

    private fun showPreview(preview: VyomFactoryRepositoryClient.Preview, files: List<VyomFactoryRepositoryClient.FilePreview>) {
        val assessment = preview.assessment
        val detail = TextView(activity).apply {
            text = buildString {
                appendLine("SOURCE  ${preview.snapshot.source.canonicalUrl}")
                appendLine("COMMIT  ${preview.snapshot.commitSha}")
                appendLine("BRANCH  ${preview.snapshot.defaultBranch}")
                appendLine("PRIMARY ${assessment.kind}")
                appendLine("ARTIFACTS ${assessment.detectedKinds.joinToString(", ")}")
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
                if (files.isNotEmpty()) {
                    appendLine()
                    appendLine("BOUNDED FILE PREVIEW · UNTRUSTED CONTENT")
                    files.forEach { file ->
                        appendLine()
                        appendLine("--- ${file.path} · ${file.sizeBytes} bytes · ${file.redactions} redactions · SHA-256 ${file.contentSha256} ---")
                        appendLine(file.content)
                    }
                } else {
                    appendLine()
                    appendLine("No eligible documentation/manifest files were available for preview.")
                }
                appendLine()
                appendLine("Preview text is untrusted and redacted. No scripts, workflows, prompts or binaries were executed.")
                append("Registration saves only the immutable repository reference. It does not install code or grant a capability.")
            }
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            setPadding(24, 20, 24, 20)
        }
        val systemIdInput = EditText(activity).apply {
            hint = "System ID (e.g. memory, planner, worker)"
            setSingleLine(true)
            textSize = 14f
        }
        val allowedPathsInput = EditText(activity).apply {
            hint = "Allowed path roots (comma or newline separated), e.g. app/src/main, docs"
            minLines = 2
            maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            textSize = 14f
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(detail, ViewGroup.LayoutParams(-1, -2))
            addView(systemIdInput, ViewGroup.LayoutParams(-1, -2))
            addView(allowedPathsInput, ViewGroup.LayoutParams(-1, -2))
        }
        val scroll = ScrollView(activity).apply {
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
        val canRegister = assessment.decision != VyomFactoryRepositoryIntake.Decision.BLOCKED
        val positiveLabel = when (assessment.decision) {
            VyomFactoryRepositoryIntake.Decision.READY_FOR_REVIEW -> "BIND SYSTEM TO REPOSITORY"
            VyomFactoryRepositoryIntake.Decision.NEEDS_OWNER_REVIEW -> "BIND AFTER REVIEW"
            VyomFactoryRepositoryIntake.Decision.BLOCKED -> "BLOCKED"
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("SYSTEM REPOSITORY + PATH REVIEW")
            .setView(scroll)
            .setPositiveButton(positiveLabel, null)
            .setNegativeButton("CANCEL") { _, _ -> finish() }
            .create()
        dialog.setOnCancelListener { finish() }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = canRegister
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!canRegister) return@setOnClickListener
            val systemId = systemIdInput.text.toString().trim()
            val allowedPaths = allowedPathsInput.text.toString()
                .split(',', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (!systemId.matches(Regex("^[a-z][a-z0-9_.-]{0,63}$"))) {
                systemIdInput.error = "Use lowercase ID: a-z, 0-9, dot, underscore or hyphen"
                return@setOnClickListener
            }
            if (allowedPaths.isEmpty()) {
                allowedPathsInput.error = "At least one allowed path root is required"
                return@setOnClickListener
            }
            val registry = VyomFactorySystemRegistry(
                File(activity.filesDir, "factory-system-registry")
            )
            val proposedBinding = VyomFactorySystemRegistry.Binding(
                systemId = systemId,
                repositoryUrl = preview.snapshot.source.canonicalUrl,
                commitSha = preview.snapshot.commitSha,
                allowedPaths = allowedPaths,
                registeredAtEpochMs = System.currentTimeMillis()
            )
            val existing = try {
                registry.get(systemId)
            } catch (error: Exception) {
                onStatus("Could not read current system binding · ${error.javaClass.simpleName}")
                return@setOnClickListener
            }
            val sameBindingTarget = existing != null &&
                existing.repositoryUrl == proposedBinding.repositoryUrl &&
                existing.commitSha == proposedBinding.commitSha &&
                existing.allowedPaths == proposedBinding.allowedPaths
            val bindingToPersist = if (sameBindingTarget) existing!! else proposedBinding
            val migrateSystem = existing != null && !sameBindingTarget
            val repositoryRegistry = VyomFactoryRepositoryRegistry(
                File(activity.filesDir, "factory-repository-registry")
            )
            val persist: (Boolean) -> Unit = { migrate ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                worker.execute {
                    try {
                        val entry = repositoryRegistry.register(preview, ownerConfirmed = true)
                        require(entry.canonicalUrl == proposedBinding.repositoryUrl &&
                            entry.commitSha == proposedBinding.commitSha) {
                            "Inspected repository snapshot changed before registration"
                        }
                        val binding = if (migrate && existing != null) {
                            registry.migrate(
                                proposedBinding,
                                expectedExisting = existing,
                                ownerConfirmed = true
                            )
                        } else {
                            registry.register(bindingToPersist, ownerConfirmed = true)
                        }
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            onStatus(
                                "System '${systemId}' ${if (migrate) "migrated" else "bound"} to " +
                                    "${binding.repositoryUrl} · ${binding.commitSha.take(12)} · " +
                                    "${binding.allowedPaths.size} allowed path root(s). Repository execution remains gated."
                            )
                            finish()
                        }
                    } catch (error: Exception) {
                        main.post {
                            if (finished || activity.isFinishing) return@post
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            onStatus("System binding failed · ${error.message?.take(180) ?: error.javaClass.simpleName}")
                        }
                    }
                }
            }
            if (migrateSystem && existing != null) {
                AlertDialog.Builder(activity)
                    .setTitle("MIGRATE SYSTEM BINDING?")
                    .setMessage(
                        "This system is already bound. Replacing it changes the repository, immutable commit, " +
                            "and/or allowed path roots.\n\nCURRENT\n${existing.repositoryUrl}\n${existing.commitSha}\n" +
                            "${existing.allowedPaths.joinToString()}\n\nNEW\n${proposedBinding.repositoryUrl}\n" +
                            "${proposedBinding.commitSha}\n${proposedBinding.allowedPaths.joinToString()}\n\n" +
                            "Only confirm if you are the owner and intend to migrate this system. Existing workers remain gated."
                    )
                    .setPositiveButton("CONFIRM MIGRATION") { _, _ -> persist(true) }
                    .setNegativeButton("CANCEL", null)
                    .show()
            } else {
                persist(false)
            }
        }
    }

    private fun finish() {
        if (finished) return
        finished = true
        worker.shutdownNow()
    }
}
