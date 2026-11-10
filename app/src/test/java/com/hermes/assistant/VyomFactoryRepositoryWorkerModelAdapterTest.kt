package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryWorkerModelAdapterTest {
    private class MemoryJournal : VyomFactoryWorkerModelCallJournal {
        val entries = mutableListOf<VyomFactoryWorkerModelCallJournal.Entry>()
        override fun record(entry: VyomFactoryWorkerModelCallJournal.Entry) { entries += entry }
        override fun callsSince(epochMillis: Long): Int = entries.count {
            it.timestampMillis >= epochMillis && it.providerCallStarted
        }
        override fun callsForTask(taskNumber: Int): Int = entries.count {
            it.taskNumber == taskNumber && it.providerCallStarted
        }
        override fun tokensForTask(taskNumber: Int): Long = entries
            .filter { it.taskNumber == taskNumber && it.providerCallStarted }
            .sumOf { it.inputTokens.toLong() + it.outputTokens.toLong() }
    }

    @Test fun stopPreventsModelProposalParsing() {
        val root = File.createTempFile("repo-model", ".tmp").apply { delete(); mkdirs() }
        val registry = VyomFactorySystemRegistry(root).apply {
            register(
                VyomFactorySystemRegistry.Binding(
                    "worker",
                    "https://github.com/example/repo",
                    "a".repeat(40),
                    listOf("README.md"),
                    1L
                ),
                ownerConfirmed = true
            )
        }
        val journal = MemoryJournal()
        var providerCalls = 0
        val provider = object : VyomFactoryWorkerModelProvider {
            override val providerId = "stop-test"
            override fun generate(
                request: VyomFactoryWorkerModelProvider.Request,
                apiKey: CharArray
            ): VyomFactoryWorkerModelProvider.Response {
                providerCalls++
                error("STOP must prevent model invocation and proposal parsing")
            }
        }
        val keyStore = object : VyomFactoryWorkerApiKeyStore {
            override fun readKey(): CharArray = "test-key".toCharArray()
            override fun saveKey(value: CharArray) = Unit
            override fun clear() = Unit
        }
        val adapter = VyomFactoryStructuredWorkerModelAdapter(provider, keyStore, journal)
        val task = VyomFactoryGitHubClient.Task(
            1,
            "Repository proposal",
            "Add one line to README.md",
            "open",
            "https://github.com/example/repo/issues/1",
            "2026-11-09",
            VyomFactoryStatus.Metadata("P2", "open", "test", "source-12345678", "idempotency", emptyList())
        )

        org.junit.Assert.assertThrows(VyomFactoryExecutionStoppedException::class.java) {
            adapter.propose(task, registry, object : VyomFactoryExecutionControl {
                override fun shouldStop() = true
            })
        }

        assertEquals(0, providerCalls)
        assertEquals("STOPPED", journal.entries.single().result)
        assertFalse(journal.entries.single().providerCallStarted)
    }
}
