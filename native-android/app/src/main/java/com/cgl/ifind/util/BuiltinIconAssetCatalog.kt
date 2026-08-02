package com.cgl.ifind.util

import java.math.BigInteger
import java.util.Locale

internal data class BuiltinIconAsset(
  val fileName: String,
  val iconValue: String,
  val sortOrder: BigInteger?
)

internal object BuiltinIconAssetCatalog {
  fun build(fileNames: Iterable<String>): List<BuiltinIconAsset> {
    val assets = fileNames.asSequence()
      .filter(::isSupportedFileName)
      .map(::parse)
      .sortedWith(assetComparator)
      .toList()
    requireUniqueIconValues(assets)
    return assets
  }

  fun selectableIconValues(assets: List<BuiltinIconAsset>): List<String> {
    return assets.map { it.iconValue }
  }

  fun resolveFileName(assets: List<BuiltinIconAsset>, iconValue: String): String? {
    val normalizedValue = iconValue.lowercase(Locale.ROOT)
    return assets.firstOrNull { it.fileName.lowercase(Locale.ROOT) == normalizedValue }?.fileName
      ?: assets.firstOrNull { it.iconValue.lowercase(Locale.ROOT) == normalizedValue }?.fileName
  }

  fun isSupportedFileName(fileName: String): Boolean {
    if (fileName.contains('/') || fileName.contains('\\')) return false
    return SUPPORTED_EXTENSIONS.any { extension ->
      fileName.endsWith(extension, ignoreCase = true)
    }
  }

  private fun parse(fileName: String): BuiltinIconAsset {
    val prefixMatch = ORDER_PREFIX_PATTERN.matchEntire(fileName)
    val order = prefixMatch?.groupValues?.get(1)?.let(::parseOrder)
    val keyCandidate = prefixMatch?.groupValues?.get(2)
    val hasValidPrefix = order != null && keyCandidate?.let(::isSupportedFileName) == true
    return BuiltinIconAsset(
      fileName = fileName,
      iconValue = if (hasValidPrefix) keyCandidate.orEmpty() else fileName,
      sortOrder = order.takeIf { hasValidPrefix }
    )
  }

  private fun parseOrder(value: String): BigInteger? {
    return runCatching { BigInteger(value) }.getOrNull()
  }

  private fun requireUniqueIconValues(assets: List<BuiltinIconAsset>) {
    val duplicateValues = assets
      .groupBy { it.iconValue.lowercase(Locale.ROOT) }
      .filterValues { it.size > 1 }
      .values
      .map { duplicateAssets ->
        duplicateAssets.joinToString(", ") { it.fileName }
      }
    require(duplicateValues.isEmpty()) {
      "Duplicate built-in icon keys: ${duplicateValues.joinToString("; ")}"
    }
  }

  private val assetComparator = Comparator<BuiltinIconAsset> { left, right ->
    val orderComparison = when {
      left.sortOrder != null && right.sortOrder != null -> {
        left.sortOrder.compareTo(right.sortOrder)
      }
      left.sortOrder != null -> -1
      right.sortOrder != null -> 1
      else -> 0
    }
    if (orderComparison != 0) {
      orderComparison
    } else {
      val keyComparison = left.iconValue.compareTo(right.iconValue, ignoreCase = true)
      if (keyComparison != 0) {
        keyComparison
      } else {
        left.fileName.compareTo(right.fileName, ignoreCase = true)
      }
    }
  }

  private val ORDER_PREFIX_PATTERN = Regex("^(\\d+)_(.+)$")
  private val SUPPORTED_EXTENSIONS = setOf(".png", ".webp", ".jpg", ".jpeg", ".svg")
}
