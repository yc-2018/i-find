package com.cgl.ifind.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cgl.ifind.R
import com.cgl.ifind.data.AppStore
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.databinding.ActivitySettingsBinding
import com.cgl.ifind.shizuku.ShizukuBridge
import com.cgl.ifind.shizuku.ShizukuState
import com.cgl.ifind.shizuku.ShizukuStatus

class SettingsActivity : AppCompatActivity(), SettingsAdapterListener {
  private lateinit var binding: ActivitySettingsBinding
  private lateinit var store: AppStore
  private lateinit var settingsAdapter: SettingsAdapter
  private lateinit var itemTouchHelper: ItemTouchHelper
  private lateinit var shizukuBridge: ShizukuBridge
  private var currentStatus: ShizukuStatus? = null
  private var dragChanged = false

  private val editorLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    reloadTargets()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applySystemBarInsets(binding.root)

    store = AppStore(applicationContext)
    shizukuBridge = ShizukuBridge(applicationContext)
    settingsAdapter = SettingsAdapter(this)
    binding.settingsList.apply {
      layoutManager = LinearLayoutManager(this@SettingsActivity)
      adapter = settingsAdapter
      itemAnimator = null
      setHasFixedSize(false)
    }

    itemTouchHelper = ItemTouchHelper(createDragCallback())
    itemTouchHelper.attachToRecyclerView(binding.settingsList)
    reloadTargets()
    updateHeader(currentStatus)
  }

  override fun onStart() {
    super.onStart()
    shizukuBridge.onStart { status ->
      currentStatus = status
      if (status.state != ShizukuState.READY && store.isAutoDefrostEnabled()) {
        store.setAutoDefrostEnabled(false)
      }
      updateHeader(status)
    }
  }

  override fun onResume() {
    super.onResume()
    reloadTargets()
    currentStatus = shizukuBridge.refresh()
    updateHeader(currentStatus)
  }

  override fun onStop() {
    shizukuBridge.onStop()
    super.onStop()
  }

  override fun onPause() {
    persistDraggedOrder()
    super.onPause()
  }

  override fun onDestroy() {
    shizukuBridge.close()
    super.onDestroy()
  }

  override fun onBack() = finish()

  override fun onAdd() {
    editorLauncher.launch(Intent(this, TargetEditorActivity::class.java))
  }

  override fun onRestore() {
    AlertDialog.Builder(this)
      .setMessage(R.string.restore_defaults_confirm)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.restore_defaults) { _, _ ->
        store.restoreDefaults()
        reloadTargets()
        Toast.makeText(this, R.string.defaults_restored, Toast.LENGTH_SHORT).show()
      }
      .show()
  }

  override fun onHistoryChanged(enabled: Boolean) {
    store.setHistoryRecordingEnabled(enabled)
    updateHeader(currentStatus)
  }

  override fun onTargetLabelsChanged(visible: Boolean) {
    store.setTargetLabelsVisible(visible)
    updateHeader(currentStatus)
  }

  override fun onHomeColumnCountChanged(columnCount: Int) {
    store.setHomeColumnCount(columnCount)
    updateHeader(currentStatus)
  }

  override fun onAutoDefrostChanged(enabled: Boolean) {
    if (enabled && currentStatus?.state != ShizukuState.READY) {
      Toast.makeText(this, R.string.auto_defrost_not_ready, Toast.LENGTH_SHORT).show()
      store.setAutoDefrostEnabled(false)
    } else {
      store.setAutoDefrostEnabled(enabled)
    }
    updateHeader(currentStatus)
  }

  override fun onShizukuAction() {
    if (currentStatus?.state == ShizukuState.PERMISSION_REQUIRED) {
      shizukuBridge.requestPermission(this) { result ->
        Toast.makeText(
          this,
          if (result.granted) R.string.permission_granted else R.string.permission_denied,
          Toast.LENGTH_SHORT
        ).show()
        currentStatus = shizukuBridge.refresh()
        updateHeader(currentStatus)
      }
      return
    }

    if (!shizukuBridge.openShizuku(this)) {
      Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onRefreshStatus() {
    currentStatus = shizukuBridge.refresh()
    updateHeader(currentStatus)
  }

  override fun onEdit(target: SearchTarget) {
    editorLauncher.launch(
      Intent(this, TargetEditorActivity::class.java)
        .putExtra(TargetEditorActivity.EXTRA_TARGET_ID, target.id)
    )
  }

  override fun onDelete(target: SearchTarget) {
    AlertDialog.Builder(this)
      .setMessage(getString(R.string.delete_target_confirm, target.name))
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.delete) { _, _ ->
        store.deleteTarget(target.id)
        reloadTargets()
      }
      .show()
  }

  override fun onVisibilityChanged(target: SearchTarget, visible: Boolean) {
    val nextTargets = store.getTargets().map {
      if (it.id == target.id) it.copy(hidden = !visible) else it
    }
    store.saveTargets(nextTargets)
    reloadTargets()
  }

  override fun onDragRequested(holder: RecyclerView.ViewHolder) {
    if (!settingsAdapter.isTargetPosition(holder.bindingAdapterPosition)) return
    binding.settingsList.stopScroll()
    dragChanged = false
    itemTouchHelper.startDrag(holder)
  }

  private fun reloadTargets() {
    settingsAdapter.submitTargets(store.getTargets())
  }

  private fun updateHeader(status: ShizukuStatus?) {
    val state = status?.state
    val statusLabel = getString(
      when (state) {
        ShizukuState.NOT_INSTALLED -> R.string.status_not_installed
        ShizukuState.SERVICE_NOT_RUNNING -> R.string.status_service_stopped
        ShizukuState.PERMISSION_REQUIRED -> R.string.status_permission_required
        ShizukuState.READY -> R.string.status_ready
        null -> R.string.status_checking
      }
    )
    val statusDetail = getString(
      when (state) {
        ShizukuState.NOT_INSTALLED -> R.string.status_not_installed_detail
        ShizukuState.SERVICE_NOT_RUNNING -> R.string.status_service_stopped_detail
        ShizukuState.PERMISSION_REQUIRED -> R.string.status_permission_required_detail
        ShizukuState.READY -> R.string.status_ready_detail
        null -> R.string.status_unavailable_detail
      }
    )
    val actionLabel = getString(
      if (state == ShizukuState.PERMISSION_REQUIRED) {
        R.string.grant_permission
      } else {
        R.string.open_shizuku
      }
    )
    val actionEnabled = state != null && state != ShizukuState.NOT_INSTALLED

    settingsAdapter.updateHeader(
      SettingsHeaderState(
        historyEnabled = store.isHistoryRecordingEnabled(),
        targetLabelsVisible = store.areTargetLabelsVisible(),
        homeColumnCount = store.getHomeColumnCount(),
        autoDefrostEnabled = store.isAutoDefrostEnabled(),
        canAutoDefrost = state == ShizukuState.READY,
        statusLabel = statusLabel,
        statusDetail = statusDetail,
        statusReady = state == ShizukuState.READY,
        packageNamesVisible = status?.serviceRunning == true,
        actionLabel = actionLabel,
        actionEnabled = actionEnabled
      )
    )
  }

  private fun createDragCallback(): ItemTouchHelper.Callback {
    return object : ItemTouchHelper.SimpleCallback(
      ItemTouchHelper.UP or ItemTouchHelper.DOWN,
      0
    ) {
      override fun isLongPressDragEnabled(): Boolean = false

      override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
      ): Int {
        if (!settingsAdapter.isTargetPosition(viewHolder.bindingAdapterPosition)) {
          return makeMovementFlags(0, 0)
        }
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
      }

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        val moved = settingsAdapter.moveTarget(
          viewHolder.bindingAdapterPosition,
          target.bindingAdapterPosition
        )
        dragChanged = dragChanged || moved
        return moved
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

      override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
          binding.settingsList.stopScroll()
          viewHolder?.itemView?.apply {
            alpha = 0.92f
            scaleX = 1.02f
            scaleY = 1.02f
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
          elevation = 0f
        }
        recyclerView.stopScroll()
        persistDraggedOrder()
      }

      override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long
      ): Int {
        val maxStep = (10 * recyclerView.resources.displayMetrics.density).toInt()
        return viewSizeOutOfBounds.coerceIn(-maxStep, maxStep)
      }
    }
  }

  private fun persistDraggedOrder() {
    if (!dragChanged) return
    store.saveTargets(settingsAdapter.snapshotTargets())
    dragChanged = false
  }
}
