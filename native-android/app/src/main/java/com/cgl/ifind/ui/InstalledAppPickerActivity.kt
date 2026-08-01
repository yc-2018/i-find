package com.cgl.ifind.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.cgl.ifind.data.InstalledAppInfo
import com.cgl.ifind.databinding.ActivityInstalledAppPickerBinding
import java.text.Collator
import java.util.Locale
import java.util.concurrent.Executors

class InstalledAppPickerActivity : AppCompatActivity() {
  private lateinit var binding: ActivityInstalledAppPickerBinding
  private lateinit var appsAdapter: InstalledAppsAdapter
  private val executor = Executors.newSingleThreadExecutor()
  private var allApps: List<InstalledAppInfo> = emptyList()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityInstalledAppPickerBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applySystemBarInsets(binding.root)

    appsAdapter = InstalledAppsAdapter(::selectApp)
    binding.appList.layoutManager = LinearLayoutManager(this)
    binding.appList.adapter = appsAdapter
    binding.appList.itemAnimator = null
    binding.backButton.setOnClickListener { finish() }
    binding.filterInput.doAfterTextChanged { filterApps(it?.toString().orEmpty()) }

    loadApps()
  }

  override fun onDestroy() {
    executor.shutdownNow()
    super.onDestroy()
  }

  private fun loadApps() {
    binding.loadingIndicator.isVisible = true
    binding.emptyState.isVisible = false
    executor.execute {
      val loaded = readInstalledApps()
      runOnUiThread {
        if (isFinishing || isDestroyed) return@runOnUiThread
        allApps = loaded
        binding.loadingIndicator.isVisible = false
        filterApps(binding.filterInput.text?.toString().orEmpty())
      }
    }
  }

  private fun filterApps(rawQuery: String) {
    if (!::appsAdapter.isInitialized || binding.loadingIndicator.isVisible) return
    val query = rawQuery.trim()
    val filtered = if (query.isEmpty()) {
      allApps
    } else {
      allApps.filter {
        it.label.contains(query, ignoreCase = true) ||
          it.packageName.contains(query, ignoreCase = true)
      }
    }
    appsAdapter.submitItems(filtered)
    binding.emptyState.isVisible = filtered.isEmpty()
  }

  private fun selectApp(app: InstalledAppInfo) {
    setResult(
      RESULT_OK,
      Intent()
        .putExtra(EXTRA_APP_LABEL, app.label)
        .putExtra(EXTRA_PACKAGE_NAME, app.packageName)
    )
    finish()
  }

  @Suppress("DEPRECATION")
  private fun readInstalledApps(): List<InstalledAppInfo> {
    val flags = PackageManager.MATCH_DISABLED_COMPONENTS
    val applications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.getInstalledApplications(
        PackageManager.ApplicationInfoFlags.of(flags.toLong())
      )
    } else {
      packageManager.getInstalledApplications(flags)
    }

    val collator = Collator.getInstance(Locale.getDefault())
    return applications.asSequence()
      .filter { it.packageName != packageName }
      .filter { application ->
        val isUserApp = application.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
          application.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        isUserApp || packageManager.getLaunchIntentForPackage(application.packageName) != null
      }
      .mapNotNull { application ->
        val label = runCatching {
          packageManager.getApplicationLabel(application).toString().trim()
        }.getOrNull().orEmpty()
        label.takeIf { it.isNotEmpty() }?.let {
          InstalledAppInfo(packageName = application.packageName, label = it)
        }
      }
      .distinctBy { it.packageName }
      .sortedWith { left, right ->
        val labelOrder = collator.compare(left.label, right.label)
        if (labelOrder != 0) labelOrder else left.packageName.compareTo(right.packageName)
      }
      .toList()
  }

  companion object {
    const val EXTRA_APP_LABEL = "installed_app_label"
    const val EXTRA_PACKAGE_NAME = "installed_app_package"
  }
}
