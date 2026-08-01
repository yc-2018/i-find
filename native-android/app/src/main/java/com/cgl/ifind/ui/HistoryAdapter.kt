package com.cgl.ifind.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.R
import com.cgl.ifind.data.SearchHistory
import com.cgl.ifind.databinding.ItemHistoryBinding
import com.cgl.ifind.databinding.ItemHistoryDayHeaderBinding
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class HistoryAdapter(
  private val onSelect: (SearchHistory) -> Unit,
  private val onDelete: (SearchHistory) -> Unit,
  private val onClearDay: (dayKey: String, dayTitle: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  private val collapsedDays = mutableSetOf<LocalDate>()
  private var groups: List<HistoryGroup> = emptyList()
  private var rows: List<HistoryRow> = emptyList()
  private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  init {
    setHasStableIds(true)
  }

  fun submitItems(nextItems: List<SearchHistory>) {
    val zoneId = ZoneId.systemDefault()
    groups = nextItems
      .groupBy { Instant.ofEpochMilli(it.searchedAt).atZone(zoneId).toLocalDate() }
      .map { (day, items) -> HistoryGroup(day, items.sortedByDescending { it.searchedAt }) }
      .sortedByDescending { it.day }
    collapsedDays.retainAll(groups.mapTo(mutableSetOf()) { it.day })
    rebuildRows()
  }

  override fun getItemId(position: Int): Long {
    return when (val row = rows[position]) {
      is HistoryRow.DayHeader -> Long.MIN_VALUE or (row.group.day.hashCode().toLong() and 0xffffffffL)
      is HistoryRow.Item -> row.history.id.hashCode().toLong()
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (rows[position]) {
      is HistoryRow.DayHeader -> VIEW_TYPE_DAY_HEADER
      is HistoryRow.Item -> VIEW_TYPE_HISTORY
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return if (viewType == VIEW_TYPE_DAY_HEADER) {
      DayHeaderViewHolder(ItemHistoryDayHeaderBinding.inflate(inflater, parent, false))
    } else {
      HistoryViewHolder(ItemHistoryBinding.inflate(inflater, parent, false))
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is DayHeaderViewHolder -> holder.bind((rows[position] as HistoryRow.DayHeader).group)
      is HistoryViewHolder -> holder.bind((rows[position] as HistoryRow.Item).history)
    }
  }

  override fun getItemCount(): Int = rows.size

  private fun toggleDay(day: LocalDate) {
    if (!collapsedDays.add(day)) {
      collapsedDays.remove(day)
    }
    rebuildRows()
  }

  private fun rebuildRows() {
    rows = buildList {
      groups.forEach { group ->
        add(HistoryRow.DayHeader(group))
        if (group.day !in collapsedDays) {
          group.items.forEach { add(HistoryRow.Item(it)) }
        }
      }
    }
    notifyDataSetChanged()
  }

  private fun formatDayTitle(context: Context, day: LocalDate): String {
    val formattedDate = day.format(DAY_FORMATTER)
    val today = LocalDate.now()
    return when (day) {
      today -> context.getString(R.string.today_with_date, formattedDate)
      today.minusDays(1) -> context.getString(R.string.yesterday_with_date, formattedDate)
      else -> formattedDate
    }
  }

  inner class DayHeaderViewHolder(
    private val binding: ItemHistoryDayHeaderBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(group: HistoryGroup) {
      val expanded = group.day !in collapsedDays
      val title = formatDayTitle(binding.root.context, group.day)
      binding.dayTitle.text = title
      binding.dayCount.text = binding.root.context.getString(
        R.string.history_count,
        group.items.size
      )
      binding.expandIcon.rotation = if (expanded) 90f else 0f
      binding.root.setOnClickListener { toggleDay(group.day) }
      binding.clearDayButton.setOnClickListener {
        onClearDay(group.day.toString(), title)
      }
    }
  }

  inner class HistoryViewHolder(
    private val binding: ItemHistoryBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: SearchHistory) {
      binding.keywordText.text = item.keyword
      val targetLabel = item.targetName.ifBlank {
        binding.root.context.getString(R.string.web_search)
      }
      val targetText = binding.root.context.getString(R.string.history_target_format, targetLabel)
      binding.metaText.text = binding.root.context.getString(
        R.string.history_meta_format,
        targetText,
        timeFormat.format(Date(item.searchedAt))
      )
      binding.root.setOnClickListener { onSelect(item) }
      binding.deleteButton.setOnClickListener { onDelete(item) }
    }
  }

  data class HistoryGroup(
    val day: LocalDate,
    val items: List<SearchHistory>
  )

  private sealed interface HistoryRow {
    data class DayHeader(val group: HistoryGroup) : HistoryRow
    data class Item(val history: SearchHistory) : HistoryRow
  }

  companion object {
    private const val VIEW_TYPE_DAY_HEADER = 0
    private const val VIEW_TYPE_HISTORY = 1
    private val DAY_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy年M月d日 EEEE",
      Locale.SIMPLIFIED_CHINESE
    )
  }
}
