package com.hermes.assistant

/** Deterministic task intelligence. Findings are advisory and never mutate or dispatch the source task. */
internal object VyomFactoryTaskIntelligence {
    enum class Stage { PLANNING, IMPLEMENTATION, TEST }
    enum class FindingKind { BUG, MISSING_FEATURE, IDEA }
    data class Finding(val stage: Stage, val kind: FindingKind, val title: String, val detail: String)
    data class SeparateTaskProposal(val parentIssueNumber: Int, val title: String, val description: String, val sourceId: String)

    private val dependencyPattern = Regex("(?im)^\\s*(?:Blocked by|Depends on)\\s*:\\s*(.+)$")
    private val issuePattern = Regex("#(\\d+)")
    private val criteriaHeader = Regex("(?im)^\\s*Acceptance criteria\\s*:\\s*$")
    private val criteriaItem = Regex("(?m)^\\s*[-*]\\s*\\[([ xX])\\]\\s+(.+?)\\s*$")
    private val maxIterationsPattern = Regex("(?im)^\\s*Max iterations\\s*:\\s*(\\d+)\\s*$")
    private val iterationsUsedPattern = Regex("(?im)^\\s*Iterations used\\s*:\\s*(\\d+)\\s*$")
    private val markerPattern = Regex("^\\s*\\[(BUG|MISSING_FEATURE|IDEA)]\\s+(.+?)\\s*$", RegexOption.IGNORE_CASE)

    fun dependencies(task: VyomFactoryGitHubClient.Task): Set<Int> =
        dependencyPattern.findAll(cleanBody(task.body)).flatMap { match ->
            issuePattern.findAll(match.groupValues[1]).mapNotNull { it.groupValues[1].toIntOrNull() }
        }.filter { it > 0 }.toSet()

    /** A dependency is satisfied only when its issue is present, closed, and explicitly completed. */
    fun dependenciesSatisfied(
        task: VyomFactoryGitHubClient.Task,
        snapshot: List<VyomFactoryGitHubClient.Task>
    ): Boolean {
        val byNumber = snapshot.associateBy { it.number }
        return dependencies(task).all { dependencyNumber ->
            val dependency = byNumber[dependencyNumber] ?: return false
            dependency.state == "closed" && dependency.metadata.status == "completed"
        }
    }

