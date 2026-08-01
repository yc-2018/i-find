package com.cgl.ifind.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

fun AppCompatActivity.applySystemBarInsets(
  root: View,
  includeIme: Boolean = false,
  onInsetsChanged: ((WindowInsetsCompat) -> Unit)? = null
) {
  WindowCompat.setDecorFitsSystemWindows(window, false)

  val initialLeft = root.paddingLeft
  val initialTop = root.paddingTop
  val initialRight = root.paddingRight
  val initialBottom = root.paddingBottom

  ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
    val safeInsets = windowInsets.getInsets(
      WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    )
    val bottomInset = if (includeIme) {
      maxOf(safeInsets.bottom, windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
    } else {
      safeInsets.bottom
    }
    view.setPadding(
      initialLeft + safeInsets.left,
      initialTop + safeInsets.top,
      initialRight + safeInsets.right,
      initialBottom + bottomInset
    )
    onInsetsChanged?.invoke(windowInsets)
    windowInsets
  }

  WindowCompat.getInsetsController(window, root).apply {
    isAppearanceLightStatusBars = true
    isAppearanceLightNavigationBars = true
  }
  root.post { ViewCompat.requestApplyInsets(root) }
}
