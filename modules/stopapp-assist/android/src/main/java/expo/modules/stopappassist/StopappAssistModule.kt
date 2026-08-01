package expo.modules.stopappassist

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.pm.PackageInfoCompat
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StopappAssistModule : Module() {
  private var pendingPermissionPromise: Promise? = null

  override fun definition() = ModuleDefinition {
    Name("StopappAssist")

    AsyncFunction("getStatusAsync") {
      return@AsyncFunction getStatusPayload()
    }

    AsyncFunction("attemptDefrostAsync") { targetPackageName: String ->
      return@AsyncFunction attemptDefrostPayload(targetPackageName)
    }

    AsyncFunction("openManagerAsync") {
      return@AsyncFunction openManagerPayload()
    }

    AsyncFunction("requestPermissionAsync") { promise: Promise ->
      requestPermissionPayload(promise)
    }
  }

  private fun getStatusPayload(): Map<String, Any?> {
    val packageInfo = getShizukuPackageInfo()
    val binderAlive = isShizukuBinderAlive()
    val permissionGranted = binderAlive && isPermissionGranted()
    val launchIntent = requirePackageManager().getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
    val backendMode = resolveBackendMode(binderAlive)

    val reason: String
    val message: String
    val availability: String
    val prerequisites = mutableListOf<String>()

    when {
      !binderAlive && packageInfo == null -> {
        availability = "not_installed"
        reason = "shizuku_not_installed"
        message = "Shizuku is not installed on this device."
        prerequisites += "install_shizuku"
      }
      !binderAlive -> {
        availability = "prerequisites_missing"
        reason = "shizuku_service_not_running"
        message = "Shizuku is installed, but its service is not running."
        prerequisites += "start_shizuku_service"
      }
      !permissionGranted -> {
        availability = "prerequisites_missing"
        reason =
          if (Shizuku.shouldShowRequestPermissionRationale()) {
            "shizuku_permission_denied"
          } else {
            "shizuku_permission_not_granted"
          }
        message = "Shizuku service is running, but this app does not have permission yet."
        prerequisites += "grant_shizuku_permission"
      }
      else -> {
        availability = "ready"
        reason = "shizuku_ready"
        message = "Shizuku is ready for direct defrost commands."
      }
    }

    return mapOf(
      "packageName" to SHIZUKU_PACKAGE_NAME,
      "availability" to availability,
      "nativeModuleAvailable" to true,
      "installed" to (packageInfo != null),
      "launchable" to (launchIntent != null),
      "versionName" to packageInfo?.versionName,
      "versionCode" to packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) },
      "publicDefrostSupported" to permissionGranted,
      "permissionGranted" to permissionGranted,
      "serviceRunning" to binderAlive,
      "backendMode" to backendMode,
      "reason" to reason,
      "message" to message,
      "requiredPrerequisites" to prerequisites,
      "exportedActivityCount" to (packageInfo?.activities?.count { it.exported } ?: 0),
      "exportedServiceCount" to (packageInfo?.services?.count { it.exported } ?: 0),
      "exportedReceiverCount" to (packageInfo?.receivers?.count { it.exported } ?: 0),
      "exportedProviderCount" to (packageInfo?.providers?.count { it.exported } ?: 0)
    )
  }

  private fun attemptDefrostPayload(targetPackageName: String): Map<String, Any?> {
    val normalizedPackageName = targetPackageName.trim()

    if (normalizedPackageName.isEmpty()) {
      return mapOf(
        "attempted" to false,
        "supported" to false,
        "success" to false,
        "code" to "invalid_target_package",
        "message" to "Target package name is empty.",
        "targetPackageName" to normalizedPackageName,
        "attemptedCommands" to emptyList<String>(),
        "successfulCommands" to emptyList<String>(),
        "stdout" to "",
        "stderr" to ""
      )
    }

    if (!isShizukuBinderAlive()) {
      return mapOf(
        "attempted" to false,
        "supported" to false,
        "success" to false,
        "code" to "shizuku_service_not_running",
        "message" to "Shizuku service is not running.",
        "targetPackageName" to normalizedPackageName,
        "attemptedCommands" to emptyList<String>(),
        "successfulCommands" to emptyList<String>(),
        "stdout" to "",
        "stderr" to ""
      )
    }

    if (!isPermissionGranted()) {
      return mapOf(
        "attempted" to false,
        "supported" to false,
        "success" to false,
        "code" to "shizuku_permission_not_granted",
        "message" to "Shizuku permission has not been granted to this app.",
        "targetPackageName" to normalizedPackageName,
        "attemptedCommands" to emptyList<String>(),
        "successfulCommands" to emptyList<String>(),
        "stdout" to "",
        "stderr" to ""
      )
    }

    val commandChain = listOf(
      "cmd package unsuspend --user current $normalizedPackageName",
      "pm unsuspend --user current $normalizedPackageName",
      "pm unhide --user current $normalizedPackageName",
      "pm enable --user current $normalizedPackageName"
    )

    val attemptedCommands = mutableListOf<String>()
    val successfulCommands = mutableListOf<String>()
    val stdoutParts = mutableListOf<String>()
    val stderrParts = mutableListOf<String>()

    val serviceResult = withUserService { service ->
      commandChain.forEach { command ->
        attemptedCommands += command
        val result = service.runCommand(command)
        val exitCode = result.getInt("exitCode", -1)
        val stdout = result.getString("stdout").orEmpty()
        val stderr = result.getString("stderr").orEmpty()

        if (stdout.isNotBlank()) {
          stdoutParts += "$command\n$stdout"
        }

        if (stderr.isNotBlank()) {
          stderrParts += "$command\n$stderr"
        }

        if (exitCode == 0) {
          successfulCommands += command
        }
      }

      true
    }

    if (!serviceResult) {
      return mapOf(
        "attempted" to false,
        "supported" to true,
        "success" to false,
        "code" to "shizuku_user_service_bind_failed",
        "message" to "Failed to bind the Shizuku user service.",
        "targetPackageName" to normalizedPackageName,
        "attemptedCommands" to attemptedCommands,
        "successfulCommands" to successfulCommands,
        "stdout" to stdoutParts.joinToString("\n\n"),
        "stderr" to stderrParts.joinToString("\n\n")
      )
    }

    return mapOf(
      "attempted" to attemptedCommands.isNotEmpty(),
      "supported" to true,
      "success" to successfulCommands.isNotEmpty(),
      "code" to if (successfulCommands.isNotEmpty()) "defrost_commands_applied" else "defrost_commands_failed",
      "message" to
        if (successfulCommands.isNotEmpty()) {
          "Direct defrost commands were applied."
        } else {
          "No direct defrost command succeeded for this package."
        },
      "targetPackageName" to normalizedPackageName,
      "attemptedCommands" to attemptedCommands,
      "successfulCommands" to successfulCommands,
      "stdout" to stdoutParts.joinToString("\n\n"),
      "stderr" to stderrParts.joinToString("\n\n")
    )
  }

  private fun openManagerPayload(): Map<String, Any?> {
    val packageManager = requirePackageManager()
    val reactContext = appContext.reactContext
      ?: return mapOf(
        "opened" to false,
        "code" to "react_context_unavailable",
        "message" to "React context is unavailable."
      )

    val launchIntent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
      ?: return mapOf(
        "opened" to false,
        "code" to "manager_not_launchable",
        "message" to "Shizuku is not launchable on this device."
      )

    return try {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      reactContext.startActivity(launchIntent)
      mapOf(
        "opened" to true,
        "code" to "opened",
        "message" to "Shizuku launch intent sent."
      )
    } catch (error: Exception) {
      mapOf(
        "opened" to false,
        "code" to "open_failed",
        "message" to (error.message ?: "Failed to open Shizuku.")
      )
    }
  }

  private fun requestPermissionPayload(promise: Promise) {
    if (!isShizukuBinderAlive()) {
      promise.resolve(
        mapOf(
          "granted" to false,
          "code" to "shizuku_service_not_running",
          "message" to "Shizuku service is not running.",
          "shouldShowRequestPermissionRationale" to false
        )
      )
      return
    }

    if (isPermissionGranted()) {
      promise.resolve(
        mapOf(
          "granted" to true,
          "code" to "already_granted",
          "message" to "Shizuku permission is already granted.",
          "shouldShowRequestPermissionRationale" to false
        )
      )
      return
    }

    if (pendingPermissionPromise != null) {
      promise.resolve(
        mapOf(
          "granted" to false,
          "code" to "permission_request_in_progress",
          "message" to "A Shizuku permission request is already in progress.",
          "shouldShowRequestPermissionRationale" to Shizuku.shouldShowRequestPermissionRationale()
        )
      )
      return
    }

    pendingPermissionPromise = promise

    val listener = object : Shizuku.OnRequestPermissionResultListener {
      override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
          return
        }

        Shizuku.removeRequestPermissionResultListener(this)

        pendingPermissionPromise?.resolve(
          mapOf(
            "granted" to (grantResult == PackageManager.PERMISSION_GRANTED),
            "code" to
              if (grantResult == PackageManager.PERMISSION_GRANTED) {
                "granted"
              } else {
                "denied"
              },
            "message" to
              if (grantResult == PackageManager.PERMISSION_GRANTED) {
                "Shizuku permission granted."
              } else {
                "Shizuku permission denied."
              },
            "shouldShowRequestPermissionRationale" to Shizuku.shouldShowRequestPermissionRationale()
          )
        )
        pendingPermissionPromise = null
      }
    }

    Shizuku.addRequestPermissionResultListener(listener)
    Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
  }

  private fun withUserService(block: (IShizukuCommandService) -> Boolean): Boolean {
    val reactContext = appContext.reactContext ?: return false
    val latch = CountDownLatch(1)
    var binderInterface: IShizukuCommandService? = null

    val serviceConnection = object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binderInterface = IShizukuCommandService.Stub.asInterface(service)
        latch.countDown()
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        binderInterface = null
      }
    }

    val userServiceArgs = Shizuku.UserServiceArgs(
      ComponentName(reactContext.packageName, ShizukuCommandService::class.java.name)
    )
      .daemon(false)
      .processNameSuffix("ifind_shizuku")
      .tag("ifind_shizuku_service")
      .version(1)

    return try {
      Shizuku.bindUserService(userServiceArgs, serviceConnection)
      if (!latch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        false
      } else {
        binderInterface?.let(block) == true
      }
    } catch (_: Exception) {
      false
    } finally {
      try {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
      } catch (_: Exception) {
      }
    }
  }

  private fun getShizukuPackageInfo(): PackageInfo? {
    return try {
      getPackageInfo(SHIZUKU_PACKAGE_NAME, PACKAGE_INFO_FLAGS)
    } catch (_: PackageManager.NameNotFoundException) {
      null
    }
  }

  private fun isShizukuBinderAlive(): Boolean {
    return try {
      !Shizuku.isPreV11() && Shizuku.pingBinder()
    } catch (_: Throwable) {
      false
    }
  }

  private fun isPermissionGranted(): Boolean {
    return try {
      Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
      false
    }
  }

  private fun resolveBackendMode(binderAlive: Boolean): String {
    if (!binderAlive) {
      return "unavailable"
    }

    return when (Shizuku.getUid()) {
      0 -> "root"
      2000 -> "adb"
      else -> "unknown"
    }
  }

  private fun requirePackageManager(): PackageManager {
    return appContext.reactContext?.packageManager
      ?: throw IllegalStateException("React context is unavailable.")
  }

  @Suppress("DEPRECATION")
  private fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
    val packageManager = requirePackageManager()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(flags.toLong())
      )
    } else {
      packageManager.getPackageInfo(packageName, flags)
    }
  }

  companion object {
    private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST_CODE = 49526
    private const val BIND_TIMEOUT_SECONDS = 6L
    private const val PACKAGE_INFO_FLAGS =
      PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS
  }
}