    /** Topological order; existing priority/FIFO ordering breaks ties among ready tasks. */
    fun order(tasks: List<VyomFactoryGitHubClient.Task>): List<VyomFactoryGitHubClient.Task> {
        val byNumber = tasks.associateBy { it.number }
        val remaining = tasks.map { it.number }.toMutableSet()
        val result = mutableListOf<VyomFactoryGitHubClient.Task>()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { number ->
                dependencies(byNumber.getValue(number)).none { it in remaining }
            }.map(byNumber::getValue).sortedWith(
                compareBy<VyomFactoryGitHubClient.Task> { priorityRank(it.metadata.priority) }
                    .thenBy { it.createdAt }.thenBy { it.number }
            )
            if (ready.isEmpty()) {
                result += remaining.map(byNumber::getValue).sortedWith(
                    compareBy<VyomFactoryGitHubClient.Task> { priorityRank(it.metadata.priority) }
                        .thenBy { it.createdAt }.thenBy { it.number }
                )
                break
            }
            val next = ready.first()
            result += next
            remaining.remove(next.number)
        }
        return result
    }

    fun circularDependencyIssues(tasks: List<VyomFactoryGitHubClient.Task>): Set<Int> {
        val byNumber = tasks.associateBy { it.number }
        val visiting = mutableSetOf<Int>()
        val visited = mutableSetOf<Int>()
        val stack = mutableListOf<Int>()
        val cycles = mutableSetOf<Int>()
        fun visit(number: Int) {
            if (number in visited) return
            if (number in visiting) {
                cycles += stack.drop(stack.indexOf(number).coerceAtLeast(0))
                return
            }
            visiting += number
            stack += number
            dependencies(byNumber.getValue(number)).filter { it in byNumber }.forEach(::visit)
            stack.removeAt(stack.lastIndex)
            visiting -= number
            visited += number
        }
        tasks.forEach { visit(it.number) }
        return cycles
    }

    fun isDuplicate(finding: Finding, tasks: List<VyomFactoryGitHubClient.Task>): Boolean {
        val title = fingerprint(finding.title)
        val detail = fingerprint(finding.detail)
        if (title.isBlank()) return false
        return tasks.any { task ->
            val taskTitle = fingerprint(task.title.replace(Regex("^\\[VYOM FACTORY]\\s*"), "").replace(Regex("^\\[P[0-3]]\\s*"), ""))
            val taskBody = fingerprint(cleanBody(task.body))
            taskTitle == title || (detail.length >= 24 && taskBody.contains(detail))
        }
    }

    fun toSeparateTaskProposal(parentIssueNumber: Int, finding: Finding,
                               existingTasks: List<VyomFactoryGitHubClient.Task>): SeparateTaskProposal? {
        require(parentIssueNumber > 0)
        val title = finding.title.trim().take(120)
        val detail = finding.detail.trim().take(4_000)
        require(title.length >= 4 && detail.isNotBlank())
        if (isDuplicate(finding, existingTasks)) return null
        val identity = parentIssueNumber.toString() + "|" + finding.kind.name + "|" + fingerprint(title) + "|" + fingerprint(detail)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8)).take(12).joinToString("") { "%02x".format(it) }
        val description = "Detected during " + finding.stage.name.lowercase() + " for parent issue #" +
            parentIssueNumber + ".\n\n" + finding.kind.name + ": " + detail +
            "\n\nThis is a separate task; it does not expand the parent task scope."
        return SeparateTaskProposal(parentIssueNumber, title, description, "detector-" + parentIssueNumber + "-" + digest)
    }

    /** Only explicit structured markers are detected; arbitrary prose is not guessed into a task. */
    fun detect(stage: Stage, output: String): List<Finding> = output.lineSequence().mapNotNull { line ->
        val match = markerPattern.matchEntire(line) ?: return@mapNotNull null
        val kind = when (match.groupValues[1].uppercase()) {
            "BUG" -> FindingKind.BUG
            "MISSING_FEATURE" -> FindingKind.MISSING_FEATURE
            else -> FindingKind.IDEA
        }
        val detail = match.groupValues[2].trim()
        if (detail.length < 4) null else Finding(stage, kind, detail.take(120), detail)
    }.distinctBy { it.kind to fingerprint(it.detail) }.toList()

    fun maximumIterations(task: VyomFactoryGitHubClient.Task): Int =
        (maxIterationsPattern.find(cleanBody(task.body))?.groupValues?.get(1)?.toIntOrNull()
            ?: DEFAULT_MAX_ITERATIONS).coerceIn(1, MAX_ITERATIONS)

    fun isIterationLimitReached(task: VyomFactoryGitHubClient.Task, iterationsUsed: Int? = null): Boolean {
        val used = iterationsUsed ?: iterationsUsedPattern.find(cleanBody(task.body))?.groupValues?.get(1)?.toIntOrNull()
            ?: task.metadata.events.count { it.startsWith("iteration:") }
        return used >= maximumIterations(task)
    }

    fun acceptanceCriteriaSatisfied(task: VyomFactoryGitHubClient.Task): Boolean {
        val body = cleanBody(task.body)
        val header = criteriaHeader.find(body) ?: return false
        val section = body.substring(header.range.last + 1)
        val criteria = criteriaItem.findAll(section).toList()
        return criteria.isNotEmpty() && criteria.all {
            it.groupValues[1].equals("x", ignoreCase = true) && it.groupValues[2].trim().length >= 4
        }
    }

    private fun cleanBody(body: String) = body.substringBefore("<!-- VYOM_FACTORY_META:")
    private fun fingerprint(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), " ")
        .trim().replace(Regex("\\s+"), " ")
    private fun priorityRank(priority: String) = when (priority) { "P0" -> 0; "P1" -> 1; "P2" -> 2; "P3" -> 3; else -> 4 }
    const val DEFAULT_MAX_ITERATIONS = 3
    const val MAX_ITERATIONS = 10
}
