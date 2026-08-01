package com.cgl.ifind.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object IconFileStore {
  fun copyToPrivateStorage(context: Context, sourceUri: Uri): String {
    val iconDirectory = File(context.filesDir, "icons").apply { mkdirs() }
    val extension = when (context.contentResolver.getType(sourceUri)) {
      "image/png" -> "png"
      "image/webp" -> "webp"
      else -> "jpg"
    }
    val targetFile = File(iconDirectory, "icon-${UUID.randomUUID()}.$extension")

    context.contentResolver.openInputStream(sourceUri).use { input ->
      requireNotNull(input) { "Unable to read selected image." }
      targetFile.outputStream().use { output -> input.copyTo(output) }
    }

    return targetFile.absolutePath
  }
}
