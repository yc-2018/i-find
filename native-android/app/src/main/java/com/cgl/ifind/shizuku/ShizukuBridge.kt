package com.cgl.ifind.shizuku

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import rikka.shizuku.Shizuku
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

enum class ShizukuState {
    NOT_INSTALLED,
    SERVICE_NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY
}

enum class ShizukuBackendMode {
    UNAVAILABLE,
    ROOT,
    ADB,
    UNKNOWN
}

data class ShizukuStatus(
    val state: ShizukuState,
    val installed: Boolean,
    val serviceRunning: Boolean,
    val permissionGranted: Boolean,
    val shouldShowPermissionRationale: Boolean,
    val backendMode: ShizukuBackendMode,
    val versionName: String?,
    val versionCode: Long?
)

data class ShizukuPermissionResult(
    val granted: Boolean,
    val code: String,
    val shouldShowPermissionRationale: Boolean
)

data class ShizukuCommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean
)

data class ShizukuDefrostResult(
    val attempted: Boolean,
    val supported: Boolean,
    val success: Boolean,
    val code: String,
    val message: String,
    val targetPackageName: String,
    val attemptedCommands: List<String> = emptyList(),
    val successfulCommands: List<String> = emptyList(),
    val commandResults: List<ShizukuCommandResult> = emptyList(),
    val stdout: String = "",
    val stderr: String = ""
)

/**
 * Native facade for Shizuku status, permission and direct package defrost commands.
 *
 * Call [onStart] and [onStop] from the matching Activity lifecycle methods, then
 * call [close] from Activity.onDestroy(). Defrost callbacks are always delivered
 * on the main thread; binding and shell commands always run on a worker thread.
 */
