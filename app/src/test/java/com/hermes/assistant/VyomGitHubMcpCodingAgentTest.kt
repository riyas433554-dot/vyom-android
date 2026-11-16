package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class VyomGitHubMcpCodingAgentTest {
    private class FakeProvider(private val outputs: MutableList<String>) : VyomFactoryWorkerModelProvider {
        override val providerId = "fake"

        override fun generate(
            request: VyomFactoryWorkerModelProvider.Request,
            apiKey: CharArray
        ): VyomFactoryWorkerModelProvider.Response {
            assertTrue(request.systemInstructions.contains("Vyom"))
            assertEquals(77, request.taskNumber)
            assertEquals("a".repeat(40), request.baseSha)
            assertEquals(VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS, request.allowedPaths)
            return VyomFactoryWorkerModelProvider.Response(outputs.removeAt(0), 10, 5)
        }
    }

    private class FakeKeyStore : VyomFactoryWorkerApiKeyStore {
        override fun readKey() = charArrayOf('x')
        override fun saveKey(value: CharArray) = Unit
        override fun clear() = Unit
    }

    private class FakeTransport : VyomGitHubMcpClient.Transport {
        val calls = mutableListOf<Pair<String, JSONObject>>()

        override fun post(
            endpoint: String,
            token: String,
            sessionId: String?,
            protocolVersion: String?,
            payload: String
        ): VyomGitHubMcpClient.Transport.Response {
            val request = JSONObject(payload)
            return when (request.optString("method")) {
                "initialize" -> VyomGitHubMcpClient.Transport.Response(
                    200, "application/json", "session-1",
                    JSONObject().put("jsonrpc", "2.0")
                        .put("id", request.getLong("id"))
                        .put("result", JSONObject().put("protocolVersion", "2025-03-26"))
                        .toString()
                )
                "notifications/initialized" -> VyomGitHubMcpClient.Transport.Response(202, "", "session-1", "")
                "tools/list" -> {
                    val names = listOf("get_file_contents", "create_branch", "push_files", "create_pull_request", "delete_file")
                    val tools = JSONArray()
                    names.forEach { name ->
                        tools.put(JSONObject().put("name", name).put("description", name)
                            .put("inputSchema", JSONObject().put("type", "object")))
                    }
                    VyomGitHubMcpClient.Transport.Response(
                        200, "application/json", "session-1",
                        JSONObject().put("jsonrpc", "2.0")
                            .put("id", request.getLong("id"))
                            .put("result", JSONObject().put("tools", tools))
                            .toString()
                    )
                }
                "tools/call" -> {
                    val params = request.getJSONObject("params")
                    val name = params.getString("name")
                    val args = params.getJSONObject("arguments")
                    calls += name to args
                    VyomGitHubMcpClient.Transport.Response(
                        200, "application/json", "session-1",
                        JSONObject().put("jsonrpc", "2.0")
                            .put("id", request.getLong("id"))
                            .put("result", JSONObject().put("content", JSONArray()
                                .put(JSONObject().put("type", "text").put("text", "ok"))))
                            .toString()
                    )
                }
                else -> error("Unexpected MCP method")
            }
        }
    }

    @Test
    fun modelCanInspectCreateIsolatedBranchPushChangesAndOpenPr() {
        val transport = FakeTransport()
        val provider = FakeProvider(mutableListOf(
            """{"action":"tool","name":"get_file_contents","arguments":{"owner":"acme","repo":"factory","path":"src","ref":"feature/repair"}}""",
            """{"action":"tool","name":"create_branch","arguments":{"owner":"acme","repo":"factory","branch":"vyom-agent/test-run","from_branch":"feature/repair"}}""",
            """{"action":"tool","name":"push_files","arguments":{"owner":"acme","repo":"factory","branch":"vyom-agent/test-run","message":"Fix factory issue","files":[{"path":"app/src/main/java/com/hermes/assistant/Fix.kt","content":"class Fix"}]}}""",
            """{"action":"tool","name":"create_pull_request","arguments":{"owner":"acme","repo":"factory","title":"Fix factory issue","head":"vyom-agent/test-run","base":"feature/repair","body":"Fixes Factory issue #77","draft":true}}""",
            """{"action":"final","message":"PR created and ready for CI."}"""
        ))
        val client = VyomGitHubMcpClient({ "test-token" }, transport = transport)
        val agent = VyomGitHubMcpCodingAgent(
            client = client,
            provider = provider,
            keyStore = FakeKeyStore(),
            repository = "acme/factory",
            taskNumber = 77,
            baseBranch = "feature/repair",
            baseSha = "a".repeat(40),
            allowedPaths = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS,
            branchName = "vyom-agent/test-run"
        )

        val result = agent.run("Fix the failing Factory source tests")

        assertTrue(result.startsWith("PR created and ready for CI."))
        assertTrue(result.contains("Branch created: true"))
        assertTrue(result.contains("Files written: true"))
        assertEquals(
            listOf("get_file_contents", "create_branch", "push_files", "create_pull_request"),
            transport.calls.map { it.first }
        )
        assertEquals("feature/repair", transport.calls[1].second.getString("from_branch"))
        assertEquals("vyom-agent/test-run", transport.calls[2].second.getString("branch"))
        assertEquals("feature/repair", transport.calls[3].second.getString("base"))
    }

    @Test(expected = SecurityException::class)
    fun modelCannotCallDeleteToolEvenWhenServerAdvertisesIt() {
        val transport = FakeTransport()
        val provider = FakeProvider(mutableListOf(
            """{"action":"tool","name":"delete_file","arguments":{"owner":"acme","repo":"factory","path":"src/Important.kt","branch":"feature/repair"}}"""
        ))
        val agent = VyomGitHubMcpCodingAgent(
            client = VyomGitHubMcpClient({ "test-token" }, transport = transport),
            provider = provider,
            keyStore = FakeKeyStore(),
            repository = "acme/factory",
            taskNumber = 77,
            baseBranch = "feature/repair",
            baseSha = "a".repeat(40),
            allowedPaths = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS,
            branchName = "vyom-agent/test-run"
        )

        agent.run("Delete a file")
    }

    @Test(expected = VyomFactoryExecutionStoppedException::class)
    fun stopAfterModelDecisionPreventsGitHubWrite() {
        val transport = FakeTransport()
        var stopChecks = 0
        val control = object : VyomFactoryExecutionControl {
            override fun shouldStop(): Boolean {
                stopChecks += 1
                return stopChecks >= 3
            }
        }
        val provider = FakeProvider(mutableListOf(
            """{"action":"tool","name":"get_file_contents","arguments":{"owner":"acme","repo":"factory","path":"src","ref":"feature/repair"}}"""
        ))
        val agent = VyomGitHubMcpCodingAgent(
            client = VyomGitHubMcpClient({ "test-token" }, transport = transport),
            provider = provider,
            keyStore = FakeKeyStore(),
            repository = "acme/factory",
            taskNumber = 77,
            baseBranch = "feature/repair",
            baseSha = "a".repeat(40),
            allowedPaths = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS,
            branchName = "vyom-agent/test-run",
            control = control
        )

        agent.run("Inspect the repository")
        assertTrue(transport.calls.isEmpty())
    }
    @Test(expected = SecurityException::class)
    fun modelCannotWriteFactoryControlFilesEvenInsideWorkerSourceRoot() {
        val transport = FakeTransport()
        val provider = FakeProvider(mutableListOf(
            """{"action":"tool","name":"create_branch","arguments":{"owner":"acme","repo":"factory","branch":"vyom-agent/test-run","from_branch":"feature/repair"}}""",
            """{"action":"tool","name":"push_files","arguments":{"owner":"acme","repo":"factory","branch":"vyom-agent/test-run","message":"Modify Factory control","files":[{"path":"app/src/main/java/com/hermes/assistant/VyomFactoryWorkerDispatchWorker.kt","content":"// bypass"}]}}"""
        ))
        val agent = VyomGitHubMcpCodingAgent(
            client = VyomGitHubMcpClient({ "test-token" }, transport = transport),
            provider = provider,
            keyStore = FakeKeyStore(),
            repository = "acme/factory",
            taskNumber = 77,
            baseBranch = "feature/repair",
            baseSha = "a".repeat(40),
            allowedPaths = VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS,
            branchName = "vyom-agent/test-run"
        )

        agent.run("Modify Factory control code")
    }

}
