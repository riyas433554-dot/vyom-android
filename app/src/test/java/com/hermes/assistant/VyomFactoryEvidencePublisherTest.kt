package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VyomFactoryEvidencePublisherTest {
    private val baseCommit = "a".repeat(40)
    private val repo = "https://github.com/acme/vyom"
    private val image = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test fun unregisteredSystemFailsBeforeAnyRemoteMutation() {
        val root = createTempDir(prefix = "evidence-unregistered-")
        try {
            val pending = File(root, "pending").apply { mkdirs() }
            val file = File(pending, "image.png").apply { writeBytes(image) }
            val transport = FakeTransport()
            val publisher = VyomFactoryEvidencePublisher(
                VyomFactorySystemRegistry(File(root, "registry")),
                transport,
                VyomFactoryScreenshotEvidenceStore(pending)
            )
            assertRejected {
                publisher.publish("factory-evidence", repo, baseCommit, 7,
                    ".factory-evidence/issue-7/image.png", file)
            }
            assertEquals(0, transport.mutations)
        } finally { root.deleteRecursively() }
    }

    @Test fun wrongRepositoryFailsClosed() = withPublisher { publisher, transport, _, file ->
        assertRejected {
            publisher.publish("factory-evidence", "https://github.com/acme/other", baseCommit, 7,
                ".factory-evidence/issue-7/image.png", file)
        }
        assertEquals(0, transport.mutations)
    }

    @Test fun wrongPinnedCommitFailsClosed() = withPublisher { publisher, transport, _, file ->
        assertRejected {
            publisher.publish("factory-evidence", repo, "b".repeat(40), 7,
                ".factory-evidence/issue-7/image.png", file)
        }
        assertEquals(0, transport.mutations)
    }

    @Test fun pathTraversalIsRejectedBeforeRemoteMutation() = withPublisher { publisher, transport, _, file ->
        assertRejected {
            publisher.publish("factory-evidence", repo, baseCommit, 7,
                ".factory-evidence/issue-7/../outside.png", file)
        }
        assertEquals(0, transport.mutations)
    }

    @Test fun pathOutsideRegisteredRootIsRejected() = withPublisher { publisher, transport, _, file ->
        assertRejected {
            publisher.publish("factory-evidence", repo, baseCommit, 7, "README.md", file)
        }
        assertEquals(0, transport.mutations)
    }

    @Test fun staleEvidenceBranchHeadIsRejectedWithoutUpload() = withPublisher { publisher, transport, _, file ->
        transport.head = "c".repeat(40)
        assertRejected {
            publisher.publish("factory-evidence", repo, baseCommit, 7,
                ".factory-evidence/issue-7/image.png", file)
        }
        assertEquals(0, transport.blobWrites)
    }

    @Test fun uploadFailureNeverCreatesIssueComment() = withPublisher { publisher, transport, _, file ->
        transport.failUpload = true
        assertRejected {
            publisher.publish("factory-evidence", repo, baseCommit, 7,
                ".factory-evidence/issue-7/image.png", file)
        }
        assertEquals(0, transport.comments)
    }

    @Test fun commentFailureKeepsVerifiedUploadReceiptAndDoesNotClaimSuccess() = withPublisher { publisher, transport, store, file ->
        transport.failComment = true
        val result = publisher.publish("factory-evidence", repo, baseCommit, 7,
            ".factory-evidence/issue-7/image.png", file)
        assertFalse(result.issueCommentRecorded)
        assertEquals(VyomFactoryScreenshotEvidencePolicy.sha256(image), result.sha256)
        assertEquals(result.commitSha, transport.head)
        assertNotNull(store.uploadReceipt(file))
        assertEquals(1, transport.comments)
        assertFalse(transport.lastForce)
    }

    @Test fun successVerifiesHashBeforeCommentAndUsesNonForceRefUpdate() = withPublisher { publisher, transport, store, file ->
        val result = publisher.publish("factory-evidence", repo, baseCommit, 7,
            ".factory-evidence/issue-7/image.png", file)
        assertTrue(result.issueCommentRecorded)
        assertEquals(VyomFactoryScreenshotEvidencePolicy.sha256(image), result.sha256)
        assertEquals(result.commitSha, transport.head)
        assertTrue(transport.hashVerifiedBeforeComment)
        assertFalse(transport.lastForce)
        assertNull(store.uploadReceipt(file))
    }

    private fun withPublisher(block: (VyomFactoryEvidencePublisher, FakeTransport, VyomFactoryScreenshotEvidenceStore, File) -> Unit) {
        val root = createTempDir(prefix = "evidence-publisher-")
        try {
            val registry = VyomFactorySystemRegistry(File(root, "registry"))
            registry.register(
                VyomFactorySystemRegistry.Binding(
                    "factory-evidence", repo, baseCommit,
                    listOf(".factory-evidence"), 1L
                ),
                ownerConfirmed = true
            )
            val pending = File(root, "pending").apply { mkdirs() }
            val file = File(pending, "image.png").apply { writeBytes(image) }
            val store = VyomFactoryScreenshotEvidenceStore(pending)
            val transport = FakeTransport()
            block(VyomFactoryEvidencePublisher(registry, transport, store), transport, store, file)
        } finally { root.deleteRecursively() }
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            fail("Expected fail-closed evidence publish rejection")
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: IllegalStateException) {
        }
    }

    private inner class FakeTransport : VyomFactoryEvidencePublisher.Transport {
        var head: String? = null
        var mutations = 0
        var blobWrites = 0
        var comments = 0
        var failUpload = false
        var failComment = false
        var lastForce = true
        var hashVerifiedBeforeComment = false
        private var uploadedBytes: ByteArray? = null

        override fun isRepositoryPrivate(repositoryUrl: String) = true
        override fun branchHead(branch: String): String? = head
        override fun createBranch(branch: String, fromCommit: String) {
            mutations++
            if (head != null) throw IllegalStateException("Branch already exists")
            head = fromCommit
        }
        override fun commitTree(commitSha: String) = "d".repeat(40)
        override fun createBlob(bytes: ByteArray): String {
            if (failUpload) throw IllegalStateException("upload failed")
            mutations++
            blobWrites++
            uploadedBytes = bytes.copyOf()
            return "e".repeat(40)
        }
        override fun createTree(baseTreeSha: String, path: String, blobSha: String) = "f".repeat(40)
        override fun createCommit(message: String, treeSha: String, parentSha: String) = "9".repeat(40)
        override fun updateRef(branch: String, newCommit: String, expectedHead: String, force: Boolean) {
            mutations++
            if (head != expectedHead) throw SecurityException("stale expected head")
            lastForce = force
            if (force) throw SecurityException("force updates are forbidden")
            head = newCommit
        }
        override fun readFile(path: String, ref: String): ByteArray {
            check(ref == head)
            return uploadedBytes?.copyOf() ?: error("No uploaded content")
        }
        override fun addIssueComment(issueNumber: Int, message: String) {
            comments++
            hashVerifiedBeforeComment = uploadedBytes?.contentEquals(image) == true
            if (failComment) throw IllegalStateException("comment failed")
        }
    }
}
