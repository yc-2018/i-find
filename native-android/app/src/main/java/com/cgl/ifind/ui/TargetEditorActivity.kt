package com.cgl.ifind.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.cgl.ifind.R
import com.cgl.ifind.data.AppStore
import com.cgl.ifind.data.DefaultTargets
import com.cgl.ifind.data.IconModes
import com.cgl.ifind.data.SearchTarget
import com.cgl.ifind.databinding.ActivityTargetEditorBinding
import com.cgl.ifind.shizuku.ShizukuBridge
import com.cgl.ifind.util.IconFileStore
import com.cgl.ifind.util.IconLoader
import com.cgl.ifind.util.RemoteIconCache
import java.io.File
import java.util.UUID

class TargetEditorActivity : AppCompatActivity() {
  private lateinit var binding: ActivityTargetEditorBinding
  private lateinit var store: AppStore
  private lateinit var shizukuBridge: ShizukuBridge
  private var existingTarget: SearchTarget? = null
  private var selectedIconMode = IconModes.GENERATED
  private var builtinIconValue = DefaultTargets.builtinIconChoices.first().key
  private var installedAppIconValue = ""
  private var galleryIconValue = ""
  private var remoteIconUrl = ""
  private var unsavedGalleryPath: String? = null
  private var saved = false

