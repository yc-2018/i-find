package com.cgl.ifind.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.cgl.ifind.R

class SearchTargetCardView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val labelExtraHeight =
    resources.getDimensionPixelSize(R.dimen.search_target_label_extra_height)

  var labelsVisible: Boolean = true
    set(value) {
      if (field == value) return
      field = value
      requestLayout()
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
    val width = View.MeasureSpec.getSize(widthMeasureSpec)
    if (widthMode == View.MeasureSpec.UNSPECIFIED || width <= 0) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      return
    }

    val targetHeight = width + if (labelsVisible) labelExtraHeight else 0
    super.onMeasure(
      widthMeasureSpec,
      View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.EXACTLY)
    )
  }
}
