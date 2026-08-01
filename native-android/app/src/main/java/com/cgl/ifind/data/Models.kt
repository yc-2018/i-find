package com.cgl.ifind.data

import org.json.JSONObject

object IconModes {
  const val BUILTIN = "builtin"
  const val GALLERY = "gallery"
  const val GENERATED = "generated"
  const val INSTALLED_APP = "installedApp"
  const val REMOTE = "remote"
}

data class SearchTarget(
  val id: String,
  val name: String,
  val primaryTemplate: String,
  val fallbackTemplate: String? = null,
  val androidPackageName: String? = null,
  val iconMode: String,
  val iconValue: String,
  val hidden: Boolean,
  val sortOrder: Int
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("primaryTemplate", primaryTemplate)
    putNullable("fallbackTemplate", fallbackTemplate)
    putNullable("androidPackageName", androidPackageName)
    put("iconMode", iconMode)
    put("iconValue", iconValue)
    put("hidden", hidden)
    put("sortOrder", sortOrder)
  }

  companion object {
    fun fromJson(json: JSONObject): SearchTarget? {
      val id = json.optString("id").trim()
      val name = json.optString("name").trim()
      if (id.isEmpty() || name.isEmpty()) {
        return null
      }

      val primaryTemplate = json.optNullableString("primaryTemplate")
        ?: migrateLegacyPrimaryTemplate(json)
        ?: return null
      val fallbackTemplate = json.optNullableString("fallbackTemplate")
        ?: migrateLegacyFallbackTemplate(json, primaryTemplate)

      return SearchTarget(
        id = id,
        name = name,
        primaryTemplate = primaryTemplate,
        fallbackTemplate = fallbackTemplate,
        androidPackageName = json.optNullableString("androidPackageName"),
        iconMode = json.optString("iconMode", IconModes.GENERATED),
        iconValue = json.optString("iconValue", ""),
        hidden = json.optBoolean("hidden", false),
        sortOrder = json.optInt("sortOrder", 0)
      )
    }

    private fun migrateLegacyPrimaryTemplate(json: JSONObject): String? {
      val legacyScheme = json.optNullableString("schemeTemplate")
      val legacyWeb = json.optNullableString("webFallbackTemplate")
      return if (json.optString("launchMode") == LEGACY_SCHEME_FIRST) {
        legacyScheme ?: legacyWeb
      } else {
        legacyWeb ?: legacyScheme
      }
    }

    private fun migrateLegacyFallbackTemplate(
      json: JSONObject,
      primaryTemplate: String
    ): String? {
      if (json.optString("launchMode") != LEGACY_SCHEME_FIRST) return null
      return json.optNullableString("webFallbackTemplate")
        ?.takeIf { it != primaryTemplate }
    }

    private const val LEGACY_SCHEME_FIRST = "schemeFirst"
  }
}

data class SearchHistory(
  val id: String,
  val keyword: String,
  val targetId: String,
  val targetName: String,
  val searchedAt: Long
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("keyword", keyword)
    put("targetId", targetId)
    put("targetName", targetName)
    put("searchedAt", searchedAt)
  }

  companion object {
    fun fromJson(json: JSONObject): SearchHistory? {
      val id = json.optString("id").trim()
      val keyword = json.optString("keyword").trim()
      if (id.isEmpty() || keyword.isEmpty()) {
        return null
      }

      return SearchHistory(
        id = id,
        keyword = keyword,
        targetId = json.optString("targetId"),
        targetName = json.optString("targetName"),
        searchedAt = json.optLong("searchedAt", 0L)
      )
    }
  }
}

data class InstalledAppInfo(
  val packageName: String,
  val label: String
)

private fun JSONObject.putNullable(key: String, value: String?) {
  if (value.isNullOrBlank()) {
    put(key, JSONObject.NULL)
  } else {
    put(key, value)
  }
}

private fun JSONObject.optNullableString(key: String): String? {
  if (!has(key) || isNull(key)) {
    return null
  }

  return optString(key).trim().takeIf { it.isNotEmpty() }
}
