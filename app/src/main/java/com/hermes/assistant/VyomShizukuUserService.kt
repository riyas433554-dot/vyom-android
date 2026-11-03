package com.hermes.assistant

import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs as Shizuku's shell identity only after the owner starts Shizuku and grants Vyom access.
 * Commands are argv arrays from ShizukuCommandPolicy; no shell parser or arbitrary command text.
 */
class VyomShizukuUserService : IVyomShellService.Stub() {
    private val running = ConcurrentHashMap<String, Process>()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()
    private val completed = ConcurrentHashMap.newKeySet<String>()

    override fun execute(
        requestId: String,
        commandId: String,
        args: Array<out String>,
        timeoutMs: Long,
        ownerConfirmed: Boolean
    ): String {
        if (!requestId.matches(Regex("^[A-Za-z0-9-]{1,64}$"))) return "rejected: invalid request id"
        if (completed.contains(requestId)) return "rejected: request id already completed"
        if (cancelled.remove(requestId)) return "cancelled: STOP arrived before execution"
        if (running.containsKey(requestId)) return "rejected: duplicate request id"
        val command = ShizukuCommandPolicy.command(
            commandId = commandId,
            args = args.toList(),
            ownerConfirmed = ownerConfirmed,
            packageName = "com.vyom.assistant"
        ) ?: return "rejected: command is not allowlisted or owner confirmation is missing"
        val boundedTimeout = timeoutMs.coerceIn(100L, command.timeoutMs)
        return try {
            val process = ProcessBuilder(command.argv).redirectErrorStream(true).start()
            if (running.putIfAbsent(requestId, process) != null) {
                process.destroyForcibly()
                return "rejected: duplicate request id"
            }
            if (cancelled.contains(requestId)) {
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
                                val allowed = minOf(count, MAX_OUTPUT_BYTES - captured.size())
                                captured.write(buffer, 0, allowed)
                            }
                        }
                        output.set(captured.toString(Charsets.UTF_8.name()))
                    }
                } catch (_: Exception) {
                    // Output is best-effort; exit state remains authoritative.
                }
            }.apply { name = "VyomShizukuOutput"; isDaemon = true; start() }

            val finished = process.waitFor(boundedTimeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(300, TimeUnit.MILLISECONDS)
                if (cancelled.remove(requestId)) "cancelled: STOP interrupted the command"
                else "timeout: command exceeded ${boundedTimeout}ms"
            } else {
                reader.join(300)
                if (cancelled.remove(requestId)) "cancelled: STOP interrupted the command"
                else "exit=${process.exitValue()}; output=${output.get().take(MAX_OUTPUT_BYTES)}"
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            "cancelled: executor thread interrupted"
        } catch (e: Exception) {
            "failed: ${e.javaClass.simpleName}"
        } finally {
            running.remove(requestId)?.let { process ->
                if (process.isAlive) process.destroyForcibly()
            }
            cancelled.remove(requestId)
            completed.add(requestId)
            if (completed.size > 256) completed.clear()
        }
    }

    override fun cancel(requestId: String) {
        if (!requestId.matches(Regex("^[A-Za-z0-9-]{1,64}$"))) return
        if (completed.contains(requestId)) return
        cancelled.add(requestId)
        running[requestId]?.let { process ->
            try {
                process.destroy()
                if (!process.waitFor(250, TimeUnit.MILLISECONDS)) process.destroyForcibly()
            } catch (_: Exception) {
                process.destroyForcibly()
            }
        }
    }

    override fun destroy() {
        running.keys.toList().forEach(::cancel)
        System.exit(0)
    }

    private companion object {
        const val MAX_OUTPUT_BYTES = 8 * 1024
    }
}
