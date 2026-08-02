package com.cgl.ifind.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuiltinIconAssetCatalogTest {
  @Test
  fun numericPrefixesAreComparedAsIntegers() {
    val assets = BuiltinIconAssetCatalog.build(
      listOf("050_fifty.png", "06_six.svg", "2_two.webp")
    )

    assertEquals(
      listOf("two.webp", "six.svg", "fifty.png"),
      BuiltinIconAssetCatalog.selectableIconValues(assets)
    )
  }

  @Test
  fun unnumberedFilesFollowNumberedFilesAlphabetically() {
    val assets = BuiltinIconAssetCatalog.build(
      listOf("zebra.png", "02_second.png", "alpha.svg", "01_first.jpg")
    )

    assertEquals(
      listOf("first.jpg", "second.png", "alpha.svg", "zebra.png"),
      BuiltinIconAssetCatalog.selectableIconValues(assets)
    )
  }

  @Test
  fun equalNumericPrefixesUseStableKeyAsTieBreaker() {
    val assets = BuiltinIconAssetCatalog.build(
      listOf("006_zebra.png", "06_alpha.png")
    )

    assertEquals(
      listOf("alpha.png", "zebra.png"),
      BuiltinIconAssetCatalog.selectableIconValues(assets)
    )
  }

  @Test
  fun changingOnlyThePrefixKeepsTheSavedKeyResolvable() {
    val assets = BuiltinIconAssetCatalog.build(listOf("050_douyin.png"))

    assertEquals("douyin.png", BuiltinIconAssetCatalog.selectableIconValues(assets).single())
    assertEquals("050_douyin.png", BuiltinIconAssetCatalog.resolveFileName(assets, "douyin.png"))
  }

  @Test
  fun oldNumberedFileNamesAreNotTreatedAsStableKeys() {
    val assets = BuiltinIconAssetCatalog.build(listOf("050_douyin.png"))

    assertNull(BuiltinIconAssetCatalog.resolveFileName(assets, "01_douyin.png"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun duplicateStableKeysAreRejected() {
    BuiltinIconAssetCatalog.build(
      listOf("01_douyin.png", "douyin.png", "011_douyin.png")
    )
  }
}
