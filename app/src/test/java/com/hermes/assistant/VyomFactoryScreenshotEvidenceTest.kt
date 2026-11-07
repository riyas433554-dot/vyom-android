package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class VyomFactoryScreenshotEvidenceTest {
    @Test fun acceptsOnlyMatchingPngJpegAndWebpSignatures() {
        assertEquals("png", VyomFactoryScreenshotEvidencePolicy.extensionFor("image/png", png()))
        assertEquals("jpg", VyomFactoryScreenshotEvidencePolicy.extensionFor("image/jpeg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertEquals("webp", VyomFactoryScreenshotEvidencePolicy.extensionFor(
            "image/webp", "RIFFxxxxWEBP".toByteArray(Charsets.US_ASCII)
        ))
        assertRejected { VyomFactoryScreenshotEvidencePolicy.extensionFor("image/svg+xml", "<svg/>".toByteArray()) }
        assertRejected { VyomFactoryScreenshotEvidencePolicy.extensionFor("image/png", "not png".toByteArray()) }
    }

    @Test fun receivedScreenshotIsCopiedAndHashVerifiedInsidePrivateDirectory() {
        val root = createTempDir(prefix = "vyom-evidence-")
        try {
            val received = VyomFactoryScreenshotEvidenceStore(root).receive(
                ByteArrayInputStream(png()), "image/png"
            )
            assertTrue(received.file.isFile)
            assertTrue(received.file.canonicalPath.startsWith(root.canonicalPath))
            assertEquals("png", received.file.extension)
            assertEquals(VyomFactoryScreenshotEvidencePolicy.sha256(png()), received.sha256)
            assertEquals(png().size, received.sizeBytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun uploadedReceiptPreventsASecondAttachAfterCommentFailure() {
        val root = createTempDir(prefix = "vyom-evidence-receipt-")
        try {
            val store = VyomFactoryScreenshotEvidenceStore(root)
            val received = store.receive(ByteArrayInputStream(png()), "image/png")
            store.recordUploaded(received.file, "https://github.com/owner/repo/blob/branch/evidence.png",
                "b".repeat(40), received.sha256)
            val receipt = store.uploadReceipt(received.file)!!
            assertEquals("https://github.com/owner/repo/blob/branch/evidence.png", receipt.url)
            assertEquals("b".repeat(40), receipt.commitSha)
            assertEquals(received.sha256, receipt.sha256)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun corruptUploadReceiptFailsClosedInsteadOfAllowingDuplicateUpload() {
        val root = createTempDir(prefix = "vyom-evidence-corrupt-receipt-")
        try {
            val store = VyomFactoryScreenshotEvidenceStore(root)
            val received = store.receive(ByteArrayInputStream(png()), "image/png")
            File(root, received.file.name + ".uploaded.json").writeText("not-json")
            assertRejected { store.uploadReceipt(received.file) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun oversizeAndMimeMismatchNeverCreateFiles() {
        val root = createTempDir(prefix = "vyom-evidence-reject-")
        try {
            val store = VyomFactoryScreenshotEvidenceStore(root)
            assertRejected {
                store.receive(ByteArrayInputStream(ByteArray(VyomFactoryScreenshotEvidencePolicy.MAX_BYTES + 1)), "image/png")
            }
            assertRejected {
                store.receive(ByteArrayInputStream(png()), "image/jpeg")
            }
            assertFalse(root.listFiles()?.isNotEmpty() == true)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun png() = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected screenshot validation to reject the input")
        } catch (_: Exception) {
            // expected validation or corrupt receipt failure
        }
    }
}
