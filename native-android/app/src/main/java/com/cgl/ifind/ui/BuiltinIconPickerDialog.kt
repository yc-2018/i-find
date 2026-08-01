package com.cgl.ifind.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import com.cgl.ifind.R
import com.cgl.ifind.util.IconLoader

object BuiltinIconPickerDialog {
  fun show(context: Context, selectedIconValue: String, onSelected: (String) -> Unit) {
    val density = context.resources.displayMetrics.density
    val horizontalPadding = (12 * density).toInt()
    val itemMargin = (4 * density).toInt()
    val availableWidth = context.resources.displayMetrics.widthPixels - (128 * density).toInt()
    val cellSize = (availableWidth / COLUMN_COUNT).coerceAtLeast((44 * density).toInt())
    val grid = GridLayout(context).apply {
      columnCount = COLUMN_COUNT
      setPadding(horizontalPadding, horizontalPadding, horizontalPadding, horizontalPadding)
    }
    val scrollView = ScrollView(context).apply {
      isFillViewport = true
      addView(
        grid,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      )
    }

    lateinit var dialog: AlertDialog
    IconLoader.listBuiltinIconValues(context).forEach { iconValue ->
      val button = AppCompatImageButton(context).apply {
        layoutParams = GridLayout.LayoutParams().apply {
          width = cellSize
          height = cellSize
          setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
        }
        background = AppCompatResources.getDrawable(context, R.drawable.bg_icon_choice)
        contentDescription = context.getString(R.string.icon_preview)
        isSelected = iconValue == selectedIconValue
        scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        setPadding(
          (12 * density).toInt(),
          (12 * density).toInt(),
          (12 * density).toInt(),
          (12 * density).toInt()
        )
        IconLoader.loadBuiltinInto(this, iconValue)
        setOnClickListener {
          onSelected(iconValue)
          dialog.dismiss()
        }
      }
      grid.addView(button)
    }

    dialog = AlertDialog.Builder(context)
      .setTitle(R.string.builtin_icons)
      .setView(scrollView)
      .setNegativeButton(R.string.cancel, null)
      .create()
    dialog.show()
  }

  private const val COLUMN_COUNT = 5
}
