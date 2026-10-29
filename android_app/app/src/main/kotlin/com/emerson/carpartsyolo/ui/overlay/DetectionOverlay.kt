package com.emerson.carpartsyolo.ui.overlay

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.emerson.carpartsyolo.ml.DetectionResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DetectionOverlay(
    detectionResult: DetectionResult?,
    modifier: Modifier = Modifier,
    showBoxes: Boolean = true,
    showLabels: Boolean = true,
    showMasks: Boolean = true,
    isMirrored: Boolean = false,
    isFillCenter: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val result = detectionResult ?: return@Canvas
        val viewW = size.width
        val viewH = size.height
        if (viewW <= 0f || viewH <= 0f) return@Canvas

        // Calcula a transformação exata entre a imagem analisada e o Canvas na tela
        // Live Camera (PreviewView.ScaleType.FILL_CENTER) -> max(scaleX, scaleY)
        // Galeria (ContentScale.Fit) -> min(scaleX, scaleY)
        val imgW = result.imageWidth.coerceAtLeast(1).toFloat()
        val imgH = result.imageHeight.coerceAtLeast(1).toFloat()

        val scale = if (isFillCenter) {
            max(viewW / imgW, viewH / imgH)
        } else {
            min(viewW / imgW, viewH / imgH)
        }

        val renderedW = imgW * scale
        val renderedH = imgH * scale
        val offsetX = (viewW - renderedW) / 2f
        val offsetY = (viewH - renderedH) / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 34f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        for (part in result.parts) {
            val box = part.box
            val normLeft = if (isMirrored) (1f - box.right) else box.left
            val normRight = if (isMirrored) (1f - box.left) else box.right

            val left = offsetX + normLeft * renderedW
            val right = offsetX + normRight * renderedW
            val top = offsetY + box.top * renderedH
            val bottom = offsetY + box.bottom * renderedH
            val boxW = (right - left).coerceAtLeast(1f)
            val boxH = (bottom - top).coerceAtLeast(1f)

            val partColor = Color(part.partClass.color)

            // 1. Desenha a Máscara de Segmentação recortada exatamente na região da caixa
            if (showMasks && part.maskBitmap != null) {
                val maskImg = part.maskBitmap.asImageBitmap()
                val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
                val dstSize = IntSize(boxW.roundToInt().coerceAtLeast(1), boxH.roundToInt().coerceAtLeast(1))

                if (isMirrored) {
                    withTransform({
                        scale(
                            scaleX = -1f,
                            scaleY = 1f,
                            pivot = Offset(left + boxW / 2f, top + boxH / 2f)
                        )
                    }) {
                        drawImage(
                            image = maskImg,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                            filterQuality = FilterQuality.Medium
                        )
                    }
                } else {
                    drawImage(
                        image = maskImg,
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        filterQuality = FilterQuality.Medium
                    )
                }
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

            // 3. Desenha o Selo com Nome e Confiança (mantendo dentro dos limites visíveis da tela)
            if (showLabels) {
                val labelText = "${part.partClass.labelPt} ${(part.confidence * 100).roundToInt()}%"
                val textBounds = Rect()
                paint.getTextBounds(labelText, 0, labelText.length, textBounds)

                val badgePaddingH = 14f
                val badgePaddingV = 8f
                val badgeW = textBounds.width() + badgePaddingH * 2
                val badgeH = textBounds.height() + badgePaddingV * 2

                val badgeLeft = left.coerceIn(4f, max(4f, viewW - badgeW - 4f))
                val badgeTop = if (top - badgeH >= 4f) {
                    top - badgeH
                } else {
                    (top + 4f).coerceAtMost(max(4f, viewH - badgeH - 4f))
                }

                // Fundo do selo
                drawRoundRect(
                    color = partColor,
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeW, badgeH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Texto do selo
                drawContext.canvas.nativeCanvas.apply {
                    paint.color = android.graphics.Color.WHITE
                    drawText(
                        labelText,
                        badgeLeft + badgePaddingH,
                        badgeTop + badgeH - badgePaddingV - 2f,
                        paint
                    )
                }
            }
        }
    }
}
