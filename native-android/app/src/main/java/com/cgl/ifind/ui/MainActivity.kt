package com.cgl.ifind.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.R
import com.cgl.ifind.data.AppStore
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.databinding.ActivityMainBinding
import com.cgl.ifind.util.SearchLauncher

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var store: AppStore
  private lateinit var targetAdapter: TargetGridAdapter
  private lateinit var targetLayoutManager: GridLayoutManager
  private lateinit var targetTouchHelper: ItemTouchHelper
  private var targetGridDecoration: GridSpacingItemDecoration? = null
  private var currentColumnCount = 0
  private var homeDragChanged = false

  private val historyLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
    val keyword = result.data?.getStringExtra(HistoryActivity.EXTRA_KEYWORD).orEmpty()
    if (keyword.isNotBlank()) {
      binding.searchInput.setText(keyword)
      binding.searchInput.setSelection(keyword.length)
      showKeyboard()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applySystemBarInsets(binding.root)

    store = AppStore(applicationContext)
    targetAdapter = TargetGridAdapter(::searchTarget)
    targetLayoutManager = GridLayoutManager(this, AppStore.HOME_COLUMN_COUNT_DEFAULT)

    binding.targetList.apply {
      layoutManager = targetLayoutManager
      adapter = targetAdapter
      itemAnimator = null
    }
    updateTargetGridColumns()
    targetTouchHelper = ItemTouchHelper(createTargetDragCallback())
    targetTouchHelper.attachToRecyclerView(binding.targetList)

    binding.searchInput.doAfterTextChanged {
      binding.clearButton.isVisible = !it.isNullOrEmpty()
    }
    binding.searchInput.setOnEditorActionListener { _, _, _ ->
      Toast.makeText(this, R.string.tap_target_hint, Toast.LENGTH_SHORT).show()
      true
    }
    binding.clearButton.setOnClickListener {
      binding.searchInput.text.clear()
      showKeyboard()
    }
    binding.historyButton.setOnClickListener {
      historyLauncher.launch(Intent(this, HistoryActivity::class.java))
    }
    binding.settingsButton.setOnClickListener {
      startActivity(Intent(this, SettingsActivity::class.java))
    }

    window.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
    )
    binding.searchInput.postDelayed({ showKeyboard() }, 180L)
  }

  override fun onResume() {
    super.onResume()
    updateTargetGridColumns()
    reloadTargets()
  }

  override fun onPause() {
    persistHomeOrder()
    super.onPause()
  }

  private fun reloadTargets() {
    val visibleTargets = store.getTargets().filterNot { it.hidden }.sortedBy { it.sortOrder }
    targetAdapter.submitTargets(visibleTargets, store.areTargetLabelsVisible())
    binding.emptyState.isVisible = visibleTargets.isEmpty()
  }

  private fun updateTargetGridColumns() {
    val columnCount = store.getHomeColumnCount()
    if (columnCount == currentColumnCount) return

    targetLayoutManager.spanCount = columnCount
    targetGridDecoration?.let(binding.targetList::removeItemDecoration)
    val spacingDp = if (columnCount == AppStore.HOME_COLUMN_COUNT_COMPACT) {
      COMPACT_GRID_SPACING_DP
    } else {
      DEFAULT_GRID_SPACING_DP
    }
    targetGridDecoration = GridSpacingItemDecoration(
      columnCount,
      (spacingDp * resources.displayMetrics.density).toInt()
    ).also(binding.targetList::addItemDecoration)
    currentColumnCount = columnCount
    binding.targetList.invalidateItemDecorations()
  }

  private fun searchTarget(target: SearchTarget) {
    val keyword = binding.searchInput.text.toString().trim()
    if (keyword.isEmpty()) {
      Toast.makeText(this, R.string.enter_keyword, Toast.LENGTH_SHORT).show()
      showKeyboard()
      return
    }

    store.addHistory(keyword, target)
    binding.searchInput.clearFocus()
    getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(
      binding.searchInput.windowToken,
      0
    )
    SearchLauncher.launch(
      activity = this,
      target = target,
      keyword = keyword,
      autoDefrostEnabled = store.isAutoDefrostEnabled()
    )
  }

  private fun createTargetDragCallback(): ItemTouchHelper.Callback {
    return object : ItemTouchHelper.SimpleCallback(
      ItemTouchHelper.UP or ItemTouchHelper.DOWN or
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
      0
    ) {
      override fun isLongPressDragEnabled(): Boolean = true

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        val moved = targetAdapter.moveTarget(
          viewHolder.bindingAdapterPosition,
          target.bindingAdapterPosition
        )
        homeDragChanged = homeDragChanged || moved
        return moved
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

      override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
          binding.targetList.stopScroll()
          viewHolder?.itemView?.apply {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            alpha = 0.92f
            scaleX = 1.04f
            scaleY = 1.04f
            elevation = 10f * resources.displayMetrics.density
          }
        }
      }

      override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.apply {
          alpha = 1f
          scaleX = 1f
          scaleY = 1f
          elevation = 2f * resources.displayMetrics.density
        }
        recyclerView.stopScroll()
        persistHomeOrder()
      }

      override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long
      ): Int {
        val maxStep = (8 * recyclerView.resources.displayMetrics.density).toInt()
        return viewSizeOutOfBounds.coerceIn(-maxStep, maxStep)
      }
    }
  }

  private fun persistHomeOrder() {
    if (!homeDragChanged) return
    store.saveVisibleTargetOrder(targetAdapter.snapshotTargetIds())
    homeDragChanged = false
  }

  private fun showKeyboard() {
    if (isFinishing || isDestroyed) return
    binding.searchInput.requestFocus()
    binding.searchInput.setSelection(binding.searchInput.text?.length ?: 0)
    getSystemService<InputMethodManager>()?.showSoftInput(
      binding.searchInput,
      InputMethodManager.SHOW_IMPLICIT
    )
  }

  companion object {
    private const val DEFAULT_GRID_SPACING_DP = 8
    private const val COMPACT_GRID_SPACING_DP = 6
  }
}
