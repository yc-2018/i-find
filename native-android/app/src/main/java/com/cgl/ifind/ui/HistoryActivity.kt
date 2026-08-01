package com.cgl.ifind.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.cgl.ifind.R
import com.cgl.ifind.data.AppStore
import com.cgl.ifind.data.SearchHistory
import com.cgl.ifind.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
  private lateinit var binding: ActivityHistoryBinding
  private lateinit var store: AppStore
  private lateinit var historyAdapter: HistoryAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applySystemBarInsets(binding.root)

    store = AppStore(applicationContext)
    historyAdapter = HistoryAdapter(::selectHistory, ::deleteHistory, ::confirmClearDay)
    binding.historyList.layoutManager = LinearLayoutManager(this)
    binding.historyList.adapter = historyAdapter
    binding.historyList.itemAnimator = null

    binding.backButton.setOnClickListener { finish() }
    binding.clearAllButton.setOnClickListener { confirmClearAll() }
  }

  override fun onResume() {
    super.onResume()
    reloadHistory()
  }

  private fun reloadHistory() {
    val history = store.getHistory()
    historyAdapter.submitItems(history)
    binding.emptyState.isVisible = history.isEmpty()
    binding.clearAllButton.isEnabled = history.isNotEmpty()
    binding.clearAllButton.alpha = if (history.isEmpty()) 0.45f else 1f
  }

  private fun selectHistory(item: SearchHistory) {
    setResult(RESULT_OK, Intent().putExtra(EXTRA_KEYWORD, item.keyword))
    finish()
  }

  private fun deleteHistory(item: SearchHistory) {
    store.deleteHistory(item.id)
    reloadHistory()
  }

  private fun confirmClearAll() {
    if (store.getHistory().isEmpty()) return
    AlertDialog.Builder(this)
      .setMessage(R.string.confirm_clear_history)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.clear_all) { _, _ ->
        store.clearHistory()
        reloadHistory()
      }
      .show()
  }

  private fun confirmClearDay(dayKey: String, dayTitle: String) {
    AlertDialog.Builder(this)
      .setMessage(getString(R.string.confirm_clear_day, dayTitle))
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.clear_day) { _, _ ->
        store.clearHistoryDay(dayKey)
        reloadHistory()
      }
      .show()
  }

  companion object {
    const val EXTRA_KEYWORD = "history_keyword"
  }
}
