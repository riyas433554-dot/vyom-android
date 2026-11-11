package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID

/** Durable, device-local, review-only proposals produced by the isolated worker pool. */
internal class VyomFactoryWorkerProposalStore(private val root: File) {
    data class StoredProposal(
        val issueNumber: Int,
        val sourceId: String,
        val updatedAt: String,
        val workerId: String,
        val leaseId: String,
        val candidate: VyomFactoryRepositoryChangeReviewDialog.Candidate,
        val status: String
    )

    @Synchronized
    fun save(
        task: VyomFactoryGitHubClient.Task,
        workerId: String,
        leaseId: String,
        candidate: VyomFactoryRepositoryChangeReviewDialog.Candidate
    ): StoredProposal {
        require(task.number > 0 && task.metadata.sourceId.isNotBlank() && task.updatedAt.isNotBlank())
        require(VyomFactoryWorkerIdentityStore.isValidWorkerId(workerId))
        require(leaseId.isNotBlank() && leaseId.length <= 160)
        require(candidate.plan.issueNumber == task.number && !candidate.plan.ownerApproved)
        require(candidate.plan.baseCommitSha == candidate.binding.commitSha)
        require(candidate.plan.systemId == candidate.binding.systemId)
        val previous = get(task.number)
        if (previous != null && previous.sourceId == task.metadata.sourceId &&
            previous.updatedAt == task.updatedAt && previous.status in HANDLED_STATES
        ) return previous
        val stored = StoredProposal(
            task.number, task.metadata.sourceId, task.updatedAt, workerId, leaseId, candidate, "awaiting_review"
        )
        write(stored)
        return stored
    }

    @Synchronized
    fun get(issueNumber: Int): StoredProposal? {
        require(issueNumber > 0)
        val file = proposalFile(issueNumber)
        if (!file.exists()) return null
        if (!file.isFile) throw IllegalStateException("Worker proposal path is not a regular file")
        return decode(file.readText(Charsets.UTF_8)).also {
            require(it.issueNumber == issueNumber) { "Worker proposal issue identity does not match its file" }
        }
    }

    @Synchronized
    fun pending(): List<StoredProposal> {
        if (!root.exists()) return emptyList()
        check(root.canonicalFile.isDirectory) { "Worker proposal store is not a directory" }
        return root.canonicalFile.listFiles().orEmpty()
            .filter { it.isFile && it.name.matches(Regex("^issue-[1-9][0-9]*\\.json$")) }
            .map { file -> decode(file.readText(Charsets.UTF_8)) }
            .filter { it.status == "awaiting_review" }
            .sortedBy { it.issueNumber }
    }

    @Synchronized
    fun isHandled(task: VyomFactoryGitHubClient.Task): Boolean {
        val current = get(task.number) ?: return false
        return current.sourceId == task.metadata.sourceId &&
            current.updatedAt == task.updatedAt && current.status in HANDLED_STATES
    }

    @Synchronized
    fun markRejected(issueNumber: Int): Boolean {
        val current = get(issueNumber) ?: return false
        if (current.status == "rejected") return true
        write(current.copy(status = "rejected"))
        return true
    }

    @Synchronized
    fun remove(issueNumber: Int): Boolean {
        val file = proposalFile(issueNumber)
        if (!file.exists()) return false
        check(file.isFile && file.delete()) { "Worker proposal could not be removed" }
        return true
    }

    private fun write(value: StoredProposal) {
        val directory = root.canonicalFile
        if (!directory.exists()) check(directory.mkdirs()) { "Worker proposal store could not be created" }
        check(directory.isDirectory)
        val destination = proposalFile(value.issueNumber)
        check(destination.parentFile == directory) { "Worker proposal path escaped its store" }
        val temp = File(directory, ".${destination.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(encode(value).toByteArray(Charsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            Files.move(temp.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (error: Exception) {
            temp.delete()
            throw IllegalStateException("Worker proposal could not be atomically persisted", error)
        }
    }

    private fun proposalFile(issueNumber: Int): File {
        require(issueNumber > 0)
        val directory = root.canonicalFile
        val file = File(directory, "issue-$issueNumber.json").canonicalFile
        require(file.parentFile == directory) { "Worker proposal path escaped its store" }
        return file
    }

