package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

/** Owner-confirmed, local-only binding of the real worker to one immutable test repository. */
internal class VyomFactoryWorkerBindingDialog(
    private val activity: Activity,
    private val onStatus: (String) -> Unit
) {
    fun show() {
        val registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
        val existing = try {
            registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
        } catch (error: Exception) {
            onStatus("Worker binding registry is unreadable · ${error.javaClass.simpleName}")
            AlertDialog.Builder(activity).setTitle("WORKER BINDING UNAVAILABLE")
                .setMessage("Registry validation failed closed. No binding was changed.")
                .setPositiveButton("OK", null).show()
            return
        }
        val repositoryInput = EditText(activity).apply {
            hint = "https://github.com/owner/test-repository"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            existing?.let { setText(it.repositoryUrl) }
        }
        val shaInput = EditText(activity).apply {
            hint = "Exact 40-character lowercase base commit SHA"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            existing?.let { setText(it.commitSha) }
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
            addView(TextView(activity).apply { text = "SYSTEM ID · worker"; textSize = 14f },
                ViewGroup.LayoutParams(-1, -2))
            addView(repositoryInput, ViewGroup.LayoutParams(-1, -2))
            addView(shaInput, ViewGroup.LayoutParams(-1, -2))
            addView(TextView(activity).apply {
                text = "ALLOWED PATHS · README.md only (locked)\nOWNER CONFIRMATION · required\nPROVIDER / LIVE CALL · OFF"
                textSize = 12f
                setPadding(0, 12, 0, 8)
            }, ViewGroup.LayoutParams(-1, -2))
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("BIND TEST WORKER")
            .setMessage("One worker, one owner-selected immutable test repository. This screen does not inspect remote content, call a model, or enable repository execution.")
            .setView(content)
            .setPositiveButton("REVIEW BINDING", null)
            .setNegativeButton("CANCEL", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val proposed = try {
                    VyomFactoryWorkerBindingPolicy.createBinding(
                        repositoryInput.text.toString(), shaInput.text.toString(), System.currentTimeMillis()
                    )
                } catch (error: Exception) {
                    val message = error.message ?: "Invalid worker binding"
                    if (repositoryInput.text.isNullOrBlank()) repositoryInput.error = message else shaInput.error = message
                    return@setOnClickListener
                }
                val current = try {
                    registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
                } catch (error: Exception) {
                    onStatus("Worker binding registry is unreadable · ${error.javaClass.simpleName}")
                    return@setOnClickListener
                }
                if (current != null &&
                    (current.repositoryUrl != proposed.repositoryUrl ||
                        current.commitSha != proposed.commitSha ||
                        current.allowedPaths != proposed.allowedPaths)
                ) {
                    onStatus("Worker already has a different immutable binding; no overwrite or migration was attempted.")
                    AlertDialog.Builder(activity).setTitle("EXISTING BINDING IS DIFFERENT")
                        .setMessage("Current binding:\n${current.repositoryUrl}\n${current.commitSha}\n${current.allowedPaths.joinToString()}\n\nA separate explicit migration flow is required. This screen will not replace it.")
                        .setPositiveButton("OK", null).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(activity)
                    .setTitle("OWNER CONFIRMATION")
                    .setMessage(
                        "System: worker\nRepository: ${proposed.repositoryUrl}\n" +
                            "Base SHA: ${proposed.commitSha}\nAllowed path: README.md only\n\nConfirm this exact immutable test binding?"
                    )
                    .setPositiveButton("CONFIRM BINDING") { _, _ ->
                        try {
                            val saved = VyomFactoryWorkerBindingPolicy.register(registry, proposed, ownerConfirmed = true)
                            onStatus("Worker test binding saved · ${saved.repositoryUrl} · ${saved.commitSha.take(12)} · README.md only · live-call OFF")
                        } catch (error: Exception) {
                            onStatus("Worker binding rejected · ${error.message ?: error.javaClass.simpleName}")
                            AlertDialog.Builder(activity).setTitle("BINDING REJECTED")
                                .setMessage(error.message ?: "Registry rejected the binding")
                                .setPositiveButton("OK", null).show()
                        }
                    }
                    .setNegativeButton("CANCEL", null)
                    .show()
            }
        }
        dialog.show()
    }
}
