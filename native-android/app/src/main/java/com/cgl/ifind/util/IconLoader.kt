package com.cgl.ifind.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.cgl.ifind.R
import com.cgl.ifind.data.IconModes
import com.cgl.ifind.data.SearchTarget
import java.io.File

object IconLoader {
  private val builtinResources = mapOf(
    "asset:douyin" to R.drawable.ic_douyin,
    "asset:bilibili" to R.drawable.ic_bilibili,
    "asset:meituan" to R.drawable.ic_meituan,
    "asset:xhs" to R.drawable.ic_xhs,
    "asset:jd" to R.drawable.ic_jd,
    "asset:taobao" to R.drawable.ic_taobao,
    "asset:pdd" to R.drawable.ic_pdd,
    "builtin:search" to R.drawable.ic_search,
    "builtin:shopping" to R.drawable.ic_shopping,
    "builtin:play" to R.drawable.ic_play,
    "builtin:note" to R.drawable.ic_note,
    "builtin:web" to R.drawable.ic_web,
    "mdi:magnify" to R.drawable.ic_search,
    "mdi:shopping-outline" to R.drawable.ic_shopping,
    "mdi:food-fork-drink" to R.drawable.ic_search,
    "mdi:play-circle-outline" to R.drawable.ic_play,
    "mdi:note-text-outline" to R.drawable.ic_note,
    "mdi:book-open-page-variant-outline" to R.drawable.ic_note,
    "mdi:earth" to R.drawable.ic_web
  )

  fun loadInto(imageView: ImageView, target: SearchTarget) {
    imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    imageView.tag = null

    when (target.iconMode) {
      IconModes.BUILTIN -> {
        imageView.setImageResource(builtinResource(target.iconValue))
      }

      IconModes.INSTALLED_APP -> {
        val drawable = loadInstalledAppIcon(imageView.context, target.iconValue)
        if (drawable != null) {
          imageView.setImageDrawable(drawable)
        } else {
          imageView.setImageDrawable(GeneratedIconDrawable(target.name, target.name))
        }
      }

      IconModes.GALLERY -> {
        val bitmap = loadGalleryBitmap(imageView.context, target.iconValue)
        if (bitmap != null) {
          imageView.scaleType = ImageView.ScaleType.CENTER_CROP
          imageView.setImageBitmap(bitmap)
        } else {
          imageView.setImageDrawable(GeneratedIconDrawable(target.name, target.name))
        }
      }

      IconModes.REMOTE -> {
        imageView.setImageDrawable(GeneratedIconDrawable(target.name, target.name))
        val requestTag = "remote:${target.iconValue.trim()}"
        imageView.tag = requestTag
        RemoteIconCache.load(imageView.context, target.iconValue) { bitmap ->
          if (imageView.tag != requestTag || bitmap == null) return@load
          imageView.scaleType = ImageView.ScaleType.CENTER_CROP
          imageView.setImageBitmap(bitmap)
        }
      }

      else -> imageView.setImageDrawable(
        GeneratedIconDrawable(target.name, target.iconValue.ifBlank { target.name })
      )
    }
  }

  fun loadInstalledAppIcon(context: Context, packageName: String): Drawable? {
    return runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
  }

  fun builtinResource(iconValue: String): Int {
    return builtinResources[iconValue] ?: R.drawable.ic_search
  }

  private fun loadGalleryBitmap(context: Context, value: String) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decodeBitmap(context, value, bounds)
    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_ICON_BITMAP_SIZE)
    decodeBitmap(
      context,
      value,
      BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
  }.getOrNull()

  private fun decodeBitmap(context: Context, value: String, options: BitmapFactory.Options) =
    when {
      value.startsWith("content:") || value.startsWith("file:") -> {
        context.contentResolver.openInputStream(Uri.parse(value)).use { input ->
          BitmapFactory.decodeStream(input, null, options)
        }
      }

      else -> BitmapFactory.decodeFile(File(value).absolutePath, options)
    }

  private fun calculateSampleSize(width: Int, height: Int, requestedSize: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > requestedSize * 2 || height / sampleSize > requestedSize * 2) {
      sampleSize *= 2
    }
    return sampleSize
  }

  private const val MAX_ICON_BITMAP_SIZE = 256
}
