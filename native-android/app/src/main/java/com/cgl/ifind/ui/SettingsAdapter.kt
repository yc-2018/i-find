package com.cgl.ifind.ui

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.R
import com.cgl.ifind.data.AppStore
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.databinding.ItemAppSettingsBinding
import com.cgl.ifind.databinding.ItemSettingsFooterBinding
import com.cgl.ifind.databinding.ItemSettingsHeaderBinding
import com.cgl.ifind.databinding.ItemSettingsTargetBinding
import com.cgl.ifind.util.IconLoader

enum class SettingsPage {
  TARGETS,
  APP_SETTINGS
}

data class SettingsHeaderState(
  val historyEnabled: Boolean = true,
  val targetLabelsVisible: Boolean = true,
  val homeColumnCount: Int = AppStore.HOME_COLUMN_COUNT_DEFAULT,
  val autoDefrostEnabled: Boolean = false,
  val canAutoDefrost: Boolean = false,
  val statusLabel: String = "",
  val statusDetail: String = "",
  val statusReady: Boolean = false,
  val packageNamesVisible: Boolean = false,
  val actionLabel: String = "",
  val actionEnabled: Boolean = false
)

interface SettingsAdapterListener {
  fun onAdd()
  fun onRestore()
  fun onHistoryChanged(enabled: Boolean)
  fun onTargetLabelsChanged(visible: Boolean)
  fun onHomeColumnCountChanged(columnCount: Int)
  fun onAutoDefrostChanged(enabled: Boolean)
  fun onShizukuAction()
  fun onRefreshStatus()
  fun onEdit(target: SearchTarget)
  fun onDelete(target: SearchTarget)
  fun onVisibilityChanged(target: SearchTarget, visible: Boolean)
  fun onDragRequested(holder: RecyclerView.ViewHolder)
}

