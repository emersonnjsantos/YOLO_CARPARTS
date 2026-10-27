package com.emerson.carpartsyolo.ui.main

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import com.emerson.carpartsyolo.ml.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppMode {
    LIVE_CAMERA,
    GALLERY_IMAGE
}

data class AppUiState(
    val mode: AppMode = AppMode.LIVE_CAMERA,
    val isModelLoaded: Boolean = false,
    val modelName: String = "",
    val confThreshold: Float = 0.25f,
    val iouThreshold: Float = 0.45f,
    val maskOpacity: Int = 160,
    val showBoxes: Boolean = true,
    val showLabels: Boolean = true,
    val showMasks: Boolean = true,
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val liveResult: DetectionResult? = null,
    val galleryBitmap: Bitmap? = null,
    val galleryResult: DetectionResult? = null,
    val isProcessingGallery: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showModelPickerSheet: Boolean = false
)

class MainScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun setMode(mode: AppMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setModelStatus(loaded: Boolean, name: String) {
        _uiState.update { it.copy(isModelLoaded = loaded, modelName = name) }
    }

    fun updateLiveResult(result: DetectionResult) {
        _uiState.update { it.copy(liveResult = result) }
    }

    fun setGalleryImage(bitmap: Bitmap?, result: DetectionResult?) {
        _uiState.update {
            it.copy(
                galleryBitmap = bitmap,
                galleryResult = result,
                isProcessingGallery = false
            )
        }
    }

    fun setProcessingGallery(isProcessing: Boolean) {
        _uiState.update { it.copy(isProcessingGallery = isProcessing) }
    }

    fun toggleCamera() {
        _uiState.update {
            val next = if (it.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            it.copy(cameraSelector = next)
        }
    }

    fun updateConfThreshold(value: Float) {
        _uiState.update { it.copy(confThreshold = value) }
    }

    fun updateIouThreshold(value: Float) {
        _uiState.update { it.copy(iouThreshold = value) }
    }

    fun updateMaskOpacity(value: Int) {
        _uiState.update { it.copy(maskOpacity = value) }
    }

    fun toggleBoxes(show: Boolean) {
        _uiState.update { it.copy(showBoxes = show) }
    }

    fun toggleLabels(show: Boolean) {
        _uiState.update { it.copy(showLabels = show) }
    }

    fun toggleMasks(show: Boolean) {
        _uiState.update { it.copy(showMasks = show) }
    }

    fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun showModelPicker(show: Boolean) {
        _uiState.update { it.copy(showModelPickerSheet = show) }
    }
}
