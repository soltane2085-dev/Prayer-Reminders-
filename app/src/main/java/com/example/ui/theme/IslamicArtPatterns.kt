package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Islamic geometric tessellation background (8-fold rosette & Rub el Hizb pattern).
 * Highly optimized, drawn directly onto Canvas with configurable color and alpha.
 */
@Composable
fun IslamicGeometricBackground(
    modifier: Modifier = Modifier,
    patternColor: Color = Color(0xFFD4AF37),
    patternAlpha: Float = 0.08f,
    gridSizeDp: Dp = 56.dp
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSize = gridSizeDp.toPx()
        val cols = (size.width / gridSize).toInt() + 2
        val rows = (size.height / gridSize).toInt() + 2
        val effectiveColor = patternColor.copy(alpha = patternAlpha)
        val strokeWidth = 1.dp.toPx()

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val cx = i * gridSize
                val cy = j * gridSize

                // Draw 8-pointed Star (Rub el Hizb) at every grid intersection
                drawRubElHizbStar(
                    center = Offset(cx, cy),
                    radius = gridSize * 0.38f,
                    color = effectiveColor,
                    strokeWidth = strokeWidth
                )

                // Interlaced diagonal connecting lines forming the Girih lattice
                val nextCx = (i + 1) * gridSize
                val nextCy = (j + 1) * gridSize

                drawLine(
                    color = effectiveColor.copy(alpha = patternAlpha * 0.7f),
                    start = Offset(cx + gridSize * 0.38f, cy),
                    end = Offset(nextCx - gridSize * 0.38f, cy),
                    strokeWidth = strokeWidth
                )

                drawLine(
                    color = effectiveColor.copy(alpha = patternAlpha * 0.7f),
                    start = Offset(cx, cy + gridSize * 0.38f),
                    end = Offset(cx, nextCy - gridSize * 0.38f),
                    strokeWidth = strokeWidth
                )

                // Diagonal interlaced diamonds
                val midX = cx + gridSize / 2f
                val midY = cy + gridSize / 2f
                drawDiamond(
                    center = Offset(midX, midY),
                    radius = gridSize * 0.16f,
                    color = effectiveColor.copy(alpha = patternAlpha * 0.85f),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

/**
 * Draws a classic 8-pointed Islamic Star (two overlapping squares offset by 45°).
 */
private fun DrawScope.drawRubElHizbStar(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val squareSide = radius * 1.4142f / 2f

    // First square (0 deg)
    val path1 = Path().apply {
        moveTo(center.x - squareSide, center.y - squareSide)
        lineTo(center.x + squareSide, center.y - squareSide)
        lineTo(center.x + squareSide, center.y + squareSide)
        lineTo(center.x - squareSide, center.y + squareSide)
        close()
    }
    drawPath(path1, color = color, style = Stroke(width = strokeWidth))

    // Second square (45 deg)
    rotate(45f, center) {
        drawPath(path1, color = color, style = Stroke(width = strokeWidth))
    }

    // Central ornamental inner dot
    drawCircle(
        color = color,
        radius = radius * 0.15f,
        center = center,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Draws a decorative diamond shape.
 */
private fun DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth))
}

/**
 * Standalone Rub el Hizb Icon (8-pointed Islamic Star) with solid or outlined styling.
 */
@Composable
fun RubElHizbStarIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.85f
        val squareSide = radius * 1.4142f / 2f

        val path = Path().apply {
            moveTo(center.x - squareSide, center.y - squareSide)
            lineTo(center.x + squareSide, center.y - squareSide)
            lineTo(center.x + squareSide, center.y + squareSide)
            lineTo(center.x - squareSide, center.y + squareSide)
            close()
        }

        // Fill background first with subtle tone
        drawPath(path, color = color.copy(alpha = 0.25f), style = Fill)
        rotate(45f, center) {
            drawPath(path, color = color.copy(alpha = 0.25f), style = Fill)
        }

        // Outlines
        val strokeWidth = 2.dp.toPx()
        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
        rotate(45f, center) {
            drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
        }

        // Center jewel dot
        drawCircle(
            color = strokeColor,
            radius = radius * 0.22f,
            center = center,
            style = Fill
        )
    }
}

/**
 * An ornamental Islamic divider with gold geometric interlacing and center star rosette.
 */
@Composable
fun IslamicDecorativeDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val centerX = size.width / 2f
            val lineWidth = size.width * 0.38f

            // Left gradient line
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color),
                    startX = centerX - lineWidth,
                    endX = centerX - 18.dp.toPx()
                ),
                start = Offset(centerX - lineWidth, centerY),
                end = Offset(centerX - 18.dp.toPx(), centerY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Right gradient line
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(color, Color.Transparent),
                    startX = centerX + 18.dp.toPx(),
                    endX = centerX + lineWidth
                ),
                start = Offset(centerX + 18.dp.toPx(), centerY),
                end = Offset(centerX + lineWidth, centerY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center 8-pointed star
        RubElHizbStarIcon(
            modifier = Modifier.size(16.dp),
            color = color,
            strokeColor = color
        )
    }
}

/**
 * Islamic Arch (Mihrab) shape outline decoration for cards or banners.
 */
@Composable
fun IslamicArchFrame(
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFFD4AF37).copy(alpha = 0.4f),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val archHeight = 24.dp.toPx()

            val path = Path().apply {
                moveTo(0f, archHeight)
                // Pointed Islamic arch at the top
                cubicTo(
                    w * 0.25f, 0f,
                    w * 0.4f, 0f,
                    w * 0.5f, 4.dp.toPx()
                )
                cubicTo(
                    w * 0.6f, 0f,
                    w * 0.75f, 0f,
                    w, archHeight
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        content()
    }
}
