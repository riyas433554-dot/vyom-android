package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local owner approval UI for a structured, registry-bound repository change plan. */
internal class VyomFactoryRepositoryChangeReviewDialog(
    private val activity: Activity,
    private val task: VyomFactoryGitHubClient.Task,
    private val plan: VyomFactoryRepositoryChangePolicy.Plan,
    private val binding: VyomFactorySystemRegistry.Binding,
    private val approvalStore: VyomFactoryRepositoryApprovalStore,
    private val summary: String,
    private val onApproved: (VyomFactoryRepositoryChangePolicy.Plan, VyomFactoryRepositoryApprovalStore.Token) -> Unit,
    private val onRejected: () -> Unit = {}
) {
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
        section("REPOSITORY", binding.repositoryUrl, 2)
        section("IMMUTABLE BASE", plan.baseCommitSha.take(12), 1)
        section("FILE PATHS", plan.changes.joinToString("\n") { "• ${it.path}" }, 8)
        section("SUMMARY", summary.lines().take(3).joinToString("\n").ifBlank { plan.title }, 3)
        val scroll = ScrollView(activity).apply { addView(content) }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Review repository change · #${task.number}")
            .setView(scroll)
            .setNegativeButton("REJECT") { _, _ -> onRejected() }
            .setPositiveButton("APPROVE PLAN") { _, _ ->
                try {
                    // The token is created only inside this local positive-button handler.
                    val token = approvalStore.issueFromOwnerReview(plan, binding)
                    onApproved(plan, token)
                } catch (error: Exception) {
                    AlertDialog.Builder(activity)
                        .setTitle("Approval rejected")
                        .setMessage(error.message ?: "Could not create local approval")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .create()
        dialog.setOnCancelListener { onRejected() }
        dialog.show()
    }

    companion object {
        /** Parse candidate fields only. No issue-controlled approval field is read. */
        fun fromIssue(
            activity: Activity,
            task: VyomFactoryGitHubClient.Task,
            registry: VyomFactorySystemRegistry,
            approvalStore: VyomFactoryRepositoryApprovalStore,
            onApproved: (VyomFactoryRepositoryChangePolicy.Plan, VyomFactoryRepositoryApprovalStore.Token) -> Unit,
            onRejected: () -> Unit = {}
        ): VyomFactoryRepositoryChangeReviewDialog {
            val envelope = VyomFactoryExecutionEnvelope.parse(task.body)
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
                VyomFactoryRepositoryChangePolicy.Change(
                    path = item.getString("path"),
                    content = item.getString("content")
                )
            }
            val plan = VyomFactoryRepositoryChangePolicy.Plan(
                systemId = systemId,
                issueNumber = task.number,
                attemptId = "review-${UUID.randomUUID()}",
                title = args.getString("title"),
                baseBranch = args.getString("base_branch"),
                baseCommitSha = args.getString("base_commit_sha"),
                changes = changes,
                ownerApproved = false
            )
            VyomFactoryRepositoryChangePolicy.validateCandidate(plan, registry)
            return VyomFactoryRepositoryChangeReviewDialog(
                activity, task, plan, binding, approvalStore,
                args.optString("summary").take(1_000), onApproved, onRejected
            )
        }
    }
}