  private val galleryLauncher = registerForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri == null) return@registerForActivityResult
    runCatching { IconFileStore.copyToPrivateStorage(this, uri) }
      .onSuccess { path ->
        discardUnsavedGallery()
        unsavedGalleryPath = path
        galleryIconValue = path
        selectedIconMode = IconModes.GALLERY
        binding.galleryIconRadio.isChecked = true
        updateIconModeUi()
      }
      .onFailure {
        Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
      }
  }

  private val installedAppLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
    val appLabel = result.data?.getStringExtra(InstalledAppPickerActivity.EXTRA_APP_LABEL).orEmpty()
    val packageName = result.data?.getStringExtra(InstalledAppPickerActivity.EXTRA_PACKAGE_NAME).orEmpty()
    if (appLabel.isBlank() || packageName.isBlank()) return@registerForActivityResult

    installedAppIconValue = packageName
    selectedIconMode = IconModes.INSTALLED_APP
    binding.installedAppIconRadio.isChecked = true
    binding.nameInput.setText(appLabel)
    binding.packageInput.setText(packageName)
    updateIconModeUi()
    Toast.makeText(this, getString(R.string.app_selected, appLabel), Toast.LENGTH_SHORT).show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityTargetEditorBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applySystemBarInsets(binding.root)

    store = AppStore(applicationContext)
    shizukuBridge = ShizukuBridge(applicationContext)
    existingTarget = intent.getStringExtra(EXTRA_TARGET_ID)?.let(store::getTarget)

    populateForm()
    setupActions()
    updateIconModeUi()
  }

  override fun onStart() {
    super.onStart()
    shizukuBridge.onStart { status ->
      binding.packageSection.isVisible = status.serviceRunning
    }
  }

  override fun onStop() {
    shizukuBridge.onStop()
    super.onStop()
  }

  override fun onDestroy() {
    if (!saved) discardUnsavedGallery()
    shizukuBridge.close()
    super.onDestroy()
  }

  private fun populateForm() {
    val target = existingTarget
    binding.titleText.setText(
      if (target == null) R.string.target_editor_new else R.string.target_editor_edit
    )

    if (target == null) {
      selectedIconMode = IconModes.GENERATED
      binding.generatedIconRadio.isChecked = true
      return
    }

    selectedIconMode = target.iconMode.takeIf { it in SUPPORTED_ICON_MODES }
      ?: IconModes.GENERATED
    binding.nameInput.setText(target.name)
    binding.packageInput.setText(target.androidPackageName.orEmpty())
    binding.primaryInput.setText(target.primaryTemplate)
    binding.fallbackInput.setText(target.fallbackTemplate.orEmpty())

    when (selectedIconMode) {
      IconModes.BUILTIN -> {
        builtinIconValue = target.iconValue
        binding.builtinIconRadio.isChecked = true
      }
      IconModes.INSTALLED_APP -> {
        installedAppIconValue = target.iconValue
        binding.installedAppIconRadio.isChecked = true
      }
      IconModes.GALLERY -> {
        galleryIconValue = target.iconValue
        binding.galleryIconRadio.isChecked = true
      }
      IconModes.REMOTE -> {
        remoteIconUrl = target.iconValue
        binding.remoteIconInput.setText(remoteIconUrl)
        binding.remoteIconRadio.isChecked = true
      }
      else -> binding.generatedIconRadio.isChecked = true
    }
  }

  private fun setupActions() {
    binding.backButton.setOnClickListener { finish() }
    binding.nameInput.doAfterTextChanged {
      if (selectedIconMode == IconModes.GENERATED || selectedIconMode == IconModes.REMOTE) {
        renderIconPreview()
      }
    }
    binding.remoteIconInput.doAfterTextChanged {
      remoteIconUrl = it?.toString()?.trim().orEmpty()
      binding.remoteIconInput.error = null
    }
    binding.iconSourceGroup.setOnCheckedChangeListener { _, checkedId ->
      selectedIconMode = when (checkedId) {
        R.id.builtinIconRadio -> IconModes.BUILTIN
        R.id.installedAppIconRadio -> IconModes.INSTALLED_APP
        R.id.galleryIconRadio -> IconModes.GALLERY
        R.id.remoteIconRadio -> IconModes.REMOTE
        else -> IconModes.GENERATED
      }
      updateIconModeUi()
    }
    binding.iconPreviewButton.setOnClickListener { handleIconPreviewClick() }
    binding.saveButton.setOnClickListener { saveTarget() }
  }

  private fun handleIconPreviewClick() {
    when (selectedIconMode) {
      IconModes.BUILTIN -> showBuiltinIconPicker()
      IconModes.INSTALLED_APP -> {
        installedAppLauncher.launch(Intent(this, InstalledAppPickerActivity::class.java))
      }
      IconModes.GALLERY -> galleryLauncher.launch("image/*")
      IconModes.REMOTE -> {
        remoteIconUrl = binding.remoteIconInput.text.toString().trim()
        if (!RemoteIconCache.isSupportedUrl(remoteIconUrl)) {
          binding.remoteIconInput.error = getString(R.string.invalid_remote_icon_url)
          binding.remoteIconInput.requestFocus()
          return
        }
        renderIconPreview()
      }
      IconModes.GENERATED -> Unit
    }
  }

  private fun showBuiltinIconPicker() {
    BuiltinIconPickerDialog.show(this, builtinIconValue) { iconValue ->
      builtinIconValue = iconValue
      selectedIconMode = IconModes.BUILTIN
      binding.builtinIconRadio.isChecked = true
      renderIconPreview()
    }
  }

  private fun updateIconModeUi() {
    binding.remoteIconSection.isVisible = selectedIconMode == IconModes.REMOTE
    binding.iconActionArrow.isVisible = selectedIconMode != IconModes.GENERATED
    binding.iconPreviewButton.isClickable = selectedIconMode != IconModes.GENERATED
    binding.iconPreviewButton.isFocusable = selectedIconMode != IconModes.GENERATED

    val titleRes = when (selectedIconMode) {
      IconModes.BUILTIN -> R.string.choose_builtin_icon
      IconModes.INSTALLED_APP -> R.string.choose_installed_app_icon
      IconModes.GALLERY -> R.string.choose_gallery_icon
      IconModes.REMOTE -> R.string.load_remote_icon
      else -> R.string.generated_icon_ready
    }
    val summaryRes = when (selectedIconMode) {
      IconModes.GENERATED -> R.string.generated_icon_summary
      IconModes.REMOTE -> R.string.remote_icon_summary
      else -> R.string.icon_action_summary
    }
    binding.iconActionTitle.setText(titleRes)
    binding.iconActionSummary.setText(summaryRes)
    renderIconPreview()
  }

  private fun renderIconPreview() {
    val name = binding.nameInput.text?.toString()?.trim().orEmpty().ifBlank { "I" }
    val previewTarget = SearchTarget(
      id = "preview",
      name = name,
      primaryTemplate = "https://example.com/?q={keyword}",
      iconMode = selectedIconMode,
      iconValue = currentIconValue(name),
      hidden = false,
      sortOrder = 0
    )
    IconLoader.loadInto(binding.iconPreview, previewTarget)
  }

  private fun currentIconValue(name: String): String {
    return when (selectedIconMode) {
      IconModes.BUILTIN -> builtinIconValue
      IconModes.INSTALLED_APP -> installedAppIconValue.ifBlank {
        binding.packageInput.text?.toString()?.trim().orEmpty()
      }
      IconModes.GALLERY -> galleryIconValue
      IconModes.REMOTE -> remoteIconUrl
      else -> name
    }
  }

  private fun saveTarget() {
    binding.nameInput.error = null
    binding.packageInput.error = null
    binding.primaryInput.error = null
    binding.fallbackInput.error = null
    binding.remoteIconInput.error = null

    val name = binding.nameInput.text.toString().trim()
    val packageName = binding.packageInput.text.toString().trim()
    val primaryTemplate = binding.primaryInput.text.toString().trim()
    val fallbackTemplate = binding.fallbackInput.text.toString().trim()

    if (name.isEmpty()) {
      binding.nameInput.error = getString(R.string.name_required)
      binding.nameInput.requestFocus()
      return
    }
    if (primaryTemplate.isEmpty()) {
      binding.primaryInput.error = getString(R.string.primary_template_required)
      binding.primaryInput.requestFocus()
      return
    }
    if (!primaryTemplate.contains(KEYWORD_PLACEHOLDER)) {
      binding.primaryInput.error = getString(R.string.template_keyword_required)
      binding.primaryInput.requestFocus()
      return
    }
    if (fallbackTemplate.isNotEmpty() && !fallbackTemplate.contains(KEYWORD_PLACEHOLDER)) {
      binding.fallbackInput.error = getString(R.string.template_keyword_required)
      binding.fallbackInput.requestFocus()
      return
    }
    if (packageName.isNotEmpty() && !PACKAGE_NAME_PATTERN.matches(packageName)) {
      binding.packageInput.error = getString(R.string.invalid_package)
      binding.packageInput.requestFocus()
      return
    }
    if (selectedIconMode == IconModes.GALLERY && galleryIconValue.isBlank()) {
      Toast.makeText(this, R.string.icon_selection_required, Toast.LENGTH_SHORT).show()
      return
    }
    if (selectedIconMode == IconModes.INSTALLED_APP &&
      installedAppIconValue.isBlank() && packageName.isBlank()
    ) {
      Toast.makeText(this, R.string.icon_selection_required, Toast.LENGTH_SHORT).show()
      return
    }
    remoteIconUrl = binding.remoteIconInput.text.toString().trim()
    if (selectedIconMode == IconModes.REMOTE && !RemoteIconCache.isSupportedUrl(remoteIconUrl)) {
      binding.remoteIconInput.error = getString(R.string.invalid_remote_icon_url)
      binding.remoteIconInput.requestFocus()
      return
    }

    val previous = existingTarget
    val target = SearchTarget(
      id = previous?.id ?: "target-${System.currentTimeMillis()}-${UUID.randomUUID()}",
      name = name,
      primaryTemplate = primaryTemplate,
      fallbackTemplate = fallbackTemplate.takeIf { it.isNotBlank() },
      androidPackageName = packageName.takeIf { it.isNotBlank() },
      iconMode = selectedIconMode,
      iconValue = currentIconValue(name),
      hidden = previous?.hidden ?: false,
      sortOrder = previous?.sortOrder ?: store.getTargets().size
    )

    store.saveTarget(target)
    if (target.iconMode == IconModes.GALLERY && target.iconValue == unsavedGalleryPath) {
      unsavedGalleryPath = null
    } else {
      discardUnsavedGallery()
    }
    saved = true
    setResult(RESULT_OK)
    Toast.makeText(this, R.string.search_item_saved, Toast.LENGTH_SHORT).show()
    finish()
  }

  private fun discardUnsavedGallery() {
    unsavedGalleryPath?.let { File(it).delete() }
    unsavedGalleryPath = null
  }

  companion object {
    const val EXTRA_TARGET_ID = "target_id"
    private const val KEYWORD_PLACEHOLDER = "{keyword}"
    private val SUPPORTED_ICON_MODES = setOf(
      IconModes.BUILTIN,
      IconModes.INSTALLED_APP,
      IconModes.GALLERY,
      IconModes.GENERATED,
      IconModes.REMOTE
    )
    private val PACKAGE_NAME_PATTERN =
      Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
  }
}
