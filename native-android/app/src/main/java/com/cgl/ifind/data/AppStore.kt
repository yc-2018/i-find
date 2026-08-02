package com.cgl.ifind.data

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AppStore(private val context: Context) {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  init {
    if (!preferences.contains(KEY_TARGETS)) {
      saveTargets(DefaultTargets.create())
    }
    migrateDefaultTargetsIfNeeded()
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

  fun getHomeColumnCount(): Int {
    return when (preferences.getInt(KEY_HOME_COLUMN_COUNT, HOME_COLUMN_COUNT_DEFAULT)) {
      HOME_COLUMN_COUNT_COMPACT -> HOME_COLUMN_COUNT_COMPACT
      else -> HOME_COLUMN_COUNT_DEFAULT
    }
  }

  fun setHomeColumnCount(columnCount: Int) {
    val normalizedColumnCount = if (columnCount == HOME_COLUMN_COUNT_COMPACT) {
      HOME_COLUMN_COUNT_COMPACT
    } else {
      HOME_COLUMN_COUNT_DEFAULT
    }
    preferences.edit().putInt(KEY_HOME_COLUMN_COUNT, normalizedColumnCount).apply()
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
      target.copy(sortOrder = index)
    }
  }

  private fun migrateDefaultTargetsIfNeeded() {
    val savedVersion = preferences.getInt(KEY_DEFAULT_TARGETS_VERSION, 0)
    if (savedVersion >= DEFAULT_TARGETS_VERSION) {
      return
    }

    val defaults = DefaultTargets.create()
    var targets = getTargets().toList()

    if (savedVersion < DEFAULT_TARGETS_ADDED_VERSION) {
      val existingIds = targets.mapTo(hashSetOf()) { it.id }
      val missingTargets = defaults
        .filterNot { it.id in existingIds }
        .mapIndexed { index, target ->
          target.copy(
            hidden = true,
            sortOrder = targets.size + index
          )
        }
      targets += missingTargets
    }

    if (savedVersion < DEFAULT_TARGETS_ORDER_VERSION) {
      targets = reorderDefaultTargets(targets, defaults).map { target ->
        if (target.id == DEFAULT_VISIBLE_XIANYU_ID) target.copy(hidden = false) else target
      }
    }

    if (savedVersion < DEFAULT_TARGETS_BUILTIN_ICONS_VERSION) {
      targets = updateNewBuiltinIcons(targets, defaults)
      targets = reorderDefaultTargets(targets, defaults)
    }

    saveTargets(targets)
    preferences.edit().putInt(KEY_DEFAULT_TARGETS_VERSION, DEFAULT_TARGETS_VERSION).apply()
  }

  private fun reorderDefaultTargets(
    targets: List<SearchTarget>,
    defaults: List<SearchTarget>
  ): List<SearchTarget> {
    val defaultIds = defaults.mapTo(hashSetOf()) { it.id }
    val targetsById = targets
      .filter { it.id in defaultIds }
      .associateBy { it.id }
    val orderedDefaults = defaults.mapNotNull { targetsById[it.id] }.iterator()

    return targets.map { target ->
      if (target.id in defaultIds) orderedDefaults.next() else target
    }
  }

  private fun updateNewBuiltinIcons(
    targets: List<SearchTarget>,
    defaults: List<SearchTarget>
  ): List<SearchTarget> {
    val defaultsById = defaults.associateBy { it.id }
    return targets.map { target ->
      val defaultTarget = defaultsById[target.id] ?: return@map target
      val usesPreviousDefaultIcon = target.id in NEW_BUILTIN_ICON_TARGET_IDS &&
        target.iconMode == IconModes.INSTALLED_APP &&
        target.iconValue == defaultTarget.androidPackageName

      if (usesPreviousDefaultIcon) {
        target.copy(
          iconMode = defaultTarget.iconMode,
          iconValue = defaultTarget.iconValue
        )
      } else {
        target
      }
    }
  }

  private fun deleteOwnedIcon(path: String) {
    val iconRoot = File(context.filesDir, "icons").canonicalFile
    val targetFile = runCatching { File(path).canonicalFile }.getOrNull() ?: return
    if (targetFile.path.startsWith(iconRoot.path + File.separator)) {
      targetFile.delete()
    }
  }

  companion object {
    const val HOME_COLUMN_COUNT_DEFAULT = 4
    const val HOME_COLUMN_COUNT_COMPACT = 5

    private const val PREFERENCES_NAME = "ifind_native_preferences"
    private const val KEY_TARGETS = "search_targets_json"
    private const val KEY_HISTORY = "search_history_json"
    private const val KEY_RECORD_HISTORY = "record_history"
    private const val KEY_SHOW_TARGET_LABELS = "show_target_labels"
    private const val KEY_HOME_COLUMN_COUNT = "home_column_count"
    private const val KEY_AUTO_DEFROST = "auto_defrost"
    private const val KEY_DEFAULT_TARGETS_VERSION = "default_targets_version"
    private const val DEFAULT_TARGETS_ADDED_VERSION = 1
    private const val DEFAULT_TARGETS_ORDER_VERSION = 2
    private const val DEFAULT_TARGETS_BUILTIN_ICONS_VERSION = 3
    private const val DEFAULT_TARGETS_VERSION = DEFAULT_TARGETS_BUILTIN_ICONS_VERSION
    private const val DEFAULT_VISIBLE_XIANYU_ID = "xianyu"
    private val NEW_BUILTIN_ICON_TARGET_IDS = setOf("amap", "zhihu", "netease_music")
    private const val MAX_HISTORY_ITEMS = 500
  }
}
