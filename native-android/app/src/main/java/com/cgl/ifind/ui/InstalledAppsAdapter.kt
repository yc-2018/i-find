package com.cgl.ifind.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.data.InstalledAppInfo
import com.cgl.ifind.databinding.ItemInstalledAppBinding
import com.cgl.ifind.util.IconLoader

class InstalledAppsAdapter(
  private val onSelect: (InstalledAppInfo) -> Unit
) : RecyclerView.Adapter<InstalledAppsAdapter.AppViewHolder>() {
  private var items: List<InstalledAppInfo> = emptyList()

  init {
    setHasStableIds(true)
  }

  fun submitItems(nextItems: List<InstalledAppInfo>) {
    items = nextItems
    notifyDataSetChanged()
  }

  override fun getItemId(position: Int): Long = items[position].packageName.hashCode().toLong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
    return AppViewHolder(
      ItemInstalledAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
  }

  override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size

  inner class AppViewHolder(
    private val binding: ItemInstalledAppBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: InstalledAppInfo) {
      binding.appLabel.text = item.label
      binding.packageName.text = item.packageName
      binding.appIcon.setImageDrawable(
        IconLoader.loadInstalledAppIcon(binding.root.context, item.packageName)
      )
      binding.root.setOnClickListener { onSelect(item) }
    }
  }
}