class SettingsAdapter(
  private val listener: SettingsAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  private val targets = mutableListOf<SearchTarget>()
  private var headerState = SettingsHeaderState()
  private var currentPage = SettingsPage.TARGETS

  init {
    setHasStableIds(true)
  }

  fun submitTargets(nextTargets: List<SearchTarget>) {
    targets.clear()
    targets.addAll(nextTargets.sortedBy { it.sortOrder })
    notifyDataSetChanged()
  }

  fun showPage(page: SettingsPage) {
    if (currentPage == page) return
    currentPage = page
    notifyDataSetChanged()
  }

  fun updateHeader(nextState: SettingsHeaderState) {
    val packageVisibilityChanged =
      headerState.packageNamesVisible != nextState.packageNamesVisible
    headerState = nextState
    if (currentPage == SettingsPage.APP_SETTINGS) {
      notifyItemChanged(0)
    }
    if (
      currentPage == SettingsPage.TARGETS &&
      packageVisibilityChanged &&
      targets.isNotEmpty()
    ) {
      notifyItemRangeChanged(1, targets.size)
    }
  }

  fun isTargetPosition(adapterPosition: Int): Boolean {
    return currentPage == SettingsPage.TARGETS && adapterPosition in 1..targets.size
  }

  fun moveTarget(fromAdapterPosition: Int, toAdapterPosition: Int): Boolean {
    if (!isTargetPosition(fromAdapterPosition) || !isTargetPosition(toAdapterPosition)) {
      return false
    }

    val fromIndex = fromAdapterPosition - 1
    val toIndex = toAdapterPosition - 1
    if (fromIndex == toIndex) return false

    val target = targets.removeAt(fromIndex)
    targets.add(toIndex, target)
    notifyItemMoved(fromAdapterPosition, toAdapterPosition)
    return true
  }

  fun snapshotTargets(): List<SearchTarget> {
    return targets.mapIndexed { index, target -> target.copy(sortOrder = index) }
  }

  override fun getItemId(position: Int): Long {
    return when (getItemViewType(position)) {
      VIEW_TYPE_HEADER -> Long.MIN_VALUE
      VIEW_TYPE_APP_SETTINGS -> Long.MIN_VALUE + 1
      VIEW_TYPE_FOOTER -> Long.MAX_VALUE
      else -> targets[position - 1].id.hashCode().toLong()
    }
  }

  override fun getItemViewType(position: Int): Int {
    if (currentPage == SettingsPage.APP_SETTINGS) {
      return if (position == 0) VIEW_TYPE_APP_SETTINGS else VIEW_TYPE_FOOTER
    }
    return when (position) {
      0 -> VIEW_TYPE_HEADER
      itemCount - 1 -> VIEW_TYPE_FOOTER
      else -> VIEW_TYPE_TARGET
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_HEADER -> HeaderViewHolder(ItemSettingsHeaderBinding.inflate(inflater, parent, false))
      VIEW_TYPE_APP_SETTINGS -> AppSettingsViewHolder(
        ItemAppSettingsBinding.inflate(inflater, parent, false)
      )
      VIEW_TYPE_FOOTER -> FooterViewHolder(ItemSettingsFooterBinding.inflate(inflater, parent, false))
      else -> TargetViewHolder(ItemSettingsTargetBinding.inflate(inflater, parent, false))
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is HeaderViewHolder -> holder.bind()
      is AppSettingsViewHolder -> holder.bind(headerState)
      is TargetViewHolder -> holder.bind(targets[position - 1])
    }
  }

  override fun getItemCount(): Int {
    return if (currentPage == SettingsPage.APP_SETTINGS) 2 else targets.size + 2
  }

  inner class HeaderViewHolder(
    private val binding: ItemSettingsHeaderBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind() {
      binding.addButton.setOnClickListener { listener.onAdd() }
      binding.restoreButton.setOnClickListener { listener.onRestore() }
    }
  }

  inner class AppSettingsViewHolder(
    private val binding: ItemAppSettingsBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(state: SettingsHeaderState) {
      binding.refreshStatusButton.setOnClickListener { listener.onRefreshStatus() }
      binding.shizukuActionButton.setOnClickListener { listener.onShizukuAction() }

      binding.historySwitch.setOnCheckedChangeListener(null)
      binding.historySwitch.isChecked = state.historyEnabled
      binding.historySwitch.setOnCheckedChangeListener { _, checked ->
        listener.onHistoryChanged(checked)
      }

      binding.targetLabelsSwitch.setOnCheckedChangeListener(null)
      binding.targetLabelsSwitch.isChecked = state.targetLabelsVisible
      binding.targetLabelsSwitch.setOnCheckedChangeListener { _, checked ->
        listener.onTargetLabelsChanged(checked)
      }

      binding.homeFiveColumnsSwitch.setOnCheckedChangeListener(null)
      binding.homeFiveColumnsSwitch.isChecked =
        state.homeColumnCount == AppStore.HOME_COLUMN_COUNT_COMPACT
      binding.homeFiveColumnsSwitch.setOnCheckedChangeListener { _, checked ->
        listener.onHomeColumnCountChanged(
          if (checked) {
            AppStore.HOME_COLUMN_COUNT_COMPACT
          } else {
            AppStore.HOME_COLUMN_COUNT_DEFAULT
          }
        )
      }

      binding.autoDefrostSwitch.setOnCheckedChangeListener(null)
      binding.autoDefrostSwitch.isChecked = state.autoDefrostEnabled
      binding.autoDefrostSwitch.isEnabled = state.canAutoDefrost || state.autoDefrostEnabled
      binding.autoDefrostSwitch.setOnCheckedChangeListener { _, checked ->
        listener.onAutoDefrostChanged(checked)
      }

      binding.shizukuStatusLabel.text = state.statusLabel
      binding.shizukuStatusDetail.text = state.statusDetail
      binding.shizukuStatusLabel.setBackgroundResource(
        if (state.statusReady) R.drawable.bg_status_ready else R.drawable.bg_status_attention
      )
      binding.shizukuStatusLabel.setTextColor(
        ContextCompat.getColor(
          binding.root.context,
          if (state.statusReady) R.color.success else R.color.accent
        )
      )
      binding.shizukuActionButton.text = state.actionLabel
      binding.shizukuActionButton.isEnabled = state.actionEnabled
      binding.shizukuActionButton.alpha = if (state.actionEnabled) 1f else 0.5f
    }
  }

  inner class TargetViewHolder(
    private val binding: ItemSettingsTargetBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(target: SearchTarget) {
      binding.targetName.text = target.name
      val visibilityLabel = binding.root.context.getString(
        if (target.hidden) R.string.target_hidden else R.string.target_visible
      )
      val packageName = target.androidPackageName?.takeIf {
        headerState.packageNamesVisible && it.isNotBlank()
      }
      binding.targetVisibility.text = visibilityLabel
      binding.targetStatus.text = packageName.orEmpty()
      binding.targetStatus.isVisible = packageName != null
      IconLoader.loadInto(binding.targetIcon, target)

      binding.visibleSwitch.setOnCheckedChangeListener(null)
      binding.visibleSwitch.isChecked = !target.hidden
      binding.visibleSwitch.setOnCheckedChangeListener { _, checked ->
        listener.onVisibilityChanged(target, checked)
      }
      binding.editButton.setOnClickListener { listener.onEdit(target) }
      binding.deleteButton.setOnClickListener { listener.onDelete(target) }
      binding.dragHandle.setOnLongClickListener {
        binding.dragHandle.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        listener.onDragRequested(this)
        true
      }
    }
  }

  class FooterViewHolder(binding: ItemSettingsFooterBinding) :
    RecyclerView.ViewHolder(binding.root)

  companion object {
    private const val VIEW_TYPE_HEADER = 0
    private const val VIEW_TYPE_TARGET = 1
    private const val VIEW_TYPE_FOOTER = 2
    private const val VIEW_TYPE_APP_SETTINGS = 3
  }
}
