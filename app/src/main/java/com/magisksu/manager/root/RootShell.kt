package com.magisksu.manager.root

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Result of a shell command executed through `su`.
 */
data class ShellResult(
    val code: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean get() = code == 0

    val combined: String
        get() = buildString {
            if (stdout.isNotBlank()) append(stdout.trimEnd())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(stderr.trimEnd())
            }
        }

    /** True when the command failed because `su` itself is unavailable. */
    val isSuMissing: Boolean get() = !isSuccess && (stderr.contains("su:") || stderr.contains("not found"))
}

/**
 * Thin, synchronous wrapper around `su -c <command>`.
 *
 * This is the only entry point that touches the device root; everything else
 * in the app goes through [exec].
 */
object RootShell {

    private const val DEFAULT_TIMEOUT_MS = 20_000L

    @Volatile
    private var cachedHasRoot: Boolean? = null

    /** Checks (and caches) whether a working `su` is available on this device. */
    fun hasRoot(): Boolean {
        cachedHasRoot?.let { return it }
        val result = exec("id", timeoutMs = 10_000L)
        val root = result.isSuccess && result.stdout.contains("uid=0")
        cachedHasRoot = root
        return root
    }

    /** Forgets the cached root state so the next check runs again. */
    fun invalidateCache() {
        cachedHasRoot = null
    }

    /** Runs [command] as root. Never throws; failures are reported in [ShellResult]. */
    fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult =
        run(arrayOf("su", "-c", command), timeoutMs)

    private fun run(cmd: Array<String>, timeoutMs: Long): ShellResult {
        return try {
            val process = ProcessBuilder(*cmd).start()
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outThread = thread(name = "rootshell-out", isDaemon = true) {
                process.inputStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        synchronized(stdout) { stdout.append(line).append('\n') }
                        line = reader.readLine()
                    }
                }
            }
            val errThread = thread(name = "rootshell-err", isDaemon = true) {
                process.errorStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        synchronized(stderr) { stderr.append(line).append('\n') }
                        line = reader.readLine()
                    }
                }
            }

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                outThread.join(1_000)
                errThread.join(1_000)
                return ShellResult(
                    -1,
                    synchronized(stdout) { stdout.toString() },
                    "Komut zaman aşımına uğradı",
                )
            }

            outThread.join(2_000)
            errThread.join(2_000)
            ShellResult(
                process.exitValue(),
                synchronized(stdout) { stdout.toString() },
                synchronized(stderr) { stderr.toString() },
            )
        } catch (e: IOException) {
            ShellResult(-1, "", "su çalıştırılamadı: ${e.message ?: "bilinmeyen hata"}")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            ShellResult(-1, "", "Komut kesildi")
        }
    }
}
