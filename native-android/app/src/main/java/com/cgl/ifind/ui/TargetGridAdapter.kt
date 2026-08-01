package com.cgl.ifind.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.databinding.ItemSearchTargetBinding
import com.cgl.ifind.util.IconLoader

class TargetGridAdapter(
  private val onTargetClick: (SearchTarget) -> Unit
) : RecyclerView.Adapter<TargetGridAdapter.TargetViewHolder>() {
  private val targets = mutableListOf<SearchTarget>()
  private var showLabels = true

  init {
    setHasStableIds(true)
  }

  fun submitTargets(nextTargets: List<SearchTarget>, nextShowLabels: Boolean) {
    targets.clear()
    targets.addAll(nextTargets)
    showLabels = nextShowLabels
    notifyDataSetChanged()
  }

  fun moveTarget(fromPosition: Int, toPosition: Int): Boolean {
    if (fromPosition !in targets.indices || toPosition !in targets.indices) return false
    if (fromPosition == toPosition) return false
    val target = targets.removeAt(fromPosition)
    targets.add(toPosition, target)
    notifyItemMoved(fromPosition, toPosition)
    return true
  }

  fun snapshotTargetIds(): List<String> = targets.map { it.id }

  override fun getItemId(position: Int): Long = targets[position].id.hashCode().toLong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TargetViewHolder {
    val binding = ItemSearchTargetBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return TargetViewHolder(binding)
  }

  override fun onBindViewHolder(holder: TargetViewHolder, position: Int) {
    holder.bind(targets[position])
  }

  override fun getItemCount(): Int = targets.size

  inner class TargetViewHolder(
    private val binding: ItemSearchTargetBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(target: SearchTarget) {
      binding.targetName.text = target.name
      binding.root.labelsVisible = showLabels
      binding.targetName.isVisible = showLabels
      IconLoader.loadInto(binding.targetIcon, target)
      binding.root.setOnClickListener { onTargetClick(target) }
    }
  }
}
