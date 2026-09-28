package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Helper to persist custom product images picked from device local storage
 * (phones and tablets) permanently into internal app storage.
 */
object ImageStorageHelper {

    /**
     * Copies a user-selected image [sourceUri] into the app's internal "product_images" directory.
     * Returns the absolute file path on success, or null on failure.
     */
    fun copyUriToLocalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "product_images").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "prod_img_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
