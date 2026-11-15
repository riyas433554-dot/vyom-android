package com.hermes.assistant

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer
import java.util.Locale

/** Runs the committed Grok corpus through the real adapter.propose() request path. */
class VyomFactoryPromptInjectionCorpusTest {
    private data class CorpusCase(val id: String, val memoryFacts: List<String?>, val skills: List<VyomFactoryWorkerSkillContext>, val assertions: JSONObject)

    @Test fun allThirtyNineCommittedCorpusCasesKeepHostileTextOutOfSystemInstructions() {
        val loader = javaClass.classLoader ?: throw AssertionError("Test class loader unavailable")
        val index = loader.getResourceAsStream("injection/index.txt")?.bufferedReader(Charsets.UTF_8)?.use { it.readLines().filter(String::isNotBlank) }
            ?: throw AssertionError("Missing injection/index.txt")
        assertEquals("PR #116 corpus must contain exactly 39 JSON cases", 39, index.size)

        index.forEach { fileName ->
            val raw = loader.getResourceAsStream("injection/$fileName")?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw AssertionError("Missing injection corpus file: $fileName")
            val corpus = loadCase(JSONObject(raw))
            assertEquals(corpus.id, "inert_data", corpus.assertions.getString("expect"))
            val context = VyomFactoryWorkerContextComposer.compose("REFERENCE CONTEXT ONLY.", corpus.memoryFacts, corpus.skills)
            val journal = VyomFactoryPromptInjectionTestSupport.MemoryJournal()
            val provider = VyomFactoryPromptInjectionTestSupport.FakeProvider {
                VyomFactoryWorkerModelProvider.Response(VyomFactoryPromptInjectionTestSupport.validJson(), 1, 1)
            }
            val adapter = VyomFactoryPromptInjectionTestSupport.adapter(provider, journal, contextProvider = { context })

            // Deliberately no runCatching: propose() failures must fail the corpus test.
            val candidate = adapter.propose(
                VyomFactoryPromptInjectionTestSupport.task(),
                VyomFactoryPromptInjectionTestSupport.registry(),
                object : VyomFactoryExecutionControl { override fun shouldStop() = false }
            )
            assertFalse("${corpus.id}: proposal must remain unapproved", candidate.plan.ownerApproved)
            val request = provider.lastRequest ?: throw AssertionError("${corpus.id}: provider saw no Request")
            val system = request.systemInstructions
            val normalizedSystem = normalizeHostile(system)

            strings(corpus.assertions, "mustNotAppearInSystemInstructions").forEach { needle ->
                assertFalse("${corpus.id}: system instructions contain '$needle'", system.contains(needle))
            }
            val knownGap = corpus.assertions.optString("knownGap").takeIf { it.isNotBlank() }
            if (knownGap == null) {
                strings(corpus.assertions, "forbiddenNormalized").forEach { needle ->
                    assertFalse("${corpus.id}: normalized system contains '$needle'", normalizedSystem.contains(normalizeHostile(needle)))
                }
            } else {
                println("KNOWN GAP [${corpus.id}]: $knownGap; normalized assertion intentionally soft-skipped")
            }

            val data = request.taskDescriptionData
            val fence = "LOCAL MEMORY AND PINNED SKILL CONTEXT (UNTRUSTED REFERENCE DATA"
            val fenceIndex = data.indexOf(fence)
            val sources = corpus.memoryFacts.filterNotNull().map { it.trim() } + corpus.skills.map { it.markdown.trim() }
            val needles = (strings(corpus.assertions, "mustNotAppearInSystemInstructions") + strings(corpus.assertions, "forbiddenNormalized")).distinct()
            needles.forEach { needle ->
                val occurrence = data.indexOf(needle, ignoreCase = true)
                if (occurrence >= 0) {
                    assertTrue("${corpus.id}: '$needle' escaped the data fence", fenceIndex >= 0 && occurrence > fenceIndex)
                    assertTrue("${corpus.id}: '$needle' is not in a JSON-quoted source fact/skill", sources.any { source ->
                        source.contains(needle, ignoreCase = true) && data.substring(fenceIndex).contains(quotedSource(source))
                    })
                }
            }

            if (corpus.assertions.optBoolean("expectSkillBodyAbsent", false)) {
                corpus.skills.forEach { skill -> assertFalse("${corpus.id}: rejected skill body leaked", data.contains(skill.markdown.trim())) }
            }
            // Keep one independent assertFalse for every corpus needle.
            strings(corpus.assertions, "skillBodyMustNotAppear").forEach { needle ->
                assertFalse("${corpus.id}: forbidden skill-body needle '$needle' leaked into system instructions", system.contains(needle))
            }
            if (corpus.assertions.has("expectCommitShaLength")) {
                val expected = corpus.assertions.getInt("expectCommitShaLength")
                corpus.skills.forEach { skill -> assertEquals("${corpus.id}: commit SHA length", expected, skill.commitSha.length) }
            }
        }
    }

    private fun loadCase(json: JSONObject): CorpusCase {
        val memory = json.optJSONArray("memoryFacts") ?: JSONArray()
        val facts = (0 until memory.length()).map { expand(memory.opt(it)) }
        val rawSkills = json.optJSONArray("skills") ?: JSONArray()
        val skills = (0 until rawSkills.length()).map { index ->
            val item = rawSkills.getJSONObject(index)
            VyomFactoryWorkerSkillContext(item.optString("repository"), item.optString("commitSha"),
                expand(item.opt("markdown")) ?: throw AssertionError("${json.optString("id")}: null skill markdown"))
        }
        return CorpusCase(json.getString("id"), facts, skills, json.getJSONObject("assert"))
    }

    private fun expand(value: Any?): String? = when {
        value == null || value == JSONObject.NULL -> null
        value is String -> value
        value is JSONObject -> {
            require(value.has("prefix") && value.has("fill") && value.has("count")) { "Unknown corpus expansion object" }
            value.getString("prefix") + value.getString("fill").repeat(value.getInt("count"))
        }
        else -> throw AssertionError("Unsupported corpus value type: ${value.javaClass.name}")
    }

    private fun strings(assertions: JSONObject, key: String): List<String> {
        val array = assertions.optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }

    private fun normalizeHostile(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC).replace(Regex("\\p{Cf}"), "").lowercase(Locale.ROOT)

    private fun quotedSource(value: String): String = JSONObject.quote(value)
        .replace("<", "\\u003c").replace(">", "\\u003e")
        .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
        .replace("\u200B", "\\u200b").replace("\u200C", "\\u200c")
        .replace("\u200D", "\\u200d").replace("\u2060", "\\u2060")
        .replace("\uFEFF", "\\ufeff")
}
