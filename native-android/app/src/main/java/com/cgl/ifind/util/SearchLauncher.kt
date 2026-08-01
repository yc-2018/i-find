package com.cgl.ifind.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Browser
import android.widget.Toast
import com.cgl.ifind.R
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.shizuku.ShizukuBridge

object SearchLauncher {
  private const val KEYWORD_PLACEHOLDER = "{keyword}"
  private const val POST_DEFROST_DELAY_MS = 80L

  fun launch(
    activity: Activity,
    target: SearchTarget,
    keyword: String,
    autoDefrostEnabled: Boolean
  ) {
    val packageName = target.androidPackageName
    val primaryUrl = buildSearchUrl(target.primaryTemplate, keyword)
    val fallbackUrl = target.fallbackTemplate?.let { buildSearchUrl(it, keyword) }
    val shouldInspectPackage = autoDefrostEnabled &&
      !packageName.isNullOrBlank() &&
      !isWebUrl(primaryUrl)

    if (!shouldInspectPackage) {
      openTarget(activity, primaryUrl, fallbackUrl)
      return
    }

    val inspection = PackageStateInspector.inspect(activity, packageName.orEmpty())
    if (inspection.readiness != PackageReadiness.NEEDS_DEFROST) {
      openTarget(activity, primaryUrl, fallbackUrl)
      return
    }

    Toast.makeText(
      activity,
      activity.getString(R.string.defrosting_and_opening, target.name),
      Toast.LENGTH_SHORT
    ).show()

    val bridge = ShizukuBridge(activity.applicationContext)
    bridge.attemptDefrost(packageName.orEmpty()) {
      bridge.close()
      Handler(Looper.getMainLooper()).postDelayed(
        {
          if (!activity.isFinishing && !activity.isDestroyed) {
            openTarget(activity, primaryUrl, fallbackUrl)
          }
        },
        POST_DEFROST_DELAY_MS
      )
    }
  }

  private fun openTarget(activity: Activity, primaryUrl: String, fallbackUrl: String?) {
    if (openUrl(activity, primaryUrl, isWeb = isWebUrl(primaryUrl))) {
      return
    }
    if (fallbackUrl != null && openUrl(activity, fallbackUrl, isWeb = isWebUrl(fallbackUrl))) {
      return
    }

    Toast.makeText(
      activity,
      if (fallbackUrl != null) R.string.web_open_failed else R.string.open_failed,
      Toast.LENGTH_SHORT
    ).show()
  }

  private fun buildSearchUrl(template: String, keyword: String): String {
    return template.replace(KEYWORD_PLACEHOLDER, Uri.encode(keyword))
  }

  private fun isWebUrl(url: String): Boolean {
    return when (Uri.parse(url).scheme?.lowercase()) {
      "http", "https" -> true
      else -> false
    }
  }

  private fun openUrl(activity: Activity, url: String, isWeb: Boolean): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      if (isWeb) putExtra(Browser.EXTRA_APPLICATION_ID, activity.packageName)
    }
    return try {
      activity.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: SecurityException) {
      false
    } catch (_: IllegalArgumentException) {
      false
    }
  }
}
