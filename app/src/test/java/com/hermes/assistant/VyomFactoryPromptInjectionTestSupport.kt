package com.hermes.assistant

import java.io.File

/** Shared real-adapter fixtures for worker-model and prompt-injection tests. */
internal object VyomFactoryPromptInjectionTestSupport {
    val BASE_SHA = "a".repeat(40)
    const val REPOSITORY = "https://github.com/riyas433554-dot/vyom-android"

    class MemoryKeyStore(private val value: String) : VyomFactoryWorkerApiKeyStore {
        override fun readKey(): CharArray = value.toCharArray()
        override fun saveKey(value: CharArray) = Unit
        override fun clear() = Unit
    }

    class MemoryJournal : VyomFactoryWorkerModelCallJournal {
        val entries = mutableListOf<VyomFactoryWorkerModelCallJournal.Entry>()
        override fun record(entry: VyomFactoryWorkerModelCallJournal.Entry) {
            val existing = entries.indexOfFirst { it.callId == entry.callId }
            if (existing >= 0) entries[existing] = entry else entries += entry
        }
        override fun callsSince(epochMillis: Long): Int = entries.count { it.timestampMillis >= epochMillis && it.providerCallStarted }
        override fun callsForTask(taskNumber: Int): Int = entries.count { it.taskNumber == taskNumber && it.providerCallStarted }
        override fun tokensForTask(taskNumber: Int): Long = entries.filter { it.taskNumber == taskNumber && it.providerCallStarted }.sumOf { it.inputTokens.toLong() + it.outputTokens.toLong() }
        override fun consecutiveOutputCount(taskNumber: Int, outputHash: String): Int = entries
            .filter { it.taskNumber == taskNumber && it.result in setOf("FAILED", "BLOCKED") }
            .asReversed()
            .takeWhile { it.result != "SUCCESS" && it.outputHash == outputHash }
            .size
    }

    class FakeProvider(private val response: () -> VyomFactoryWorkerModelProvider.Response) : VyomFactoryWorkerModelProvider {
        override val providerId = "fake"
        var calls = 0
        var lastRequest: VyomFactoryWorkerModelProvider.Request? = null
        var lastKey: String? = null
        override fun generate(request: VyomFactoryWorkerModelProvider.Request, apiKey: CharArray): VyomFactoryWorkerModelProvider.Response {
            calls++
            lastRequest = request
            lastKey = String(apiKey)
            return response()
        }
    }

    fun validJson(path: String = "app/src/main/java/com/hermes/assistant/WorkerGeneratedNote.kt") = """
        {
          "repository": "$REPOSITORY",
          "base_sha": "$BASE_SHA",
          "files": [{"path": "$path", "content": "Updated by owner-reviewed plan."}]
        }
    """.trimIndent()

    fun task(body: String = "Please add one small safe Kotlin helper.", number: Int = 42) =
        VyomFactoryGitHubClient.Task(
            number, "Document the worker flow", body, "open",
            "https://github.com/riyas433554-dot/vyom-android/issues/$number", "2026-11-08",
            VyomFactoryStatus.Metadata("P2", "open", "test", "worker-task-$number", "worker-task-$number", emptyList())
        )

    fun registry(): VyomFactorySystemRegistry {
        val root = File.createTempFile("worker-model-registry", ".tmp").apply { delete(); mkdirs() }
        return VyomFactorySystemRegistry(root).also {
            it.register(VyomFactoryWorkerBindingPolicy.createBinding(REPOSITORY, BASE_SHA, 1L), ownerConfirmed = true)
        }
    }

    fun adapter(
        provider: VyomFactoryWorkerModelProvider,
        journal: MemoryJournal = MemoryJournal(),
        limits: VyomFactoryWorkerModelLimits = VyomFactoryWorkerModelLimits(),
        key: String = "fake-provider-key",
        now: Long = 1_800_000_000_000L,
        contextProvider: (String) -> String = { "" },
        warningLogger: (String, String, Throwable) -> Unit = { _, _, _ -> }
    ) = VyomFactoryStructuredWorkerModelAdapter(
        provider = provider,
        keyStore = MemoryKeyStore(key),
        journal = journal,
        limits = limits,
        nowMillis = { now },
        preCallAuthorization = { true },
        repositoryClient = VyomFactoryRepositoryClient(getJson = { requestPath ->
            val relativePath = requestPath.substringAfter("/contents/").substringBefore("?ref=")
            val sourceContent = "base snapshot for $relativePath".toByteArray(Charsets.UTF_8)
            """{"type":"file","path":"$relativePath","encoding":"base64","size":${sourceContent.size},"sha":"blob-sha","content":"${java.util.Base64.getEncoder().encodeToString(sourceContent)}"}"""
        }),
        contextProvider = contextProvider,
        warningLogger = warningLogger
    )
}
