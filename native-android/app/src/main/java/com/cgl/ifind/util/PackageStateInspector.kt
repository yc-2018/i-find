package com.cgl.ifind.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

enum class PackageReadiness {
  AVAILABLE,
  NEEDS_DEFROST,
  NOT_INSTALLED
}

data class PackageInspection(
  val readiness: PackageReadiness,
  val hasLaunchIntent: Boolean
)

object PackageStateInspector {
  fun inspect(context: Context, packageName: String): PackageInspection {
    val packageManager = context.packageManager
    val normalizedPackageName = packageName.trim()
    val launchIntentAvailable = runCatching {
      packageManager.getLaunchIntentForPackage(normalizedPackageName) != null
    }.getOrDefault(false)

    val installedInfo = getApplicationInfo(
      packageManager,
      normalizedPackageName,
      PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
    )
    if (installedInfo != null) {
      return PackageInspection(
        readiness = readinessFor(packageManager, normalizedPackageName, installedInfo),
        hasLaunchIntent = launchIntentAvailable
      )
    }

    // Device-owner freezing can hide a package from ordinary lookups while leaving
    // its package record available through MATCH_UNINSTALLED_PACKAGES.
    val hiddenInfo = getApplicationInfo(
      packageManager,
      normalizedPackageName,
      PackageManager.MATCH_DISABLED_COMPONENTS.toLong() or
        PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
    )
    return PackageInspection(
      readiness = if (hiddenInfo == null) {
        PackageReadiness.NOT_INSTALLED
      } else {
        PackageReadiness.NEEDS_DEFROST
      },
      hasLaunchIntent = launchIntentAvailable
    )
  }

  private fun readinessFor(
    packageManager: PackageManager,
    packageName: String,
    applicationInfo: ApplicationInfo
  ): PackageReadiness {
    val enabledSetting = runCatching {
      packageManager.getApplicationEnabledSetting(packageName)
    }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    val explicitlyDisabled = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
      enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
      enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
    val suspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      runCatching { packageManager.isPackageSuspended(packageName) }.getOrDefault(false)
    } else {
      false
    }
    val installedForCurrentUser = applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED != 0

    return if (!applicationInfo.enabled || explicitlyDisabled || suspended || !installedForCurrentUser) {
      PackageReadiness.NEEDS_DEFROST
    } else {
      PackageReadiness.AVAILABLE
    }
  }

  @Suppress("DEPRECATION")
  private fun getApplicationInfo(
    packageManager: PackageManager,
    packageName: String,
    flags: Long
  ): ApplicationInfo? {
    return runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(
          packageName,
          PackageManager.ApplicationInfoFlags.of(flags)
        )
      } else {
        packageManager.getApplicationInfo(packageName, flags.toInt())
      }
    }.getOrNull()
  }
}
