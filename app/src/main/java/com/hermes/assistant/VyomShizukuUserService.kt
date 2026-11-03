package com.hermes.assistant

import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Shizuku shell-only executor. Requests are fixed command IDs, bounded and owner-authorized.
 * STOP and execute share an atomic lifecycle registry to close startup/completion races.
 */
class VyomShizukuUserService : IVyomShellService.Stub() {
    private val running = ConcurrentHashMap<String, Process>()
    private val requests = ShizukuRequestRegistry()

    override fun execute(
        requestId: String,
        commandId: String,
        args: Array<out String>,
        timeoutMs: Long,
        ownerConfirmed: Boolean
    ): String {
        if (!requestId.matches(Regex("^[A-Za-z0-9-]{1,64}$"))) return "rejected: invalid request id"
        val command = ShizukuCommandPolicy.command(
            commandId = commandId,
            args = args.toList(),
            ownerConfirmed = ownerConfirmed,
            packageName = "com.vyom.assistant"
        ) ?: return "rejected: command is not allowlisted or owner confirmation is missing"

        when (requests.begin(requestId)) {
            ShizukuRequestRegistry.Begin.CANCELLED_BEFORE_START -> return "cancelled: STOP arrived before execution"
            ShizukuRequestRegistry.Begin.DUPLICATE -> return "rejected: duplicate or previously completed request id"
            ShizukuRequestRegistry.Begin.ACCEPTED -> Unit
        }
        val boundedTimeout = timeoutMs.coerceIn(100L, command.timeoutMs)
        var process: Process? = null
        return try {
            process = ProcessBuilder(command.argv).redirectErrorStream(true).start()
            if (running.putIfAbsent(requestId, process) != null) {
                process.destroyForcibly()
                return "rejected: duplicate request id"
            }
            if (!requests.markRunning(requestId)) {
                process.destroyForcibly()
                return "cancelled: STOP interrupted command startup"
            }
            val output = AtomicReference("")
            val reader = Thread {
                try {
                    process.inputStream.use { input ->
                        val buffer = ByteArray(1024)
                        val captured = ByteArrayOutputStream()
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (captured.size() < MAX_OUTPUT_BYTES) {
                                captured.write(buffer, 0, minOf(count, MAX_OUTPUT_BYTES - captured.size()))
                            }
                        }
                        output.set(captured.toString(Charsets.UTF_8.name()))
                    }
                } catch (_: Exception) {
                    // Exit state remains authoritative; output is best-effort.
                }
            }.apply { name = "VyomShizukuOutput"; isDaemon = true; start() }

            val finished = process.waitFor(boundedTimeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(300, TimeUnit.MILLISECONDS)
                if (requests.isCancelled(requestId)) "cancelled: STOP interrupted the command"
                else "timeout: command exceeded ${boundedTimeout}ms"
            } else {
                reader.join(300)
                if (requests.isCancelled(requestId)) "cancelled: STOP interrupted the command"
                else "exit=${process.exitValue()}; output=${output.get().take(MAX_OUTPUT_BYTES)}"
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            "cancelled: executor thread interrupted"
        } catch (e: Exception) {
            "failed: ${e.javaClass.simpleName}"
        } finally {
            running.remove(requestId)?.let { child ->
                if (child.isAlive) child.destroyForcibly()
            }
            if (requests.isCancelled(requestId)) {
                Log.i(TAG, "request=$requestId command=$commandId outcome=cancelled")
            } else {
                Log.i(TAG, "request=$requestId command=$commandId outcome=completed")
            }
            requests.complete(requestId)
        }
    }

    override fun cancel(requestId: String) {
        if (!requestId.matches(Regex("^[A-Za-z0-9-]{1,64}$"))) return
        val accepted = requests.cancel(requestId)
        val process = running[requestId]
        if (process != null) {
            try {
                process.destroy()
                if (!process.waitFor(250, TimeUnit.MILLISECONDS)) process.destroyForcibly()
            } catch (_: Exception) {
                process.destroyForcibly()
            }
        }
        if (accepted) Log.i(TAG, "request=$requestId outcome=STOP_requested")
    }

    override fun destroy() {
        running.keys.toList().forEach(::cancel)
        System.exit(0)
    }

    private companion object {
        const val TAG = "VyomShizuku"
        const val MAX_OUTPUT_BYTES = 8 * 1024
    }
}
