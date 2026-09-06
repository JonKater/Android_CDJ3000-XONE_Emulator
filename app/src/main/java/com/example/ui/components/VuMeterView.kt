package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun VuMeterView(
    level: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    height: Dp = 80.dp,
    segmentCount: Int = 8
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val segmentGap = 2.dp.toPx()
        val totalGaps = (segmentCount - 1) * segmentGap
        val segmentHeight = (size.height - totalGaps) / segmentCount
        val w = size.width

        for (i in 0 until segmentCount) {
            // Segment 0 is at bottom, segmentCount - 1 is at top
            val segmentThreshold = (i + 1).toFloat() / segmentCount
            val isLit = level >= (i.toFloat() / segmentCount)

            // Segment color
            val activeColor = when {
                i >= segmentCount - 1 -> DjLedRed
                i >= segmentCount - 3 -> DjLedYellow
                else -> DjLedGreen
            }

            val inactiveColor = activeColor.copy(alpha = 0.12f)
            val yPos = size.height - (i + 1) * segmentHeight - (i * segmentGap)

            drawRoundRect(
                color = if (isLit) activeColor else inactiveColor,
                topLeft = Offset(0f, yPos),
                size = Size(w, segmentHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}
