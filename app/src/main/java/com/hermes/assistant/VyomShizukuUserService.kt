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
            ShizukuRequestRegistry.Begin.CAPACITY_REACHED -> return "rejected: Shizuku request capacity reached"
            ShizukuRequestRegistry.Begin.SERVICE_STOPPED -> return "rejected: Shizuku service is shutting down"
            ShizukuRequestRegistry.Begin.ACCEPTED -> Unit
        }
        val boundedTimeout = timeoutMs.coerceIn(100L, command.timeoutMs)
        var auditOutcome = "failed"
        var exitCode: Int? = null
        return try {
            val process = ProcessBuilder(command.argv).redirectErrorStream(true).start()
            if (running.putIfAbsent(requestId, process) != null) {
                auditOutcome = "rejected_duplicate"
                ShizukuProcessTerminator.terminate(
                    isAlive = process::isAlive,
                    destroy = process::destroy,
                    waitForExit = { process.waitFor(250, TimeUnit.MILLISECONDS) },
                    destroyForcibly = process::destroyForcibly
                )
                return "rejected: duplicate request id"
            }
            if (!requests.markRunning(requestId)) {
                auditOutcome = "cancelled_startup"
                ShizukuProcessTerminator.terminate(
                    isAlive = process::isAlive,
                    destroy = process::destroy,
                    waitForExit = { process.waitFor(250, TimeUnit.MILLISECONDS) },
                    destroyForcibly = process::destroyForcibly
                )
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
                ShizukuProcessTerminator.terminate(
                    isAlive = process::isAlive,
                    destroy = process::destroy,
                    waitForExit = { process.waitFor(300, TimeUnit.MILLISECONDS) },
                    destroyForcibly = process::destroyForcibly
                )
                if (requests.isCancelled(requestId)) {
                    auditOutcome = "cancelled"
                    "cancelled: STOP interrupted the command"
                } else {
                    auditOutcome = "timeout"
                    "timeout: command exceeded ${boundedTimeout}ms"
                }
            } else {
                reader.join(300)
                if (requests.isCancelled(requestId)) {
                    auditOutcome = "cancelled"
                    "cancelled: STOP interrupted the command"
                } else {
                    exitCode = process.exitValue()
                    auditOutcome = if (exitCode == 0) "success" else "nonzero_exit"
                    "exit=$exitCode; output=${output.get().take(MAX_OUTPUT_BYTES)}"
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            auditOutcome = if (requests.isCancelled(requestId)) "cancelled" else "interrupted"
            "cancelled: executor thread interrupted"
        } catch (e: Exception) {
            auditOutcome = "failed"
            "failed: ${e.javaClass.simpleName}"
        } finally {
            running.remove(requestId)?.let { child ->
                ShizukuProcessTerminator.terminate(
                    isAlive = child::isAlive,
                    destroy = child::destroy,
                    waitForExit = { child.waitFor(250, TimeUnit.MILLISECONDS) },
                    destroyForcibly = child::destroyForcibly
                )
            }
            if (requests.isCancelled(requestId)) auditOutcome = "cancelled"
            Log.i(TAG, ShizukuAudit.format(requestId, commandId, auditOutcome, exitCode))
            requests.complete(requestId)
        }
    }

    override fun cancel(requestId: String) {
        if (!requestId.matches(Regex("^[A-Za-z0-9-]{1,64}$"))) return
        val accepted = requests.cancel(requestId)
        val process = running[requestId]
        if (process != null) {
            ShizukuProcessTerminator.terminate(
                isAlive = process::isAlive,
                destroy = process::destroy,
                waitForExit = { process.waitFor(250, TimeUnit.MILLISECONDS) },
                destroyForcibly = process::destroyForcibly
            )
        }
        if (accepted) Log.i(TAG, "request=$requestId outcome=STOP_requested")
    }

    override fun destroy() {
        // Close request admission and mark STARTING/RUNNING requests cancelled in
        // the same critical section before taking the process-termination snapshot.
        val stoppingIds = requests.shutdown()
        ShizukuProcessTerminator.terminateAll(
            requestIds = stoppingIds,
            processFor = { running[it] },
            terminate = ::terminateProcess
        )
        // A request may have passed begin() but not yet published its Process when
        // shutdown took its snapshot. Keep the service alive while that execute()
        // observes the closed registry, kills any late child and completes cleanup.
        requests.awaitQuiescence(2_000L)
        running.values.toList().forEach(::terminateProcess)
        System.exit(0)
    }

    private fun terminateProcess(process: Process): Boolean = ShizukuProcessTerminator.terminate(
        isAlive = process::isAlive,
        destroy = process::destroy,
        waitForExit = { process.waitFor(250, TimeUnit.MILLISECONDS) },
        destroyForcibly = process::destroyForcibly
    )

    private companion object {
        const val TAG = "VyomShizuku"
        const val MAX_OUTPUT_BYTES = 8 * 1024
    }
}
