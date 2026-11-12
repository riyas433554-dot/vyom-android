package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.UUID

/** Local owner approval UI for a structured, registry-bound repository change plan. */
internal class VyomFactoryRepositoryChangeReviewDialog(
    private val activity: Activity,
    private val task: VyomFactoryGitHubClient.Task,
    private val candidate: Candidate,
    private val approvalStore: VyomFactoryRepositoryApprovalStore,
    private val onApproved: (VyomFactoryRepositoryChangePolicy.Plan, VyomFactoryRepositoryApprovalStore.Token) -> Unit,
    private val onRejected: () -> Unit = {}
) {
    data class Candidate(
        val plan: VyomFactoryRepositoryChangePolicy.Plan,
        val binding: VyomFactorySystemRegistry.Binding,
        val summary: String,
        val sourceFileSha256: Map<String, String> = emptyMap()
    )

    fun show() {
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 8)
        }
        fun section(label: String, value: String, maxLines: Int = 8) {
            content.addView(TextView(activity).apply {
                text = label
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 10, 0, 2)
            })
            content.addView(TextView(activity).apply {
                text = value
                textSize = 13f
                setTextIsSelectable(true)
                this.maxLines = maxLines
            })
        }
        section("REPOSITORY", candidate.binding.repositoryUrl, 2)
        section("IMMUTABLE BASE", "${candidate.plan.baseBranch} · ${candidate.plan.baseCommitSha.take(12)}", 1)
        section(
            "FILE PATHS",
            candidate.plan.changes.joinToString("\n") { "• ${it.path}" },
            candidate.plan.changes.size.coerceAtLeast(1)
        )
        section("PROPOSED FILE CONTENT", formatFileContents(candidate.plan.changes), Int.MAX_VALUE)
        section("SUMMARY", candidate.summary.lines().take(3).joinToString("\n").ifBlank { candidate.plan.title }, 3)
        val scroll = ScrollView(activity).apply { addView(content) }
        AlertDialog.Builder(activity)
            .setTitle("Review repository change · #${task.number}")
            .setView(scroll)
            .setNegativeButton("REJECT") { _, _ -> onRejected() }
            .setPositiveButton("APPROVE PLAN") { _, _ ->
                try {
                    // Only this local owner tap can issue an approval token.
                    val token = approvalStore.issueFromOwnerReview(candidate.plan, candidate.binding)
                    onApproved(candidate.plan, token)
                } catch (error: Exception) {
                    AlertDialog.Builder(activity)
                        .setTitle("Approval rejected")
                        .setMessage(error.message ?: "Could not create local approval")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setOnCancelListener { onRejected() }
            .show()
    }

    companion object {
        internal fun formatFileContents(changes: List<VyomFactoryRepositoryChangePolicy.Change>): String =
            changes.joinToString("\n\n") { "PATH: ${it.path}\n\n${it.content}" }

        /** Issue fields are proposal input only. In particular, ownerApproved is never read. */
        fun parseCandidate(
            issueNumber: Int,
            body: String?,
            registry: VyomFactorySystemRegistry
        ): Candidate {
            val envelope = VyomFactoryExecutionEnvelope.parse(body, issueNumber)
            require(envelope.isExecutable &&
                envelope.target == VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE) {
                "Issue does not contain a valid repository-change proposal"
            }
            val args = envelope.arguments
            val systemId = args.getString("system_id")
            val binding = registry.get(systemId)
                ?: throw SecurityException("Repository proposal has no local system binding")
            val changesJson = args.optJSONArray("changes")
                ?: throw IllegalArgumentException("Repository proposal changes are missing")
            require(changesJson.length() in 1..VyomFactoryRepositoryChangePolicy.MAX_FILES)
            val changes = (0 until changesJson.length()).map { index ->
                val item: JSONObject = changesJson.getJSONObject(index)
                VyomFactoryRepositoryChangePolicy.Change(item.getString("path"), item.getString("content"))
            }
            val plan = VyomFactoryRepositoryChangePolicy.Plan(
                systemId = systemId,
                issueNumber = issueNumber,
                attemptId = "review-${UUID.randomUUID()}",
                title = args.getString("title"),
                baseBranch = args.getString("base_branch"),
                baseCommitSha = args.getString("base_commit_sha"),
                changes = changes,
                ownerApproved = false
            )
            VyomFactoryRepositoryChangePolicy.validateCandidate(plan, registry)
            return Candidate(plan, binding, args.optString("summary").take(1_000))
        }

        fun fromIssue(
            activity: Activity,
            task: VyomFactoryGitHubClient.Task,
            registry: VyomFactorySystemRegistry,
            approvalStore: VyomFactoryRepositoryApprovalStore,
            onApproved: (VyomFactoryRepositoryChangePolicy.Plan, VyomFactoryRepositoryApprovalStore.Token) -> Unit,
            onRejected: () -> Unit = {},
            workerModel: VyomFactoryRepositoryWorkerModelAdapter = VyomFactoryDisabledWorkerModelAdapter(),
            control: VyomFactoryExecutionControl = object : VyomFactoryExecutionControl {
                override fun shouldStop() = false
            }
        ): VyomFactoryRepositoryChangeReviewDialog {
            val candidate = workerModel.propose(task, registry, control)
            return VyomFactoryRepositoryChangeReviewDialog(
                activity, task, candidate, approvalStore, onApproved, onRejected
            )
        }
    }
}
