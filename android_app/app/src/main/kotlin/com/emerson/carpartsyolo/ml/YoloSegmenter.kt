package com.emerson.carpartsyolo.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class YoloSegmenter(private val context: Context) : AutoCloseable {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    // Dimensões do tensor de entrada
    var inputWidth = 640
        private set
    var inputHeight = 640
        private set
    private var isNCHW = false

    // Formato do tensor 0 (detecções): [1, 59, 8400] ou [1, 8400, 59]
    private var numChannels0 = 59
    private var numAnchors0 = 8400
    private var isTransposed0 = false // true se [1, 8400, 59]

    // Formato do tensor 1 (máscaras proto): [1, 32, 160, 160] ou [1, 160, 160, 32]
    private var protoChannels = 32
    private var protoHeight = 160
    private var protoWidth = 160
    private var isProtoNCHW = true

    val isModelLoaded: Boolean get() = interpreter != null
    var currentModelName: String = ""
        private set

    /**
     * Tenta carregar o modelo de um dos assets conhecidos ou de um arquivo externo.
     */
    fun loadDefaultModel(): Boolean {
        val candidates = listOf("best.tflite", "carparts_yolo26n.tflite", "model.tflite")
        for (name in candidates) {
            try {
                if (loadFromAssets(name)) return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    fun loadFromAssets(assetName: String): Boolean {
        close()
        return try {
            val fileDescriptor = context.assets.openFd(assetName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            initInterpreter(buffer, assetName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadFromFile(file: File): Boolean {
        close()
        return try {
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            initInterpreter(buffer, file.name)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadFromByteBuffer(buffer: ByteBuffer, name: String = "custom.tflite"): Boolean {
        close()
        return try {
            initInterpreter(buffer, name)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun initInterpreter(buffer: ByteBuffer, modelName: String) {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            try {
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    val delegate = GpuDelegate()
                    gpuDelegate = delegate
                    addDelegate(delegate)
                }
            } catch (_: Throwable) {
                gpuDelegate = null
            }
        }

        val interp = Interpreter(buffer, options)
        this.interpreter = interp
        this.currentModelName = modelName

        // Inspeciona formatos de entrada
        val inShape = interp.getInputTensor(0).shape() // ex: [1, 3, 640, 640] ou [1, 640, 640, 3]
        if (inShape.size == 4) {
            if (inShape[1] == 3) {
                isNCHW = true
                inputHeight = inShape[2]
                inputWidth = inShape[3]
            } else {
                isNCHW = false
                inputHeight = inShape[1]
                inputWidth = inShape[2]
            }
        }

        // Inspeciona saída 0 (Boxes, classes, mask coeffs)
        val out0Shape = interp.getOutputTensor(0).shape()
        if (out0Shape.size == 3) {
            if (out0Shape[1] < out0Shape[2]) {
                // [1, 59, 8400]
                numChannels0 = out0Shape[1]
                numAnchors0 = out0Shape[2]
                isTransposed0 = false
            } else {
                // [1, 8400, 59]
                numAnchors0 = out0Shape[1]
                numChannels0 = out0Shape[2]
                isTransposed0 = true
            }
        }

        // Inspeciona saída 1 (Protótipos de máscaras de segmentação)
        if (interp.outputTensorCount > 1) {
            val out1Shape = interp.getOutputTensor(1).shape()
            if (out1Shape.size == 4) {
                if (out1Shape[1] == 32) {
                    // [1, 32, 160, 160] (NCHW)
                    isProtoNCHW = true
                    protoChannels = out1Shape[1]
                    protoHeight = out1Shape[2]
                    protoWidth = out1Shape[3]
                } else {
                    // [1, 160, 160, 32] (NHWC)
                    isProtoNCHW = false
                    protoHeight = out1Shape[1]
                    protoWidth = out1Shape[2]
                    protoChannels = out1Shape[3]
                }
            }
        }
    }

    /**
     * Executa a inferência YOLO em um Bitmap de qualquer dimensão.
     */
    fun segment(
        bitmap: Bitmap,
        confThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f,
        maskOpacity: Int = 140
    ): DetectionResult {
        val interp = interpreter ?: return DetectionResult(emptyList(), 0, 0, 0, bitmap.width, bitmap.height)

        val t0 = SystemClock.uptimeMillis()

        // 1. Pré-processamento: Redimensiona para 640x640 mantendo proporções (Letterbox)
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = min(inputWidth.toFloat() / originalWidth, inputHeight.toFloat() / originalHeight)
        val scaledW = (originalWidth * scale).toInt()
        val scaledH = (originalHeight * scale).toInt()
        val padX = (inputWidth - scaledW) / 2f
        val padY = (inputHeight - scaledH) / 2f

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        val letterboxBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(letterboxBitmap)
        canvas.drawColor(Color.rgb(114, 114, 114)) // Padrão YOLO cinza 114
        canvas.drawBitmap(resizedBitmap, padX, padY, null)

        // Preenche o buffer de entrada Float32 normalizado [0..1]
        val byteBuffer = ByteBuffer.allocateDirect(1 * 3 * inputWidth * inputHeight * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(inputWidth * inputHeight)
        letterboxBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        if (isNCHW) {
            // Plano R, depois G, depois B
            val totalPixels = inputWidth * inputHeight
            for (p in pixels) {
                byteBuffer.putFloat(((p shr 16) and 0xFF) / 255.0f)
            }
            for (p in pixels) {
                byteBuffer.putFloat(((p shr 8) and 0xFF) / 255.0f)
            }
            for (p in pixels) {
                byteBuffer.putFloat((p and 0xFF) / 255.0f)
            }
        } else {
            // Intercalado RGB
            for (p in pixels) {
                byteBuffer.putFloat(((p shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((p shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((p and 0xFF) / 255.0f)
            }
        }

        val t1 = SystemClock.uptimeMillis()

        // 2. Inferência TFLite
        val output0 = Array(1) { Array(numChannels0) { FloatArray(numAnchors0) } }
        val output1 = Array(1) { Array(protoChannels) { Array(protoHeight) { FloatArray(protoWidth) } } }

        val outputs = mutableMapOf<Int, Any>()
        if (isTransposed0) {
            // [1, 8400, 59]
            val transposedOut0 = Array(1) { Array(numAnchors0) { FloatArray(numChannels0) } }
            outputs[0] = transposedOut0
            if (interp.outputTensorCount > 1) outputs[1] = output1
            interp.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputs)

            // Desvirar para acesso rápido out0[canal][âncora]
            for (a in 0 until numAnchors0) {
                for (c in 0 until numChannels0) {
                    output0[0][c][a] = transposedOut0[0][a][c]
                }
            }
        } else {
            outputs[0] = output0
            if (interp.outputTensorCount > 1) outputs[1] = output1
            interp.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputs)
        }

        val t2 = SystemClock.uptimeMillis()

        // 3. Pós-processamento e NMS
        val numClasses = 23
        val rawDetections = mutableListOf<RawDetection>()

        for (a in 0 until numAnchors0) {
            // Determina a classe de maior probabilidade entre 4 e 26
            var maxScore = 0f
            var bestClass = -1
            for (c in 0 until numClasses) {
                val score = output0[0][4 + c][a]
                if (score > maxScore) {
                    maxScore = score
                    bestClass = c
                }
            }

            if (maxScore >= confThreshold && bestClass >= 0) {
                var cx = output0[0][0][a]
                var cy = output0[0][1][a]
                var w = output0[0][2][a]
                var h = output0[0][3][a]

                // Normaliza se veio em coordenadas de pixel 0..640
                if (cx > 1.5f || cy > 1.5f || w > 1.5f || h > 1.5f) {
                    cx /= inputWidth
                    cy /= inputHeight
                    w /= inputWidth
                    h /= inputHeight
                }

                // Desfaz o letterbox para retornar às coordenadas da imagem original [0..1]
                val origLeft = (((cx - w / 2f) * inputWidth - padX) / scaledW).coerceIn(0f, 1f)
                val origTop = (((cy - h / 2f) * inputHeight - padY) / scaledH).coerceIn(0f, 1f)
                val origRight = (((cx + w / 2f) * inputWidth - padX) / scaledW).coerceIn(0f, 1f)
                val origBottom = (((cy + h / 2f) * inputHeight - padY) / scaledH).coerceIn(0f, 1f)

                // Coeficientes da máscara (32 floats a partir do canal 27)
                val maskCoeffs = FloatArray(protoChannels)
                for (k in 0 until protoChannels) {
                    maskCoeffs[k] = output0[0][4 + numClasses + k][a]
                }

                // Coordenadas relativas ao frame 640x640 para recorte no mapa proto
                val protoBoxLeft = ((cx - w / 2f) * protoWidth).coerceIn(0f, protoWidth - 1f)
                val protoBoxTop = ((cy - h / 2f) * protoHeight).coerceIn(0f, protoHeight - 1f)
                val protoBoxRight = ((cx + w / 2f) * protoWidth).coerceIn(0f, protoWidth - 1f)
                val protoBoxBottom = ((cy + h / 2f) * protoHeight).coerceIn(0f, protoHeight - 1f)

                rawDetections.add(
                    RawDetection(
                        classId = bestClass,
                        confidence = maxScore,
                        box = BoundingBox(origLeft, origTop, origRight, origBottom),
                        protoBox = BoundingBox(protoBoxLeft, protoBoxTop, protoBoxRight, protoBoxBottom),
                        maskCoeffs = maskCoeffs
                    )
                )
            }
        }

        // Aplica NMS
        val nmsDetections = applyNMS(rawDetections, iouThreshold)

        // 4. Reconstrói máscaras para os itens retidos no NMS
        val segmentedParts = mutableListOf<SegmentedPart>()
        for (det in nmsDetections) {
            val partClass = CarPartsCatalog.get(det.classId)
            val maskBitmap = generateMaskBitmap(
                protoTensor = output1[0],
                coeffs = det.maskCoeffs,
                protoBox = det.protoBox,
                color = partClass.color,
                alpha = maskOpacity
            )

            segmentedParts.add(
                SegmentedPart(
                    partClass = partClass,
                    confidence = det.confidence,
                    box = det.box,
                    maskBitmap = maskBitmap
                )
            )
        }

        val t3 = SystemClock.uptimeMillis()

        return DetectionResult(
            parts = segmentedParts,
            inferenceTimeMs = t2 - t1,
            preprocessTimeMs = t1 - t0,
            postprocessTimeMs = t3 - t2,
            imageWidth = originalWidth,
            imageHeight = originalHeight
        )
    }

    private fun generateMaskBitmap(
        protoTensor: Array<Array<FloatArray>>, // [32][160][160]
        coeffs: FloatArray,
        protoBox: BoundingBox,
        color: Int,
        alpha: Int
    ): Bitmap {
        val maskBmp = Bitmap.createBitmap(protoWidth, protoHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(protoWidth * protoHeight)

        val minX = protoBox.left.toInt()
        val maxX = protoBox.right.toInt()
        val minY = protoBox.top.toInt()
        val maxY = protoBox.bottom.toInt()

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maskColor = Color.argb(alpha, r, g, b)

        for (y in minY..maxY) {
            val rowOffset = y * protoWidth
            for (x in minX..maxX) {
                var sum = 0f
                for (k in 0 until protoChannels) {
                    sum += coeffs[k] * protoTensor[k][y][x]
                }
                // Sigmoid
                val sigmoid = 1.0f / (1.0f + exp(-sum))
                if (sigmoid > 0.5f) {
                    pixels[rowOffset + x] = maskColor
                }
            }
        }

        maskBmp.setPixels(pixels, 0, protoWidth, 0, 0, protoWidth, protoHeight)
        return maskBmp
    }

    private fun applyNMS(boxes: List<RawDetection>, iouThreshold: Float): List<RawDetection> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<RawDetection>()

        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected.add(current)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (current.classId == next.classId && computeIoU(current.box, next.box) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selected
    }

    private fun computeIoU(b1: BoundingBox, b2: BoundingBox): Float {
        val interLeft = max(b1.left, b2.left)
        val interTop = max(b1.top, b2.top)
        val interRight = min(b1.right, b2.right)
        val interBottom = min(b1.bottom, b2.bottom)

        val interW = max(0f, interRight - interLeft)
        val interH = max(0f, interBottom - interTop)
        val interArea = interW * interH

        val area1 = b1.width * b1.height
        val area2 = b2.width * b2.height
        val unionArea = area1 + area2 - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
    }

    private data class RawDetection(
        val classId: Int,
        val confidence: Float,
        val box: BoundingBox,
        val protoBox: BoundingBox,
        val maskCoeffs: FloatArray
    )
}
