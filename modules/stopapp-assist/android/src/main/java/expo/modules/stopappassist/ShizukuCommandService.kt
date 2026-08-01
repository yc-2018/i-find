package expo.modules.stopappassist

import android.os.Bundle
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuCommandService : IShizukuCommandService.Stub() {
  override fun runCommand(command: String): Bundle {
    val stdout = StringBuilder()
    val stderr = StringBuilder()

    val process = ProcessBuilder("/system/bin/sh", "-c", command)
      .redirectErrorStream(false)
      .start()

    BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
      lines.forEach { line ->
        if (stdout.isNotEmpty()) {
          stdout.append('\n')
        }
        stdout.append(line)
      }
    }

    BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
      lines.forEach { line ->
        if (stderr.isNotEmpty()) {
          stderr.append('\n')
        }
        stderr.append(line)
      }
    }

    val exitCode = process.waitFor()

    return Bundle().apply {
      putInt("exitCode", exitCode)
      putString("stdout", stdout.toString())
      putString("stderr", stderr.toString())
    }
  }
}