class ShizukuBridge(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ifind-shizuku-worker").apply { isDaemon = true }
    }

    private val lifecycleLock = Any()
    private val permissionLock = Any()

    @Volatile
    private var currentStatus = resolveStatus()

    @Volatile
    private var closed = false

    private var listenersRegistered = false
    private var statusCallback: ((ShizukuStatus) -> Unit)? = null
    private var pendingPermission: PendingPermission? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refresh()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        refresh()
    }

    /** Register binder listeners. Pair this with Activity.onStart(). */
    fun onStart(callback: (ShizukuStatus) -> Unit) {
        if (closed) {
            dispatchMain { callback(currentStatus) }
            return
        }

        synchronized(lifecycleLock) {
            statusCallback = callback
            if (!listenersRegistered) {
                registerBinderListenersLocked()
            }
        }

        dispatchStatus(refreshInternal(notify = false))
    }

    /** Remove binder listeners and the UI callback. Pair this with Activity.onStop(). */
    fun onStop() {
        synchronized(lifecycleLock) {
            statusCallback = null
            unregisterBinderListenersLocked()
        }
    }

    /** Recompute status immediately and notify the active status callback on changes. */
    fun refresh(): ShizukuStatus = refreshInternal(notify = true)

    /** Return the latest cached status snapshot. */
    fun getStatus(): ShizukuStatus = currentStatus

    /** Request Shizuku permission. The result callback is delivered on the main thread. */
    fun requestPermission(
        activity: Activity,
        callback: (ShizukuPermissionResult) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { requestPermission(activity, callback) }
            return
        }

        if (closed) {
            callback(permissionResult(false, CODE_BRIDGE_CLOSED))
            return
        }

        if (isActivityUnavailable(activity)) {
            callback(permissionResult(false, CODE_ACTIVITY_UNAVAILABLE))
            return
        }

        when (refreshInternal(notify = true).state) {
            ShizukuState.NOT_INSTALLED -> {
                callback(permissionResult(false, CODE_NOT_INSTALLED))
                return
            }
            ShizukuState.SERVICE_NOT_RUNNING -> {
                callback(permissionResult(false, CODE_SERVICE_NOT_RUNNING))
                return
            }
            ShizukuState.READY -> {
                callback(permissionResult(true, CODE_ALREADY_GRANTED))
                return
            }
            ShizukuState.PERMISSION_REQUIRED -> Unit
        }

        synchronized(permissionLock) {
            if (pendingPermission != null) {
                callback(permissionResult(false, CODE_REQUEST_IN_PROGRESS))
                return
            }

            lateinit var listener: Shizuku.OnRequestPermissionResultListener
            listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == PERMISSION_REQUEST_CODE) {
                    completePermissionRequest(listener, grantResult)
                }
            }

            pendingPermission = PendingPermission(
                activity = WeakReference(activity),
                callback = callback,
                listener = listener
            )

            try {
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            } catch (_: Throwable) {
                pendingPermission = null
                safelyRemovePermissionListener(listener)
                callback(permissionResult(false, CODE_REQUEST_FAILED))
            }
        }
    }

    /** Open the Shizuku manager app if it is installed and launchable. */
    fun openShizuku(context: Context): Boolean {
        val launchIntent = try {
            context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
        } catch (_: Throwable) {
            null
        } ?: return false

        if (context !is Activity) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Attempt to unsuspend, unhide and enable [packageName]. Work runs off the main
     * thread and [callback] is always posted back to the main thread.
     */
    fun attemptDefrost(
        packageName: String,
        callback: (ShizukuDefrostResult) -> Unit
    ) {
        val normalizedPackageName = packageName.trim()
        if (!isValidAndroidPackageName(normalizedPackageName)) {
            dispatchMain {
                callback(
                    failureResult(
                        code = CODE_INVALID_TARGET,
                        message = "Invalid Android package name.",
                        targetPackageName = normalizedPackageName
                    )
                )
            }
            return
        }

        if (closed) {
            dispatchMain {
                callback(
                    failureResult(
                        code = CODE_BRIDGE_CLOSED,
                        message = "Shizuku bridge is closed.",
                        targetPackageName = normalizedPackageName
                    )
                )
            }
            return
        }

        try {
            worker.execute {
                val result = attemptDefrostOnWorker(normalizedPackageName)
                dispatchMain {
                    if (!closed) callback(result)
                }
            }
        } catch (_: RejectedExecutionException) {
            dispatchMain {
                callback(
                    failureResult(
                        code = CODE_BRIDGE_CLOSED,
                        message = "Shizuku bridge is closed.",
                        targetPackageName = normalizedPackageName
                    )
                )
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        onStop()

        val permission = synchronized(permissionLock) {
            pendingPermission.also { pendingPermission = null }
        }
        permission?.let { safelyRemovePermissionListener(it.listener) }

        worker.shutdownNow()
    }

    private fun attemptDefrostOnWorker(packageName: String): ShizukuDefrostResult {
        when (refreshInternal(notify = true).state) {
            ShizukuState.NOT_INSTALLED -> return failureResult(
                code = CODE_NOT_INSTALLED,
                message = "Shizuku is not installed.",
                targetPackageName = packageName
            )
            ShizukuState.SERVICE_NOT_RUNNING -> return failureResult(
                code = CODE_SERVICE_NOT_RUNNING,
                message = "Shizuku service is not running.",
                targetPackageName = packageName
            )
            ShizukuState.PERMISSION_REQUIRED -> return failureResult(
                code = CODE_PERMISSION_REQUIRED,
                message = "Shizuku permission is required.",
                targetPackageName = packageName
            )
            ShizukuState.READY -> Unit
        }

        val commands = buildCommandChain(packageName)
        return when (val serviceCall = withUserService { service -> runCommandChain(service, commands) }) {
            is UserServiceCall.Failure -> failureResult(
                attempted = false,
                supported = true,
                code = CODE_BIND_FAILED,
                message = serviceCall.message,
                targetPackageName = packageName
            )
            is UserServiceCall.Success -> buildDefrostResult(packageName, serviceCall.value)
        }
    }

    private fun runCommandChain(
        service: IShizukuCommandService,
        commands: List<String>
    ): List<ShizukuCommandResult> {
        val results = mutableListOf<ShizukuCommandResult>()

        for (command in commands) {
            val commandResult = try {
                val bundle = service.runCommand(command)
                ShizukuCommandResult(
                    command = command,
                    exitCode = bundle.getInt(ShizukuCommandService.KEY_EXIT_CODE, -1),
                    stdout = bundle.getString(ShizukuCommandService.KEY_STDOUT).orEmpty(),
                    stderr = bundle.getString(ShizukuCommandService.KEY_STDERR).orEmpty(),
                    timedOut = bundle.getBoolean(ShizukuCommandService.KEY_TIMED_OUT, false)
                )
            } catch (error: Throwable) {
                ShizukuCommandResult(
                    command = command,
                    exitCode = -1,
                    stdout = "",
                    stderr = error.message ?: error.javaClass.simpleName,
                    timedOut = false
                )
            }

            results += commandResult
            if (commandResult.timedOut || !service.asBinder().isBinderAlive) break
        }

        return results
    }

    private fun buildDefrostResult(
        packageName: String,
        commandResults: List<ShizukuCommandResult>
    ): ShizukuDefrostResult {
        val attemptedCommands = commandResults.map { it.command }
        val successfulCommands = commandResults.filter { it.exitCode == 0 }.map { it.command }
        val stdout = commandResults
            .filter { it.stdout.isNotBlank() }
            .joinToString("\n\n") { "${it.command}\n${it.stdout}" }
        val stderr = commandResults
            .filter { it.stderr.isNotBlank() }
            .joinToString("\n\n") { "${it.command}\n${it.stderr}" }
        val success = successfulCommands.isNotEmpty()

        return ShizukuDefrostResult(
            attempted = attemptedCommands.isNotEmpty(),
            supported = true,
            success = success,
            code = if (success) CODE_COMMANDS_APPLIED else CODE_COMMANDS_FAILED,
            message = if (success) {
                "At least one direct defrost command succeeded."
            } else {
                "No direct defrost command succeeded."
            },
            targetPackageName = packageName,
            attemptedCommands = attemptedCommands,
            successfulCommands = successfulCommands,
            commandResults = commandResults,
            stdout = stdout,
            stderr = stderr
        )
    }

    private fun <T> withUserService(block: (IShizukuCommandService) -> T): UserServiceCall<T> {
        val latch = CountDownLatch(1)
        val serviceReference = AtomicReference<IShizukuCommandService?>()

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                serviceReference.set(IShizukuCommandService.Stub.asInterface(binder))
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceReference.set(null)
                latch.countDown()
            }

            override fun onBindingDied(name: ComponentName?) {
                serviceReference.set(null)
                latch.countDown()
            }

            override fun onNullBinding(name: ComponentName?) {
                serviceReference.set(null)
                latch.countDown()
            }
        }

        val args = Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, ShizukuCommandService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix(USER_SERVICE_PROCESS_SUFFIX)
            .tag(USER_SERVICE_TAG)
            .version(USER_SERVICE_VERSION)

        return try {
            Shizuku.bindUserService(args, connection)
            if (!latch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                UserServiceCall.Failure("Timed out while binding the Shizuku user service.")
            } else {
                val service = serviceReference.get()
                    ?: return UserServiceCall.Failure("Shizuku user service returned no binder.")
                UserServiceCall.Success(block(service))
            }
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            UserServiceCall.Failure("Shizuku user service binding was interrupted.")
        } catch (error: Throwable) {
            UserServiceCall.Failure(error.message ?: "Failed to bind the Shizuku user service.")
        } finally {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Throwable) {
                // The service may already be gone after binder death or a bind timeout.
            }
        }
    }

    private fun completePermissionRequest(
        listener: Shizuku.OnRequestPermissionResultListener,
        grantResult: Int
    ) {
        val pending = synchronized(permissionLock) {
            val current = pendingPermission ?: return
            if (current.listener !== listener) return
            pendingPermission = null
            current
        }

        safelyRemovePermissionListener(listener)
        refreshInternal(notify = true)

        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        dispatchMain {
            if (!closed && !isActivityUnavailable(pending.activity.get())) {
                pending.callback(
                    permissionResult(
                        granted = granted,
                        code = if (granted) CODE_GRANTED else CODE_DENIED
                    )
                )
            }
        }
    }

    private fun permissionResult(granted: Boolean, code: String): ShizukuPermissionResult =
        ShizukuPermissionResult(
            granted = granted,
            code = code,
            shouldShowPermissionRationale = shouldShowPermissionRationale()
        )

    private fun safelyRemovePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (_: Throwable) {
            // Shizuku can disappear while a permission request is active.
        }
    }

    private fun registerBinderListenersLocked() {
        var receivedListenerAdded = false
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            receivedListenerAdded = true
            Shizuku.addBinderDeadListener(binderDeadListener)
            listenersRegistered = true
        } catch (_: Throwable) {
            if (receivedListenerAdded) {
                try {
                    Shizuku.removeBinderReceivedListener(binderReceivedListener)
                } catch (_: Throwable) {
                }
            }
            listenersRegistered = false
        }
    }

    private fun unregisterBinderListenersLocked() {
        if (!listenersRegistered) return

        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
        } catch (_: Throwable) {
        }
        try {
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (_: Throwable) {
        }
        listenersRegistered = false
    }

    private fun refreshInternal(notify: Boolean): ShizukuStatus {
        val refreshed = resolveStatus()
        val changed = refreshed != currentStatus
        currentStatus = refreshed
        if (notify && changed) dispatchStatus(refreshed)
        return refreshed
    }

    private fun resolveStatus(): ShizukuStatus {
        val packageInfo = getShizukuPackageInfo()
        val binderAlive = isShizukuBinderAlive()
        val permissionGranted = binderAlive && isPermissionGranted()

        val state = when {
            !binderAlive && packageInfo == null -> ShizukuState.NOT_INSTALLED
            !binderAlive -> ShizukuState.SERVICE_NOT_RUNNING
            !permissionGranted -> ShizukuState.PERMISSION_REQUIRED
            else -> ShizukuState.READY
        }

        return ShizukuStatus(
            state = state,
            installed = packageInfo != null,
            serviceRunning = binderAlive,
            permissionGranted = permissionGranted,
            shouldShowPermissionRationale = binderAlive && !permissionGranted && shouldShowPermissionRationale(),
            backendMode = resolveBackendMode(binderAlive),
            versionName = packageInfo?.versionName,
            versionCode = packageInfo?.let(::getLongVersionCode)
        )
    }

    private fun dispatchStatus(status: ShizukuStatus) {
        dispatchMain {
            val callback = synchronized(lifecycleLock) { statusCallback }
            callback?.invoke(status)
        }
    }

    private fun dispatchMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    @Suppress("DEPRECATION")
    private fun getShizukuPackageInfo(): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.packageManager.getPackageInfo(
                SHIZUKU_PACKAGE_NAME,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: Throwable) {
        null
    }

    @Suppress("DEPRECATION")
    private fun getLongVersionCode(packageInfo: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

    private fun isShizukuBinderAlive(): Boolean = try {
        !Shizuku.isPreV11() && Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun isPermissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    private fun shouldShowPermissionRationale(): Boolean = try {
        Shizuku.shouldShowRequestPermissionRationale()
    } catch (_: Throwable) {
        false
    }

    private fun resolveBackendMode(binderAlive: Boolean): ShizukuBackendMode {
        if (!binderAlive) return ShizukuBackendMode.UNAVAILABLE
        return try {
            when (Shizuku.getUid()) {
                0 -> ShizukuBackendMode.ROOT
                2000 -> ShizukuBackendMode.ADB
                else -> ShizukuBackendMode.UNKNOWN
            }
        } catch (_: Throwable) {
            ShizukuBackendMode.UNKNOWN
        }
    }

    private fun buildCommandChain(packageName: String): List<String> = listOf(
        "cmd package unsuspend --user current $packageName",
        "pm unsuspend --user current $packageName",
        "pm unhide --user current $packageName",
        "pm enable --user current $packageName"
    )

    private fun isValidAndroidPackageName(packageName: String): Boolean =
        packageName.length <= MAX_PACKAGE_NAME_LENGTH && PACKAGE_NAME_REGEX.matches(packageName)

    private fun isActivityUnavailable(activity: Activity?): Boolean =
        activity == null || activity.isFinishing || activity.isDestroyed

    private fun failureResult(
        code: String,
        message: String,
        targetPackageName: String,
        attempted: Boolean = false,
        supported: Boolean = false
    ): ShizukuDefrostResult = ShizukuDefrostResult(
        attempted = attempted,
        supported = supported,
        success = false,
        code = code,
        message = message,
        targetPackageName = targetPackageName
    )

    private data class PendingPermission(
        val activity: WeakReference<Activity>,
        val callback: (ShizukuPermissionResult) -> Unit,
        val listener: Shizuku.OnRequestPermissionResultListener
    )

    private sealed interface UserServiceCall<out T> {
        data class Success<T>(val value: T) : UserServiceCall<T>
        data class Failure(val message: String) : UserServiceCall<Nothing>
    }

    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

        private const val PERMISSION_REQUEST_CODE = 49526
        private const val BIND_TIMEOUT_SECONDS = 6L
        private const val USER_SERVICE_PROCESS_SUFFIX = "ifind_shizuku"
        private const val USER_SERVICE_TAG = "ifind_shizuku_service"
        private const val USER_SERVICE_VERSION = 1
        private const val MAX_PACKAGE_NAME_LENGTH = 255

        private const val CODE_NOT_INSTALLED = "shizuku_not_installed"
        private const val CODE_SERVICE_NOT_RUNNING = "shizuku_service_not_running"
        private const val CODE_PERMISSION_REQUIRED = "shizuku_permission_not_granted"
        private const val CODE_ALREADY_GRANTED = "already_granted"
        private const val CODE_GRANTED = "granted"
        private const val CODE_DENIED = "denied"
        private const val CODE_REQUEST_IN_PROGRESS = "permission_request_in_progress"
        private const val CODE_REQUEST_FAILED = "permission_request_failed"
        private const val CODE_ACTIVITY_UNAVAILABLE = "activity_unavailable"
        private const val CODE_INVALID_TARGET = "invalid_target_package"
        private const val CODE_BIND_FAILED = "shizuku_user_service_bind_failed"
        private const val CODE_COMMANDS_APPLIED = "defrost_commands_applied"
        private const val CODE_COMMANDS_FAILED = "defrost_commands_failed"
        private const val CODE_BRIDGE_CLOSED = "bridge_closed"

        private val PACKAGE_NAME_REGEX = Regex(
            "^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$"
        )
    }
}
