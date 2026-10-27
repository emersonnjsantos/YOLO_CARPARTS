package com.emerson.carpartsyolo.ui.overlay

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.emerson.carpartsyolo.ml.DetectionResult
import kotlin.math.roundToInt

@Composable
fun DetectionOverlay(
    detectionResult: DetectionResult?,
    modifier: Modifier = Modifier,
    showBoxes: Boolean = true,
    showLabels: Boolean = true,
    showMasks: Boolean = true,
    isMirrored: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val result = detectionResult ?: return@Canvas
        val viewW = size.width
        val viewH = size.height

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 34f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        for (part in result.parts) {
            val box = part.box
            val left = if (isMirrored) (1f - box.right) * viewW else box.left * viewW
            val right = if (isMirrored) (1f - box.left) * viewW else box.right * viewW
            val top = box.top * viewH
            val bottom = box.bottom * viewH
            val boxW = right - left
            val boxH = bottom - top

            val partColor = Color(part.partClass.color)

            // 1. Desenha a Máscara de Segmentação
            if (showMasks && part.maskBitmap != null) {
                val maskImg = part.maskBitmap.asImageBitmap()
                val dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt())
                val dstSize = androidx.compose.ui.unit.IntSize(boxW.roundToInt().coerceAtLeast(1), boxH.roundToInt().coerceAtLeast(1))

                // Desenha a máscara esticada para a região da caixa delimitadora
                drawImage(
                    image = maskImg,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    alpha = 0.85f
                )
            }

            // 2. Desenha a Bounding Box
            if (showBoxes) {
                drawRoundRect(
                    color = partColor,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 5f)
                )
            }

            // 3. Desenha o Selo com Nome e Confiança
            if (showLabels) {
                val labelText = "${part.partClass.labelPt} ${(part.confidence * 100).toInt()}%"
                val textBounds = Rect()
                paint.getTextBounds(labelText, 0, labelText.length, textBounds)

                val badgePaddingH = 14f
                val badgePaddingV = 8f
                val badgeW = textBounds.width() + badgePaddingH * 2
                val badgeH = textBounds.height() + badgePaddingV * 2
                val badgeTop = (top - badgeH).coerceAtLeast(4f)

                // Fundo do selo
                drawRoundRect(
                    color = partColor,
                    topLeft = Offset(left, badgeTop),
                    size = Size(badgeW, badgeH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Texto do selo
                drawContext.canvas.nativeCanvas.apply {
                    paint.color = android.graphics.Color.WHITE
                    drawText(
                        labelText,
                        left + badgePaddingH,
                        badgeTop + badgeH - badgePaddingV - 2f,
                        paint
                    )
                }
            }
        }
    }
}
