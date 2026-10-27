package com.emerson.carpartsyolo.ml

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Coordenadas normalizadas do retângulo [0..1] relativas à imagem.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

/**
 * Informações e rótulos das 23 classes do dataset CarParts.
 */
data class CarPartClass(
    val id: Int,
    val key: String,
    val labelPt: String,
    val color: Int
)

object CarPartsCatalog {
    val CLASSES = listOf(
        CarPartClass(0, "back_bumper", "Para-choque Traseiro", Color.rgb(255, 99, 71)),       // Tomato
        CarPartClass(1, "back_door", "Porta Traseira", Color.rgb(54, 162, 235)),              // Blue
        CarPartClass(2, "back_glass", "Vidro Traseiro", Color.rgb(75, 192, 192)),             // Teal
        CarPartClass(3, "back_left_door", "Porta Traseira Esquerda", Color.rgb(153, 102, 255)),// Purple
        CarPartClass(4, "back_left_light", "Lanterna Traseira Esq.", Color.rgb(255, 159, 64)),// Orange
        CarPartClass(5, "back_light", "Lanterna Traseira", Color.rgb(255, 205, 86)),          // Yellow
        CarPartClass(6, "back_right_door", "Porta Traseira Direita", Color.rgb(201, 203, 207)),// Grey
        CarPartClass(7, "back_right_light", "Lanterna Traseira Dir.", Color.rgb(255, 99, 132)),// Pink
        CarPartClass(8, "front_bumper", "Para-choque Dianteiro", Color.rgb(0, 204, 150)),     // Emerald
        CarPartClass(9, "front_door", "Porta Dianteira", Color.rgb(171, 99, 250)),            // Violet
        CarPartClass(10, "front_glass", "Para-brisa Dianteiro", Color.rgb(25, 211, 242)),     // Sky Blue
        CarPartClass(11, "front_left_door", "Porta Dianteira Esq.", Color.rgb(254, 203, 82)), // Amber
        CarPartClass(12, "front_left_light", "Farol Dianteiro Esq.", Color.rgb(231, 99, 250)),// Magenta
        CarPartClass(13, "front_light", "Farol Dianteiro", Color.rgb(19, 181, 234)),          // Cyan
        CarPartClass(14, "front_right_door", "Porta Dianteira Dir.", Color.rgb(131, 90, 241)),// Indigo
        CarPartClass(15, "front_right_light", "Farol Dianteiro Dir.", Color.rgb(246, 126, 0)),// Deep Orange
        CarPartClass(16, "hood", "Capô", Color.rgb(44, 160, 44)),                             // Green
        CarPartClass(17, "left_mirror", "Retrovisor Esquerdo", Color.rgb(214, 39, 40)),        // Crimson
        CarPartClass(18, "object", "Objeto Genérico", Color.rgb(148, 103, 189)),               // Lilac
        CarPartClass(19, "right_mirror", "Retrovisor Direito", Color.rgb(140, 86, 75)),        // Brown
        CarPartClass(20, "tailgate", "Tampa Traseira (Hatch)", Color.rgb(227, 119, 194)),     // Rose
        CarPartClass(21, "trunk", "Porta-Malas (Sedan)", Color.rgb(127, 127, 127)),          // Gray
        CarPartClass(22, "wheel", "Roda / Pneu", Color.rgb(188, 189, 34))                     // Olive
    )

    fun get(classId: Int): CarPartClass {
        return CLASSES.getOrNull(classId) ?: CarPartClass(
            id = classId,
            key = "part_$classId",
            labelPt = "Peça $classId",
            color = Color.rgb(100, 200, 255)
        )
    }
}

/**
 * Representa uma peça detectada e segmentada pelo YOLO.
 */
data class SegmentedPart(
    val partClass: CarPartClass,
    val confidence: Float,
    val box: BoundingBox,
    val maskBitmap: Bitmap? = null
)

/**
 * Resultado geral da inferência de um quadro ou foto.
 */
data class DetectionResult(
    val parts: List<SegmentedPart>,
    val inferenceTimeMs: Long,
    val preprocessTimeMs: Long,
    val postprocessTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)
