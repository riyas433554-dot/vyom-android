package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class SelfModificationPatchTest {
    @Test fun appliesOneExactMinimalReplacement() {
        val source = "fun reply() = \"hello\"\n"
        val result = SelfModificationPatch.apply(
            source,
            listOf(SelfModificationPatch.Operation("hello", "hello world"))
        )
        assertEquals("fun reply() = \"hello world\"\n", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAmbiguousFindTextInsteadOfReplacingMultipleLocations() {
        SelfModificationPatch.apply(
            "val first = 1\nval second = 1\n",
            listOf(SelfModificationPatch.Operation("= 1", "= 2"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingFindText() {
        SelfModificationPatch.apply(
            "val first = 1\n",
            listOf(SelfModificationPatch.Operation("not present", "replacement"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyFindText() {
        SelfModificationPatch.apply(
            "val first = 1\n",
            listOf(SelfModificationPatch.Operation("", "replacement"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPatchList() {
        SelfModificationPatch.apply("val first = 1\n", emptyList())
    }
}
