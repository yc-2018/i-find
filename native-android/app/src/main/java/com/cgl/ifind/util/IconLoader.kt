package com.cgl.ifind.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.LruCache
import android.widget.ImageView
import com.cgl.ifind.R
import com.cgl.ifind.data.IconModes
import com.cgl.ifind.data.SearchTarget
import com.caverock.androidsvg.SVG
import java.io.File

object IconLoader {
  const val DEFAULT_BUILTIN_ICON_VALUE = "douyin.png"

  private val legacyBuiltinAliases = mapOf(
    "asset:douyin" to "douyin.png",
    "asset:bilibili" to "bilibili.png",
    "asset:meituan" to "meituan.png",
    "asset:xhs" to "xhs.png",
    "asset:jd" to "jd.png",
    "asset:taobao" to "taobao.png",
    "asset:pdd" to "pdd.png",
    "builtin:search" to "search.svg",
    "builtin:shopping" to "shopping.svg",
    "builtin:play" to "play.svg",
    "builtin:note" to "note.svg",
    "builtin:web" to "web.svg",
    "mdi:magnify" to "search.svg",
    "mdi:shopping-outline" to "shopping.svg",
    "mdi:food-fork-drink" to "search.svg",
    "mdi:play-circle-outline" to "play.svg",
    "mdi:note-text-outline" to "note.svg",
    "mdi:book-open-page-variant-outline" to "note.svg",
    "mdi:earth" to "web.svg"
  )
  private val builtinAssetCache = object : LruCache<String, Bitmap>(BUILTIN_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
  }

  @Volatile
  private var builtinAssets: List<BuiltinIconAsset>? = null

  fun loadInto(imageView: ImageView, target: SearchTarget) {
    imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    imageView.tag = null

    when (target.iconMode) {
      IconModes.BUILTIN -> {
        loadBuiltinInto(imageView, target.iconValue)
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

  fun listBuiltinIconValues(context: Context): List<String> {
    return BuiltinIconAssetCatalog.selectableIconValues(getBuiltinAssets(context))
  }

  fun loadBuiltinInto(imageView: ImageView, iconValue: String) {
    imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    val compatibleIconValue = legacyBuiltinAliases[iconValue] ?: iconValue
    val assetFileName = BuiltinIconAssetCatalog.resolveFileName(
      getBuiltinAssets(imageView.context),
      compatibleIconValue
    ) ?: compatibleIconValue
    val bitmap = loadBuiltinAssetBitmap(imageView.context, assetFileName)
    if (bitmap != null) {
      imageView.setImageBitmap(bitmap)
    } else {
      imageView.setImageResource(R.drawable.ic_search)
    }
  }

  private fun loadBuiltinAssetBitmap(context: Context, fileName: String): Bitmap? {
    if (!BuiltinIconAssetCatalog.isSupportedFileName(fileName)) return null
    builtinAssetCache.get(fileName)?.let { return it }

    val assetPath = "$BUILTIN_ASSET_DIRECTORY/$fileName"
    val bitmap = runCatching {
      if (fileName.endsWith(".svg", ignoreCase = true)) {
        loadBuiltinSvgBitmap(context, assetPath)
      } else {
        loadBuiltinRasterBitmap(context, assetPath)
      }
    }.getOrNull()

    if (bitmap != null) builtinAssetCache.put(fileName, bitmap)
    return bitmap
  }

  private fun loadBuiltinRasterBitmap(context: Context, assetPath: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetPath).use { input ->
      BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = calculateSampleSize(
      bounds.outWidth,
      bounds.outHeight,
      MAX_ICON_BITMAP_SIZE
    )
    return context.assets.open(assetPath).use { input ->
      BitmapFactory.decodeStream(
        input,
        null,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
      )
    }
  }

  private fun loadBuiltinSvgBitmap(context: Context, assetPath: String): Bitmap? {
    val svg = context.assets.open(assetPath).use(SVG::getFromInputStream)
    val bitmap = Bitmap.createBitmap(
      MAX_ICON_BITMAP_SIZE,
      MAX_ICON_BITMAP_SIZE,
      Bitmap.Config.ARGB_8888
    )
    svg.renderToCanvas(Canvas(bitmap))
    return bitmap
  }

  private fun getBuiltinAssets(context: Context): List<BuiltinIconAsset> {
    builtinAssets?.let { return it }
    return synchronized(this) {
      builtinAssets ?: runCatching {
        BuiltinIconAssetCatalog.build(
          context.assets.list(BUILTIN_ASSET_DIRECTORY).orEmpty().asIterable()
        )
      }.getOrDefault(emptyList()).also { builtinAssets = it }
    }
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
  private const val BUILTIN_ASSET_DIRECTORY = "builtin-icons"
  private const val BUILTIN_CACHE_BYTES = 4 * 1024 * 1024
}