    private fun encode(value: StoredProposal): String {
        val binding = value.candidate.binding
        val plan = value.candidate.plan
        val allowedPaths = JSONArray().also { array -> binding.allowedPaths.forEach(array::put) }
        val changes = JSONArray().also { array ->
            plan.changes.forEach { change ->
                array.put(JSONObject().put("path", change.path).put("content", change.content))
            }
        }
        return JSONObject()
            .put("schema", 1)
            .put("issue_number", value.issueNumber)
            .put("source_id", value.sourceId)
            .put("updated_at", value.updatedAt)
            .put("worker_id", value.workerId)
            .put("lease_id", value.leaseId)
            .put("status", value.status)
            .put("summary", value.candidate.summary)
            .put("binding", JSONObject()
                .put("system_id", binding.systemId)
                .put("repository_url", binding.repositoryUrl)
                .put("commit_sha", binding.commitSha)
                .put("allowed_paths", allowedPaths)
                .put("registered_at", binding.registeredAtEpochMs))
            .put("plan", JSONObject()
                .put("system_id", plan.systemId)
                .put("issue_number", plan.issueNumber)
                .put("attempt_id", plan.attemptId)
                .put("title", plan.title)
                .put("base_branch", plan.baseBranch)
                .put("base_commit_sha", plan.baseCommitSha)
                .put("owner_approved", false)
                .put("changes", changes))
            .toString()
    }

    private fun decode(raw: String): StoredProposal {
        try {
            require(raw.length in 2..MAX_STORED_CHARS)
            val root = JSONObject(raw)
            require(root.getInt("schema") == 1)
            val bindingJson = root.getJSONObject("binding")
            val pathsJson = bindingJson.getJSONArray("allowed_paths")
            val allowedPaths = (0 until pathsJson.length()).map { pathsJson.getString(it) }
            val binding = VyomFactorySystemRegistry.Binding(
                bindingJson.getString("system_id"),
                bindingJson.getString("repository_url"),
                bindingJson.getString("commit_sha"),
                allowedPaths,
                bindingJson.getLong("registered_at")
            )
            val planJson = root.getJSONObject("plan")
            require(!planJson.getBoolean("owner_approved")) { "Stored worker plan cannot carry approval" }
            val changesJson = planJson.getJSONArray("changes")
            require(changesJson.length() in 1..VyomFactoryRepositoryChangePolicy.MAX_FILES)
            val changes = (0 until changesJson.length()).map { index ->
                val item = changesJson.getJSONObject(index)
                VyomFactoryRepositoryChangePolicy.Change(item.getString("path"), item.getString("content"))
            }
            val plan = VyomFactoryRepositoryChangePolicy.Plan(
                planJson.getString("system_id"),
                planJson.getInt("issue_number"),
                planJson.getString("attempt_id"),
                planJson.getString("title"),
                planJson.getString("base_branch"),
                planJson.getString("base_commit_sha"),
                changes,
                false
            )
            val candidate = VyomFactoryRepositoryChangeReviewDialog.Candidate(
                plan, binding, root.optString("summary").take(1_000)
            )
            val issue = root.getInt("issue_number")
            val sourceId = root.getString("source_id")
            val updatedAt = root.getString("updated_at")
            val workerId = root.getString("worker_id")
            val leaseId = root.getString("lease_id")
            val status = root.getString("status")
            require(issue > 0 && sourceId.isNotBlank() && sourceId.length <= 160 &&
                updatedAt.isNotBlank() && updatedAt.length <= 80 &&
                VyomFactoryWorkerIdentityStore.isValidWorkerId(workerId) &&
                leaseId.isNotBlank() && leaseId.length <= 160 &&
                status in ALL_STATES && plan.issueNumber == issue &&
                plan.systemId == binding.systemId && plan.baseCommitSha == binding.commitSha)
            return StoredProposal(issue, sourceId, updatedAt, workerId, leaseId, candidate, status)
        } catch (error: Exception) {
            throw IllegalStateException("Worker proposal is unreadable; refusing to review or execute it", error)
        }
    }

    companion object {
        private const val MAX_STORED_CHARS = 1_100_000
        private val ALL_STATES = setOf("awaiting_review", "rejected")
        private val HANDLED_STATES = setOf("awaiting_review", "rejected")
    }
}
