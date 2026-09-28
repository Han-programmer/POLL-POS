package com.example.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reusable wrapper for an [ActivityResultContracts.PickVisualMedia] launcher
 * configured specifically for local product images.
 */
class ProductImagePickerLauncher(
    private val launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
) {
    /**
     * Opens the device Photo Picker to select an image from local storage.
     */
    fun launch() {
        launcher.launch(ProductImagePickerHelper.createImageRequest())
    }
}

/**
 * Helper utility for selecting product images from local device storage using
 * [ActivityResultContracts.PickVisualMedia].
 */
object ProductImagePickerHelper {

    /**
     * Creates an [ActivityResultContracts.PickVisualMedia] contract instance.
     */
    fun createContract(): ActivityResultContracts.PickVisualMedia {
        return ActivityResultContracts.PickVisualMedia()
    }

    /**
     * Creates a [PickVisualMediaRequest] targeting only image media types.
     */
    fun createImageRequest(): PickVisualMediaRequest {
        return PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    }

    /**
     * Checks if the Android Photo Picker is supported and available on this device.
     */
    fun isPhotoPickerAvailable(context: Context): Boolean {
        return ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
    }
}

/**
 * Composable helper utility that initializes an [ActivityResultContracts.PickVisualMedia] launcher,
 * intercepts the picked [Uri], copies it to the app's persistent internal storage, and notifies
 * [onImageSelected] with the permanent local file path.
 *
 * @param onImageSelected Callback invoked with the persistent local image path on device storage.
 */
@Composable
fun rememberProductImagePicker(
    onImageSelected: (String) -> Unit
): ProductImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = ImageStorageHelper.copyUriToLocalStorage(context, uri)
            if (localPath != null) {
                onImageSelected(localPath)
            }
        }
    }

    return remember(launcher) {
        ProductImagePickerLauncher(launcher)
    }
}

/**
 * Composable helper utility that initializes an [ActivityResultContracts.PickVisualMedia] launcher
 * and returns the raw selected [Uri] directly.
 *
 * @param onUriSelected Callback invoked with the selected [Uri], or null if cancelled.
 */
@Composable
fun rememberRawProductImagePicker(
    onUriSelected: (Uri?) -> Unit
): ProductImagePickerLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onUriSelected(uri)
    }

    return remember(launcher) {
        ProductImagePickerLauncher(launcher)
    }
}
