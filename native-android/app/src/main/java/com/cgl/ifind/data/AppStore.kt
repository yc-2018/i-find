package com.cgl.ifind.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AppStore(private val context: Context) {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  init {
    migrateLegacyExpoDataIfNeeded()
    if (!preferences.contains(KEY_TARGETS)) {
      saveTargets(DefaultTargets.create())
    }
  }

  @Synchronized
  fun getTargets(): MutableList<SearchTarget> {
    val rawValue = preferences.getString(KEY_TARGETS, null) ?: return DefaultTargets.create().toMutableList()

    return try {
      val array = JSONArray(rawValue)
      val targets = buildList {
        for (index in 0 until array.length()) {
          SearchTarget.fromJson(array.getJSONObject(index))?.let(::add)
        }
      }
      normalizeTargets(targets).toMutableList()
    } catch (_: Exception) {
      DefaultTargets.create().toMutableList()
    }
  }

  @Synchronized
  fun saveTargets(targets: List<SearchTarget>) {
    val normalizedTargets = normalizeTargets(targets)
    val array = JSONArray()
    normalizedTargets.forEach { array.put(it.toJson()) }
    preferences.edit().putString(KEY_TARGETS, array.toString()).apply()
  }

  @Synchronized
  fun getTarget(id: String): SearchTarget? = getTargets().firstOrNull { it.id == id }

  @Synchronized
  fun saveTarget(target: SearchTarget) {
    val targets = getTargets()
    val existingIndex = targets.indexOfFirst { it.id == target.id }

    if (existingIndex >= 0) {
      val previousTarget = targets[existingIndex]
      if (previousTarget.iconMode == IconModes.GALLERY && previousTarget.iconValue != target.iconValue) {
        deleteOwnedIcon(previousTarget.iconValue)
      }
      targets[existingIndex] = target.copy(sortOrder = previousTarget.sortOrder)
    } else {
      targets += target.copy(sortOrder = targets.size)
    }

    saveTargets(targets)
  }

  @Synchronized
  fun deleteTarget(id: String) {
    val targets = getTargets()
    val removedTarget = targets.firstOrNull { it.id == id } ?: return
    if (removedTarget.iconMode == IconModes.GALLERY) {
      deleteOwnedIcon(removedTarget.iconValue)
    }
    saveTargets(targets.filterNot { it.id == id })
  }

  @Synchronized
  fun restoreDefaults() {
    getTargets()
      .filter { it.iconMode == IconModes.GALLERY }
      .forEach { deleteOwnedIcon(it.iconValue) }
    saveTargets(DefaultTargets.create())
  }

  fun isHistoryRecordingEnabled(): Boolean = preferences.getBoolean(KEY_RECORD_HISTORY, true)

  fun setHistoryRecordingEnabled(enabled: Boolean) {
    preferences.edit().putBoolean(KEY_RECORD_HISTORY, enabled).apply()
  }

  fun areTargetLabelsVisible(): Boolean = preferences.getBoolean(KEY_SHOW_TARGET_LABELS, true)

  fun setTargetLabelsVisible(visible: Boolean) {
    preferences.edit().putBoolean(KEY_SHOW_TARGET_LABELS, visible).apply()
  }

  fun isAutoDefrostEnabled(): Boolean = preferences.getBoolean(KEY_AUTO_DEFROST, false)

  fun setAutoDefrostEnabled(enabled: Boolean) {
    preferences.edit().putBoolean(KEY_AUTO_DEFROST, enabled).apply()
  }

  @Synchronized
  fun saveVisibleTargetOrder(visibleTargetIds: List<String>) {
    val allTargets = getTargets().sortedBy { it.sortOrder }
    val visibleTargetsById = allTargets
      .filterNot { it.hidden }
      .associateBy { it.id }
    val reorderedVisibleTargets = visibleTargetIds.mapNotNull(visibleTargetsById::get)
    if (reorderedVisibleTargets.size != visibleTargetsById.size) return

    val iterator = reorderedVisibleTargets.iterator()
    val mergedTargets = allTargets.map { target ->
      if (target.hidden) target else iterator.next()
    }.mapIndexed { index, target -> target.copy(sortOrder = index) }
    saveTargets(mergedTargets)
  }

  @Synchronized
  fun addHistory(keyword: String, target: SearchTarget) {
    if (!isHistoryRecordingEnabled()) {
      return
    }

    val trimmedKeyword = keyword.trim()
    if (trimmedKeyword.isEmpty()) {
      return
    }

    val nextHistory = mutableListOf(
      SearchHistory(
        id = UUID.randomUUID().toString(),
        keyword = trimmedKeyword,
        targetId = target.id,
        targetName = target.name,
        searchedAt = System.currentTimeMillis()
      )
    )
    nextHistory += getHistory()
    saveHistory(nextHistory.take(MAX_HISTORY_ITEMS))
  }

  @Synchronized
  fun getHistory(): MutableList<SearchHistory> {
    val rawValue = preferences.getString(KEY_HISTORY, null) ?: return mutableListOf()

    return try {
      val array = JSONArray(rawValue)
      buildList {
        for (index in 0 until array.length()) {
          SearchHistory.fromJson(array.getJSONObject(index))?.let(::add)
        }
      }.sortedByDescending { it.searchedAt }.toMutableList()
    } catch (_: Exception) {
      mutableListOf()
    }
  }

  @Synchronized
  fun deleteHistory(id: String) {
    saveHistory(getHistory().filterNot { it.id == id })
  }

  @Synchronized
  fun clearHistory() {
    preferences.edit().remove(KEY_HISTORY).apply()
  }

  @Synchronized
  fun clearHistoryDay(dayKey: String) {
    val day = runCatching { LocalDate.parse(dayKey) }.getOrNull() ?: return
    val zoneId = ZoneId.systemDefault()
    val start = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    saveHistory(getHistory().filterNot { it.searchedAt >= start && it.searchedAt < end })
  }

  private fun saveHistory(items: List<SearchHistory>) {
    val array = JSONArray()
    items.forEach { array.put(it.toJson()) }
    preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
  }

  private fun normalizeTargets(targets: List<SearchTarget>): List<SearchTarget> {
    return targets.sortedBy { it.sortOrder }.mapIndexed { index, target ->
      val builtinPackageName = DefaultTargets.create()
        .firstOrNull { it.id == target.id }
        ?.androidPackageName

      target.copy(
        androidPackageName = target.androidPackageName ?: builtinPackageName,
        sortOrder = index
      )
    }
  }

  private fun deleteOwnedIcon(path: String) {
    val iconRoot = File(context.filesDir, "icons").canonicalFile
    val targetFile = runCatching { File(path).canonicalFile }.getOrNull() ?: return
    if (targetFile.path.startsWith(iconRoot.path + File.separator)) {
      targetFile.delete()
    }
  }

  private fun migrateLegacyExpoDataIfNeeded() {
    if (preferences.getBoolean(KEY_LEGACY_MIGRATED, false)) {
      return
    }

    readLegacyAsyncStorageValue("ifind-search-targets")?.let { rawTargets ->
      runCatching {
        val array = JSONArray(rawTargets)
        val targets = buildList {
          for (index in 0 until array.length()) {
            SearchTarget.fromJson(array.getJSONObject(index))?.let(::add)
          }
        }
        if (targets.isNotEmpty()) {
          val normalized = normalizeTargets(targets)
          val nativeArray = JSONArray()
          normalized.forEach { nativeArray.put(it.toJson()) }
          preferences.edit().putString(KEY_TARGETS, nativeArray.toString()).apply()
        }
      }
    }

    readLegacyAsyncStorageValue("ifind-stopapp-settings")?.let { rawSettings ->
      runCatching {
        val settings = JSONObject(rawSettings)
        preferences.edit()
          .putBoolean(KEY_AUTO_DEFROST, settings.optBoolean("autoDefrostEnabled", false))
          .apply()
      }
    }

    preferences.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
  }

  private fun readLegacyAsyncStorageValue(key: String): String? {
    val candidateFiles = listOf(
      context.getDatabasePath("RKStorage"),
      context.getDatabasePath("RKStorage.db")
    )

    val databaseFile = candidateFiles.firstOrNull { it.exists() } ?: return null
    return runCatching {
      SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
        database.query(
          "catalystLocalStorage",
          arrayOf("value"),
          "key = ?",
          arrayOf(key),
          null,
          null,
          null,
          "1"
        ).use { cursor ->
          if (cursor.moveToFirst()) cursor.getString(0) else null
        }
      }
    }.getOrNull()
  }

  companion object {
    private const val PREFERENCES_NAME = "ifind_native_preferences"
    private const val KEY_TARGETS = "search_targets_json"
    private const val KEY_HISTORY = "search_history_json"
    private const val KEY_RECORD_HISTORY = "record_history"
    private const val KEY_SHOW_TARGET_LABELS = "show_target_labels"
    private const val KEY_AUTO_DEFROST = "auto_defrost"
    private const val KEY_LEGACY_MIGRATED = "legacy_expo_migrated"
    private const val MAX_HISTORY_ITEMS = 500
  }
}
