package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryRepositoryCatalogTest {
    @Test fun curatedCandidatesHaveCanonicalUniqueUrlsAndStableIds() {
        val candidates = VyomFactoryRepositoryCatalog.all

        assertEquals(17, candidates.size)
        assertEquals(candidates.size, candidates.map { it.id }.toSet().size)
        assertEquals(candidates.size, candidates.map { it.canonicalUrl }.toSet().size)
        candidates.forEach { candidate ->
            val parsed = VyomFactoryRepositoryIntake.parseUrl(candidate.canonicalUrl)
            assertNotNull("Invalid curated URL for ${candidate.id}", parsed)
            assertEquals(candidate.canonicalUrl, parsed?.canonicalUrl)
        }
    }

    @Test fun securityRepositoriesAreReferenceOnlyAndNeverExecutable() {
        val security = VyomFactoryRepositoryCatalog.byTrack(
            VyomFactoryRepositoryCatalog.Track.SECURITY_REFERENCE
        )

        assertEquals(7, security.size)
        assertTrue(security.all { it.referenceOnly })
        assertFalse(VyomFactoryRepositoryCatalog.all.any { it.executionAllowed })
    }

    @Test fun missingOrRestrictedLicensesRequireOwnerLicenseReview() {
        val blurr = VyomFactoryRepositoryCatalog.all.single { it.id == "blurr" }
        val isair = VyomFactoryRepositoryCatalog.all.single { it.id == "isair-jarvis" }
        val shannon = VyomFactoryRepositoryCatalog.all.single { it.id == "shannon" }

        assertTrue(blurr.licenseReviewRequired)
        assertTrue(isair.licenseReviewRequired)
        assertTrue(shannon.licenseReviewRequired)
    }

    @Test fun highPriorityAndroidAndMemorySourcesAreDiscoverableByTrack() {
        val android = VyomFactoryRepositoryCatalog.byTrack(
            VyomFactoryRepositoryCatalog.Track.ANDROID_CONTROL
        )
        val memory = VyomFactoryRepositoryCatalog.byTrack(
            VyomFactoryRepositoryCatalog.Track.MEMORY
        )

        assertTrue(android.any { it.id == "anaay-jarvis" })
        assertTrue(android.any { it.id == "jarvis-os" })
        assertTrue(memory.any { it.id == "mem0" })
        assertTrue(memory.any { it.id == "openjarvis" })
        assertTrue(memory.any { it.id == "letta-code" })
    }
}
