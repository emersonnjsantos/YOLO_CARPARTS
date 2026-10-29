package com.emerson.carpartsyolo.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class YoloSegmenter(private val context: Context) : AutoCloseable {

    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    // Dimensões do tensor de entrada
    var inputWidth = 640
        private set
    var inputHeight = 640
        private set
    private var isNCHW = true

    // Formato do tensor 0 (detecções): [1, 59, 8400] ou [1, 8400, 59]
    private var numChannels0 = 59
    private var numAnchors0 = 8400
    private var isTransposed0 = false // true se [1, 8400, 59]

    // Formato do tensor 1 (máscaras proto): [1, 32, 160, 160] ou [1, 160, 160, 32]
    private var protoChannels = 32
    private var protoHeight = 160
    private var protoWidth = 160
    private var isProtoNCHW = true

    // Buffers reutilizáveis para evitar alocações e GC a cada frame
    private var inputByteBuffer: ByteBuffer? = null
    private var inputPixels: IntArray? = null
    private var letterboxBitmap: Bitmap? = null
    private var letterboxCanvas: android.graphics.Canvas? = null
    private var output0ByteBuffer: ByteBuffer? = null
    private var output0Floats: FloatArray? = null
    private var output1ByteBuffer: ByteBuffer? = null
    private var output1Floats: FloatArray? = null

    val isModelLoaded: Boolean
        get() = synchronized(lock) { interpreter != null }

    var currentModelName: String = ""
        private set

    /**
     * Tenta carregar o modelo de um dos assets conhecidos.
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
        return synchronized(lock) {
            closeInternal()
            try {
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
    }

    fun loadFromFile(file: File): Boolean {
        return synchronized(lock) {
            closeInternal()
            try {
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
    }

    fun loadFromByteBuffer(buffer: ByteBuffer, name: String = "custom.tflite"): Boolean {
        return synchronized(lock) {
            closeInternal()
            try {
                initInterpreter(buffer, name)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun initInterpreter(buffer: ByteBuffer, modelName: String) {
        var interp: Interpreter? = null

        // Tenta inicializar com GPU Delegate; caso o grafo tenha ops incompatíveis, faz fallback seguro para CPU + XNNPACK
        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegate = GpuDelegate()
                val gpuOptions = Interpreter.Options().apply {
                    setNumThreads(4)
                    addDelegate(delegate)
                }
                buffer.rewind()
                interp = Interpreter(buffer, gpuOptions)
                gpuDelegate = delegate
            }
        } catch (_: Throwable) {
            try {
                gpuDelegate?.close()
            } catch (_: Throwable) {
            }
            gpuDelegate = null
            interp = null
        }

        if (interp == null) {
            val cpuOptions = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            buffer.rewind()
            interp = Interpreter(buffer, cpuOptions)
        }

        this.interpreter = interp
        this.currentModelName = modelName

        // Inspeciona formato de entrada: [1, 3, 640, 640] (NCHW) ou [1, 640, 640, 3] (NHWC)
        val inShape = interp.getInputTensor(0).shape()
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

        // Pré-aloca buffers reutilizáveis
        val numInputPixels = inputWidth * inputHeight
        inputByteBuffer = ByteBuffer.allocateDirect(1 * 3 * numInputPixels * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        inputPixels = IntArray(numInputPixels)
        val lbBmp = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
        letterboxBitmap = lbBmp
        letterboxCanvas = android.graphics.Canvas(lbBmp)

        val out0Count = numChannels0 * numAnchors0
        output0ByteBuffer = ByteBuffer.allocateDirect(out0Count * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        output0Floats = FloatArray(out0Count)

        val out1Count = protoChannels * protoHeight * protoWidth
        output1ByteBuffer = ByteBuffer.allocateDirect(out1Count * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        output1Floats = FloatArray(out1Count)
    }

    /**
     * Executa a inferência YOLO em um Bitmap de qualquer dimensão.
     */
    fun segment(
        bitmap: Bitmap,
        confThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f,
        maskOpacity: Int = 140
    ): DetectionResult = synchronized(lock) {
        val interp = interpreter
            ?: return DetectionResult(emptyList(), 0, 0, 0, bitmap.width, bitmap.height)

        val t0 = SystemClock.uptimeMillis()

        // 1. Pré-processamento: Redimensiona mantendo proporção (Letterbox)
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = min(inputWidth.toFloat() / originalWidth, inputHeight.toFloat() / originalHeight)
        val scaledW = (originalWidth * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (originalHeight * scale).roundToInt().coerceAtLeast(1)
        val padX = (inputWidth - scaledW) / 2f
        val padY = (inputHeight - scaledH) / 2f

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        val lbBitmap = letterboxBitmap ?: Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
        val lbCanvas = letterboxCanvas ?: android.graphics.Canvas(lbBitmap)
        lbCanvas.drawColor(Color.rgb(114, 114, 114)) // Padrão YOLO cinza 114
        lbCanvas.drawBitmap(resizedBitmap, padX, padY, null)

        val inBuffer = inputByteBuffer ?: ByteBuffer.allocateDirect(1 * 3 * inputWidth * inputHeight * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        inBuffer.rewind()

        val pixels = inputPixels ?: IntArray(inputWidth * inputHeight)
        lbBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        val inv255 = 1.0f / 255.0f
        if (isNCHW) {
            // Plano R, depois G, depois B
            for (p in pixels) {
                inBuffer.putFloat(((p shr 16) and 0xFF) * inv255)
            }
            for (p in pixels) {
                inBuffer.putFloat(((p shr 8) and 0xFF) * inv255)
            }
            for (p in pixels) {
                inBuffer.putFloat((p and 0xFF) * inv255)
            }
        } else {
            // Intercalado RGB
            for (p in pixels) {
                inBuffer.putFloat(((p shr 16) and 0xFF) * inv255)
                inBuffer.putFloat(((p shr 8) and 0xFF) * inv255)
                inBuffer.putFloat((p and 0xFF) * inv255)
            }
        }
        inBuffer.rewind()

        val t1 = SystemClock.uptimeMillis()

        // 2. Inferência TFLite com ByteBuffers diretos (sem criar milhares de FloatArrays no Heap)
        val out0Buf = output0ByteBuffer!!
        val out1Buf = output1ByteBuffer!!
        out0Buf.rewind()
        out1Buf.rewind()

        val outputs = HashMap<Int, Any>(2)
        outputs[0] = out0Buf
        if (interp.outputTensorCount > 1) {
            outputs[1] = out1Buf
        }

        interp.runForMultipleInputsOutputs(arrayOf(inBuffer), outputs)

        out0Buf.rewind()
        val out0 = output0Floats!!
        out0Buf.asFloatBuffer().get(out0)

        val out1 = output1Floats!!
        if (interp.outputTensorCount > 1) {
            out1Buf.rewind()
            out1Buf.asFloatBuffer().get(out1)
        }

        val t2 = SystemClock.uptimeMillis()

        // 3. Pós-processamento e NMS
        val numClasses = 23
        val rawDetections = mutableListOf<RawDetection>()

        for (a in 0 until numAnchors0) {
            var maxScore = 0f
            var bestClass = -1

            if (!isTransposed0) {
                // Layout [1, 59, 8400]: elemento (c, a) está em c * numAnchors0 + a
                for (c in 0 until numClasses) {
                    val score = out0[(4 + c) * numAnchors0 + a]
                    if (score > maxScore) {
                        maxScore = score
                        bestClass = c
                    }
                }
            } else {
                // Layout [1, 8400, 59]: elemento (a, c) está em a * numChannels0 + c
                val baseIdx = a * numChannels0
                for (c in 0 until numClasses) {
                    val score = out0[baseIdx + 4 + c]
                    if (score > maxScore) {
                        maxScore = score
                        bestClass = c
                    }
                }
            }

            if (maxScore >= confThreshold && bestClass >= 0) {
                var cx: Float
                var cy: Float
                var w: Float
                var h: Float

                if (!isTransposed0) {
                    cx = out0[0 * numAnchors0 + a]
                    cy = out0[1 * numAnchors0 + a]
                    w = out0[2 * numAnchors0 + a]
                    h = out0[3 * numAnchors0 + a]
                } else {
                    val baseIdx = a * numChannels0
                    cx = out0[baseIdx + 0]
                    cy = out0[baseIdx + 1]
                    w = out0[baseIdx + 2]
                    h = out0[baseIdx + 3]
                }

                // Normaliza caso venha em coordenadas de pixel 0..640
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

                if (origRight - origLeft <= 0.005f || origBottom - origTop <= 0.005f) continue

                // Coeficientes da máscara (32 floats a partir do canal 27)
                val maskCoeffs = FloatArray(protoChannels)
                if (!isTransposed0) {
                    for (k in 0 until protoChannels) {
                        maskCoeffs[k] = out0[(4 + numClasses + k) * numAnchors0 + a]
                    }
                } else {
                    val baseIdx = a * numChannels0 + 4 + numClasses
                    for (k in 0 until protoChannels) {
                        maskCoeffs[k] = out0[baseIdx + k]
                    }
                }

                // Coordenadas exatas da caixa visível mapeadas no grid de protótipos 160x160
                // (garante alinhamento 1:1 perfeito com origLeft..origBottom ao desenhar na tela)
                val protoBoxLeft = (((origLeft * scaledW + padX) / inputWidth) * protoWidth).coerceIn(0f, protoWidth - 1f)
                val protoBoxTop = (((origTop * scaledH + padY) / inputHeight) * protoHeight).coerceIn(0f, protoHeight - 1f)
                val protoBoxRight = (((origRight * scaledW + padX) / inputWidth) * protoWidth).coerceIn(0f, protoWidth - 1f)
                val protoBoxBottom = (((origBottom * scaledH + padY) / inputHeight) * protoHeight).coerceIn(0f, protoHeight - 1f)

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

        // 4. Reconstrói máscaras recortadas exatamente para a BoundingBox de cada peça
        val segmentedParts = mutableListOf<SegmentedPart>()
        for (det in nmsDetections) {
            val partClass = CarPartsCatalog.get(det.classId)
            val maskBitmap = generateCroppedMaskBitmap(
                protoFloats = out1,
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

    /**
     * Gera o Bitmap da máscara APENAS para a região recortada da Bounding Box (cropW x cropH),
     * para que o DetectionOverlay desenhe a máscara perfeitamente alinhada sobre a peça.
     */
    private fun generateCroppedMaskBitmap(
        protoFloats: FloatArray,
        coeffs: FloatArray,
        protoBox: BoundingBox,
        color: Int,
        alpha: Int
    ): Bitmap {
        val minX = protoBox.left.toInt().coerceIn(0, protoWidth - 1)
        val maxX = protoBox.right.roundToInt().coerceIn(minX, protoWidth - 1)
        val minY = protoBox.top.toInt().coerceIn(0, protoHeight - 1)
        val maxY = protoBox.bottom.roundToInt().coerceIn(minY, protoHeight - 1)

        val cropW = (maxX - minX + 1).coerceAtLeast(1)
        val cropH = (maxY - minY + 1).coerceAtLeast(1)

        val pixels = IntArray(cropW * cropH)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maskColor = Color.argb(alpha.coerceIn(0, 255), r, g, b)

        val spatialSize = protoHeight * protoWidth

        for (cy in 0 until cropH) {
            val py = minY + cy
            val rowOutOffset = cy * cropW
            for (cx in 0 until cropW) {
                val px = minX + cx
                var sum = 0f

                if (isProtoNCHW) {
                    // [1, 32, 160, 160]: índice = k * (160*160) + py * 160 + px
                    val pixelOffset = py * protoWidth + px
                    for (k in 0 until protoChannels) {
                        sum += coeffs[k] * protoFloats[k * spatialSize + pixelOffset]
                    }
                } else {
                    // [1, 160, 160, 32]: índice = (py * 160 + px) * 32 + k
                    val baseOffset = (py * protoWidth + px) * protoChannels
                    for (k in 0 until protoChannels) {
                        sum += coeffs[k] * protoFloats[baseOffset + k]
                    }
                }

                // Sigmoid > 0.5 equivale a logit (sum) > 0.0f!
                if (sum > 0f) {
                    // Suaviza levemente a borda próxima de 0 para visual mais limpo
                    val prob = 1.0f / (1.0f + exp(-sum))
                    if (prob > 0.5f) {
                        pixels[rowOutOffset + cx] = maskColor
                    }
                }
            }
        }

        val maskBmp = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
        maskBmp.setPixels(pixels, 0, cropW, 0, 0, cropW, cropH)
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

    private fun closeInternal() {
        try {
            interpreter?.close()
        } catch (_: Throwable) {
        }
        interpreter = null
        try {
            gpuDelegate?.close()
        } catch (_: Throwable) {
        }
        gpuDelegate = null
    }

    override fun close() {
        synchronized(lock) {
            closeInternal()
        }
    }

    private data class RawDetection(
        val classId: Int,
        val confidence: Float,
        val box: BoundingBox,
        val protoBox: BoundingBox,
        val maskCoeffs: FloatArray
    )
}
