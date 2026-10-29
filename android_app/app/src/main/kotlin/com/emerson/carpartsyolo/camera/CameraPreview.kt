package com.emerson.carpartsyolo.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emerson.carpartsyolo.ml.DetectionResult
import com.emerson.carpartsyolo.ml.YoloSegmenter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraPreview(
    segmenter: YoloSegmenter,
    confThreshold: Float,
    iouThreshold: Float,
    maskOpacity: Int,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onResult: (DetectionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Mantém referências atualizadas para os sliders sem reiniciar a câmera ao arrastá-los
    val currentConfThreshold by rememberUpdatedState(confThreshold)
    val currentIouThreshold by rememberUpdatedState(iouThreshold)
    val currentMaskOpacity by rememberUpdatedState(maskOpacity)
    val currentOnResult by rememberUpdatedState(onResult)

    val executor = remember { Executors.newSingleThreadExecutor() }
    val isAnalyzing = remember { AtomicBoolean(false) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    DisposableEffect(cameraSelector, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                if (!isAnalyzing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                try {
                    if (segmenter.isModelLoaded) {
                        val bitmap = imageProxy.toBitmap()
                        val rotation = imageProxy.imageInfo.rotationDegrees

                        val rotatedBitmap = if (rotation != 0) {
                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap
                        }

                        val result = segmenter.segment(
                            bitmap = rotatedBitmap,
                            confThreshold = currentConfThreshold,
                            iouThreshold = currentIouThreshold,
                            maskOpacity = currentMaskOpacity
                        )

                        previewView.post {
                            currentOnResult(result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                    isAnalyzing.set(false)
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val provider = cameraProviderFuture.get()
                provider.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
