package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

/** Owner-confirmed binding of the code worker to the trusted app repository and safe Kotlin roots. */
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
            hint = VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            existing?.takeIf { it.repositoryUrl == VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL }?.let { setText(it.repositoryUrl) }
                ?: setText(VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL)
        }
        val shaInput = EditText(activity).apply {
            hint = "Exact 40-character lowercase SHA on feature/real-worker-model"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            isEnabled = false
            setText("")
        }
        var fetchedHead: String? = null
        val headStatus = TextView(activity).apply {
            text = "BRANCH HEAD · NOT FETCHED"
            textSize = 11f
        }
        val fetchHeadButton = android.widget.Button(activity).apply {
            text = "FETCH CURRENT BRANCH HEAD"
            setOnClickListener {
                isEnabled = false
                text = "FETCHING GITHUB HEAD…"
                headStatus.text = "FETCHING feature/real-worker-model FROM GITHUB…"
                Thread {
                    val result = runCatching {
                        VyomFactoryRepositoryClient().readBranchHead(
                            repositoryInput.text.toString().trim(),
                            BASE_BRANCH
                        )
                    }
                    activity.runOnUiThread {
                        if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                        isEnabled = true
                        text = "REFRESH CURRENT BRANCH HEAD"
                        result.onSuccess { sha ->
                            fetchedHead = sha
                            shaInput.setText(sha)
                            headStatus.text = "GITHUB HEAD · $BASE_BRANCH · $sha"
                            onStatus("Fetched current GitHub branch HEAD · $BASE_BRANCH · ${sha.take(12)}")
                        }.onFailure { error ->
                            fetchedHead = null
                            shaInput.setText("")
                            headStatus.text = "BRANCH HEAD FETCH FAILED · ${error.message?.take(120) ?: error.javaClass.simpleName}"
                            onStatus("GitHub branch HEAD fetch failed · ${error.message?.take(120) ?: error.javaClass.simpleName}")
                        }
                    }
                }.start()
            }
        }
        val roots = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
            addView(TextView(activity).apply { text = "SYSTEM ID · worker"; textSize = 14f },
                ViewGroup.LayoutParams(-1, -2))
            addView(repositoryInput, ViewGroup.LayoutParams(-1, -2))
            addView(fetchHeadButton, ViewGroup.LayoutParams(-1, -2))
            addView(headStatus, ViewGroup.LayoutParams(-1, -2))
            addView(shaInput, ViewGroup.LayoutParams(-1, -2))
            addView(TextView(activity).apply {
                text = "ALLOWED ROOTS ·\n${roots.joinToString("\n")}\n" +
                    "Kotlin source + unit tests only. Factory runtime, safety/control, build, manifest, workflow, signing and credential files are blocked.\n" +
                    "OWNER REVIEW · required before any repository write\nLIVE MODEL CALLS · OFF until separately enabled"
                textSize = 12f
                setPadding(0, 12, 0, 8)
            }, ViewGroup.LayoutParams(-1, -2))
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("BIND FACTORY CODE WORKER")
            .setMessage(
                "The worker is restricted to the trusted Vyom Android repository and an immutable commit. " +
                    "It can create unapproved proposals only; it cannot merge or write without the separate owner-review flow."
            )
            .setView(content)
            .setPositiveButton("REVIEW BINDING", null)
            .setNegativeButton("CANCEL", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (fetchedHead.isNullOrBlank() || shaInput.text.toString() != fetchedHead) {
                    headStatus.text = "FETCH THE CURRENT GITHUB BRANCH HEAD BEFORE CONFIRMING"
                    return@setOnClickListener
                }
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
                val changed = current != null && (
                    current.repositoryUrl != proposed.repositoryUrl ||
                        current.commitSha != proposed.commitSha ||
                        current.allowedPaths != proposed.allowedPaths
                    )
                if (changed) {
                    val currentBinding = current ?: return@setOnClickListener
                    AlertDialog.Builder(activity)
                        .setTitle("MIGRATE WORKER BINDING")
                        .setMessage(
                            "CURRENT BINDING\n${currentBinding.repositoryUrl}\n${currentBinding.commitSha}\n${currentBinding.allowedPaths.joinToString()}\n\n" +
                                "NEW BINDING\n${proposed.repositoryUrl}\n${proposed.commitSha}\n${proposed.allowedPaths.joinToString()}\n\n" +
                                "Migration replaces the previous immutable worker scope. Existing live-call authorization will be revoked. " +
                                "No model call or repository write happens during migration."
                        )
                        .setPositiveButton("CONFIRM MIGRATION") { _, _ ->
                            try {
                                val saved = VyomFactoryWorkerBindingPolicy.migrate(
                                    registry, currentBinding, proposed, ownerConfirmed = true
                                )
                                runCatching { VyomFactoryWorkerLiveCallAuthorizationStore(activity).revoke() }
                                VyomFactoryWorkerReadmeVerificationCache.invalidate()
                                onStatus(
                                    "Worker binding migrated · ${saved.repositoryUrl} · ${saved.commitSha.take(12)} · " +
                                        "safe Kotlin roots · live calls OFF; re-enable separately."
                                )
                            } catch (error: Exception) {
                                onStatus("Worker binding migration rejected · ${error.message ?: error.javaClass.simpleName}")
                                AlertDialog.Builder(activity).setTitle("MIGRATION REJECTED")
                                    .setMessage(error.message ?: "Registry rejected the migration")
                                    .setPositiveButton("OK", null).show()
                            }
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(activity)
                    .setTitle("OWNER CONFIRMATION")
                    .setMessage(
                        "System: worker\nRepository: ${proposed.repositoryUrl}\n" +
                            "Base SHA: ${proposed.commitSha}\nAllowed roots:\n${proposed.allowedPaths.joinToString("\n")}\n\n" +
                            "Only safe Kotlin source and unit-test files are eligible. Protected Factory/security/build files remain blocked. " +
                            "Every proposal still requires owner review. Confirm this exact immutable binding?"
                    )
                    .setPositiveButton("CONFIRM BINDING") { _, _ ->
                        try {
                            val saved = VyomFactoryWorkerBindingPolicy.register(registry, proposed, ownerConfirmed = true)
                            onStatus(
                                "Factory code worker binding saved · ${saved.repositoryUrl} · ${saved.commitSha.take(12)} · " +
                                    "safe Kotlin roots · live-call OFF"
                            )
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

    private companion object {
        const val BASE_BRANCH = VyomFactoryWorkerBindingPolicy.BASE_BRANCH
    }
}
