package com.cgl.ifind.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable

class GeneratedIconDrawable(
  private val label: String,
  colorSeed: String
) : Drawable() {
  private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = PALETTE[(colorSeed.hashCode() and Int.MAX_VALUE) % PALETTE.size]
  }
  private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
  }

  override fun draw(canvas: Canvas) {
    val bounds = bounds
    val radius = minOf(bounds.width(), bounds.height()) / 2f
    canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, backgroundPaint)

    val letter = label.trim().codePoints().findFirst().orElse('?'.code)
    val text = String(Character.toChars(letter))
    textPaint.textSize = radius * 0.9f
    val metrics = textPaint.fontMetrics
    val baseline = bounds.exactCenterY() - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(text, bounds.exactCenterX(), baseline, textPaint)
  }

  override fun setAlpha(alpha: Int) {
    backgroundPaint.alpha = alpha
    textPaint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    backgroundPaint.colorFilter = colorFilter
    textPaint.colorFilter = colorFilter
  }

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
  }

  companion object {
    private val PALETTE = intArrayOf(
      Color.parseColor("#F0843D"),
      Color.parseColor("#D94827"),
      Color.parseColor("#356F58"),
      Color.parseColor("#3E6AA3"),
      Color.parseColor("#8A5C8D"),
      Color.parseColor("#B87333"),
      Color.parseColor("#8D6A45")
    )
  }
}
