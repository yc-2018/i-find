package com.cgl.ifind.shizuku

import android.os.Bundle
import androidx.annotation.Keep
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Runs the small, allowlisted command set inside a Shizuku user-service process. */
@Keep
class ShizukuCommandService : IShizukuCommandService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun runCommand(command: String): Bundle {
        if (!ALLOWED_COMMAND.matches(command)) {
            return resultBundle(
                exitCode = EXIT_COMMAND_REJECTED,
                stderr = "Command rejected by the Shizuku command allowlist."
            )
        }

        val streamExecutor = Executors.newFixedThreadPool(2)
        var process: Process? = null

        return try {
            val runningProcess = ProcessBuilder("/system/bin/sh", "-c", command)
                .redirectErrorStream(false)
                .start()
            process = runningProcess

            val stdoutFuture = streamExecutor.submit<String> {
                runningProcess.inputStream.readUtf8Text()
            }
            val stderrFuture = streamExecutor.submit<String> {
                runningProcess.errorStream.readUtf8Text()
            }

            val finished = runningProcess.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                runningProcess.destroy()
                if (!runningProcess.waitFor(PROCESS_STOP_GRACE_MILLIS, TimeUnit.MILLISECONDS)) {
                    runningProcess.destroyForcibly()
                    runningProcess.waitFor(PROCESS_STOP_GRACE_MILLIS, TimeUnit.MILLISECONDS)
                }
            }

            resultBundle(
                exitCode = if (finished) runningProcess.exitValue() else EXIT_COMMAND_TIMED_OUT,
                stdout = stdoutFuture.readCompletedText(),
                stderr = stderrFuture.readCompletedText(),
                timedOut = !finished
            )
        } catch (error: Throwable) {
            resultBundle(
                exitCode = EXIT_COMMAND_FAILED,
                stderr = error.message ?: error.javaClass.simpleName
            )
        } finally {
            process?.destroy()
            streamExecutor.shutdownNow()
        }
    }

    private fun InputStream.readUtf8Text(): String =
        bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun Future<String>.readCompletedText(): String = try {
        get(STREAM_DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (_: Throwable) {
        ""
    }

    private fun resultBundle(
        exitCode: Int,
        stdout: String = "",
        stderr: String = "",
        timedOut: Boolean = false
    ): Bundle = Bundle().apply {
        putInt(KEY_EXIT_CODE, exitCode)
        putString(KEY_STDOUT, stdout)
        putString(KEY_STDERR, stderr)
        putBoolean(KEY_TIMED_OUT, timedOut)
    }

    companion object {
        const val KEY_EXIT_CODE = "exitCode"
        const val KEY_STDOUT = "stdout"
        const val KEY_STDERR = "stderr"
        const val KEY_TIMED_OUT = "timedOut"

        private const val EXIT_COMMAND_FAILED = -1
        private const val EXIT_COMMAND_TIMED_OUT = -2
        private const val EXIT_COMMAND_REJECTED = -126
        private const val COMMAND_TIMEOUT_SECONDS = 8L
        private const val STREAM_DRAIN_TIMEOUT_SECONDS = 2L
        private const val PROCESS_STOP_GRACE_MILLIS = 500L

        private val ALLOWED_COMMAND = Regex(
            "^(?:cmd package unsuspend|pm unsuspend|pm unhide|pm enable) " +
                "--user current [A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$"
        )
    }
}
