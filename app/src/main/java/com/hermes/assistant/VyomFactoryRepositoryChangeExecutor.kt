package com.hermes.assistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Creates one immutable, registry-bound multi-file commit and a draft PR.
 *
 * It never merges, force-pushes, installs, or accepts a caller-supplied repository
 * URL. The plan must be explicitly owner-approved and the base branch must still
 * point at the exact immutable commit stored in the system registry. A proposal is
 * not reported as verified until both required exact-SHA CI workflows pass.
 */
internal class VyomFactoryRepositoryChangeExecutor(
    private val registry: VyomFactorySystemRegistry,
    private val transportFactory: (String, String) -> Transport = { apiRoot, token ->
        GitHubTransport(apiRoot, token)
    },
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val waitMillis: (Long) -> Unit = { Thread.sleep(it) }
) {
    fun interface Transport {
        fun request(method: String, path: String, body: JSONObject? = null): JSONObject
    }

    data class Proposal(
        val branch: String,
        val commitSha: String,
        val pullRequestUrl: String,
        val ciEvidence: String
    )

    fun execute(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        token: String,
        control: VyomFactoryExecutionControl,
        timeoutMillis: Long = DEFAULT_CI_TIMEOUT_MS
    ): Proposal {
        val binding = VyomFactoryRepositoryChangePolicy.validate(plan, registry)
        require(token.trim().length >= 20) {
            "Add a GitHub token with repository Contents, Pull requests and Actions read permissions"
        }
        require(timeoutMillis in 1_000L..MAX_CI_TIMEOUT_MS) { "Invalid repository worker CI timeout" }
        val source = VyomFactoryRepositoryIntake.parseUrl(binding.repositoryUrl)
            ?: throw SecurityException("Registered repository URL is invalid")
        val apiRoot = "https://api.github.com/repos/${source.owner}/${source.name}"
        val transport = transportFactory(apiRoot, token)
        val branch = "vyom-worker-${plan.issueNumber}-${plan.attemptId.take(32)}"
        require(branch.length <= 100) { "Generated repository worker branch name is too long" }

        checkNotStopped(control)
        val baseRef = request(transport, control, "GET", "/git/ref/heads/${plan.baseBranch}")
        require(readRefSha(baseRef) == plan.baseCommitSha) {
            "Registered base commit is no longer the head of the approved branch; inspect and rebind before retrying"
        }

        checkNotStopped(control)
        val baseCommit = request(transport, control, "GET", "/git/commits/${plan.baseCommitSha}")
        val baseTreeSha = baseCommit.optJSONObject("tree")?.optString("sha").orEmpty()
        require(SHA.matches(baseTreeSha)) { "GitHub did not return a valid base tree SHA" }

        val entries = JSONArray()
        plan.changes.sortedBy { it.path }.forEach { change ->
            entries.put(
                JSONObject()
                    .put("path", change.path)
                    .put("mode", "100644")
                    .put("type", "blob")
                    .put("content", change.content)
            )
        }
        checkNotStopped(control)
        val tree = request(
            transport,
            control,
            "POST",
            "/git/trees",
            JSONObject().put("base_tree", baseTreeSha).put("tree", entries)
        )
        val treeSha = tree.optString("sha")
        require(SHA.matches(treeSha)) { "GitHub did not return a valid proposal tree SHA" }

        checkNotStopped(control)
        val commit = request(
            transport,
            control,
            "POST",
            "/git/commits",
            JSONObject()
                .put("message", "factory: issue #${plan.issueNumber} ${safeTitle(plan.title)}")
                .put("tree", treeSha)
                .put("parents", JSONArray().put(plan.baseCommitSha))
        )
        val commitSha = commit.optString("sha")
        require(SHA.matches(commitSha)) { "GitHub did not return a valid proposal commit SHA" }

        var branchCreated = false
        var pullRequestAttempted = false
        try {
            checkNotStopped(control)
            val createdRef = request(
                transport,
                control,
                "POST",
                "/git/refs",
                JSONObject()
                    .put("ref", "refs/heads/$branch")
                    .put("sha", commitSha)
            )
            require(readRefSha(createdRef) == commitSha) {
                "GitHub created a proposal ref that does not point to the expected commit"
            }
            branchCreated = true

            checkNotStopped(control)
            val verifiedRef = request(transport, control, "GET", "/git/ref/heads/$branch")
            require(readRefSha(verifiedRef) == commitSha) {
                "Proposal branch does not resolve to the exact generated commit"
            }

            // Never open a PR if the approved base branch moved during preparation.
            checkNotStopped(control)
            val latestBase = request(transport, control, "GET", "/git/ref/heads/${plan.baseBranch}")
            require(readRefSha(latestBase) == plan.baseCommitSha) {
                "Approved base branch moved during proposal creation; no pull request was opened"
            }

            checkNotStopped(control)
            pullRequestAttempted = true
            val pullRequest = try {
                request(
                    transport,
                    control,
                    "POST",
                    "/pulls",
                    JSONObject()
                        .put("title", "[Vyom worker] ${safeTitle(plan.title)}")
                        .put("head", branch)
                        .put("base", plan.baseBranch)
                        .put("draft", true)
                        .put(
                            "body",
                            buildString {
                                append("## Registry-bound Factory worker proposal\n\n")
                                append("- System: `").append(plan.systemId).append("`\n")
                                append("- Source issue: #").append(plan.issueNumber).append("\n")
                                append("- Immutable base: `").append(plan.baseCommitSha).append("`\n")
                                append("- Proposal commit: `").append(commitSha).append("`\n")
                                append("- Changed paths:\n")
                                plan.changes.sortedBy { it.path }.forEach {
                                    append("  - `").append(it.path).append("`\n")
                                }
                                append("- Owner approved the exact change plan before execution.\n")
                                append("- Draft PR only: no merge, release, install, or protected-branch update is performed.")
                            }
                        )
                )
            } catch (failure: Exception) {
                recoverPullRequest(transport, source.owner, branch, binding.repositoryUrl)?.let { return it }
                throw IllegalStateException(
                    "Pull request creation outcome is uncertain; proposal branch $branch was preserved for inspection",
                    failure
                )
            }

            val pullUrl = pullRequest.optString("html_url")
            if (!pullUrl.startsWith(binding.repositoryUrl.trimEnd('/') + "/pull/") ||
                pullRequest.optJSONObject("head")?.optString("ref")?.let { it != branch } == true
            ) {
                throw SecurityException("GitHub returned a pull request outside the registered repository or proposal branch")
            }

            val ciEvidence = awaitRequiredCi(transport, control, commitSha, timeoutMillis)
            return Proposal(branch, commitSha, pullUrl, ciEvidence)
        } catch (failure: Exception) {
            if (branchCreated && !pullRequestAttempted) {
                deleteBranchIfStillOurs(transport, branch, commitSha)
            }
            throw failure
        }
    }

    private fun awaitRequiredCi(
        transport: Transport,
        control: VyomFactoryExecutionControl,
        expectedSha: String,
        timeoutMillis: Long
    ): String {
        val deadline = nowMillis() + timeoutMillis
        var lastState = "waiting for both required workflows"
        while (nowMillis() < deadline) {
            checkNotStopped(control)
            val response = request(
                transport,
                control,
                "GET",
                "/actions/runs?head_sha=$expectedSha&per_page=20"
            )
            val runs = response.optJSONArray("workflow_runs") ?: JSONArray()
            val candidates = buildList {
                for (index in 0 until runs.length()) {
                    val run = runs.optJSONObject(index) ?: continue
                    add(
                        ProposalCiGatePolicy.Run(
                            name = run.optString("name"),
                            headSha = run.optString("head_sha"),
                            status = run.optString("status"),
                            conclusion = run.optString("conclusion").takeIf(String::isNotBlank),
                            id = run.optLong("id", 0L)
                        )
                    )
                }
            }
            val evaluation = ProposalCiGatePolicy.evaluate(expectedSha, candidates)
            when (evaluation.state) {
                ProposalCiGatePolicy.State.SUCCESS -> return evaluation.detail
                ProposalCiGatePolicy.State.FAILED -> throw IllegalStateException(evaluation.detail)
                ProposalCiGatePolicy.State.WAITING -> lastState = evaluation.detail
            }
            checkNotStopped(control)
            waitMillis(CI_POLL_INTERVAL_MS)
        }
        throw IllegalStateException("Timed out waiting for exact-SHA repository worker CI ($lastState)")
    }

    private fun recoverPullRequest(
        transport: Transport,
        owner: String,
        branch: String,
        repositoryUrl: String
    ): Proposal? {
        val head = URLEncoder.encode("$owner:$branch", Charsets.UTF_8.name())
        val response = runCatching {
            transport.request("GET", "/pulls?head=$head&state=all&per_page=10")
        }.getOrNull() ?: return null
        val pulls = response.optJSONArray("_array") ?: response.optJSONArray("pulls") ?: return null
        val found = (0 until pulls.length()).asSequence()
            .mapNotNull { pulls.optJSONObject(it) }
            .firstOrNull { it.optJSONObject("head")?.optString("ref") == branch }
            ?: return null
        val url = found.optString("html_url")
        if (!url.startsWith(repositoryUrl.trimEnd('/') + "/pull/")) {
            throw SecurityException("Recovered pull request does not belong to the registered repository")
        }
        val sha = found.optJSONObject("head")?.optJSONObject("sha")?.optString("sha").orEmpty()
        if (!SHA.matches(sha)) return null
        return Proposal(branch, sha, url, "CI status must be rechecked for the recovered exact commit")
    }

    private fun deleteBranchIfStillOurs(transport: Transport, branch: String, commitSha: String) {
        runCatching {
            val current = transport.request("GET", "/git/ref/heads/$branch")
            if (readRefSha(current) == commitSha) {
                transport.request("DELETE", "/git/refs/heads/$branch")
            }
        }
    }

    private fun request(
        transport: Transport,
        control: VyomFactoryExecutionControl,
        method: String,
        path: String,
        body: JSONObject? = null
    ): JSONObject {
        checkNotStopped(control)
        return transport.request(method, path, body)
    }

    private fun readRefSha(ref: JSONObject): String =
        ref.optJSONObject("object")?.optString("sha").orEmpty()

    private fun checkNotStopped(control: VyomFactoryExecutionControl) {
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
    }

    private fun safeTitle(value: String): String =
        value.replace(Regex("[\\r\\n\\t]+"), " ").trim().take(100)

    private class GitHubTransport(
        apiRoot: String,
        private val token: String,
        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    ) : Transport {
        private val apiRoot = apiRoot.trimEnd('/')
        private val jsonType = "application/json; charset=utf-8".toMediaType()

        override fun request(method: String, path: String, body: JSONObject?): JSONObject {
            val builder = Request.Builder()
                .url(apiRoot + path)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Vyom-Factory-Worker")
            val requestBody = body?.toString()?.toRequestBody(jsonType)
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonType))
                "DELETE" -> builder.delete()
                else -> throw IllegalArgumentException("Unsupported GitHub worker method")
            }
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(text).optString("message") }.getOrDefault("")
                    throw GitHubApiException(response.code, "GitHub worker API ${response.code}: ${message.take(160)}")
                }
                if (text.isBlank()) return JSONObject()
                return when (val parsed = JSONTokener(text).nextValue()) {
                    is JSONObject -> parsed
                    is JSONArray -> JSONObject().put("_array", parsed)
                    else -> JSONObject()
                }
            }
        }
    }

    private class GitHubApiException(val statusCode: Int, message: String) : IllegalStateException(message)

    companion object {
        private val SHA = Regex("^[a-f0-9]{40}$")
        private const val CI_POLL_INTERVAL_MS = 5_000L
        private const val DEFAULT_CI_TIMEOUT_MS = 15 * 60 * 1_000L
        private const val MAX_CI_TIMEOUT_MS = 20 * 60 * 1_000L
    }
}
