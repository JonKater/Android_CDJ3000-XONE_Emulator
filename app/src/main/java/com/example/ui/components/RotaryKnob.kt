package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun RotaryKnob(
    value: Float, // 0.0f to 1.0f (0.5f is center)
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accentColor: Color = DjDeckACyan,
    centerDetent: Boolean = true,
    displayValue: String? = null
) {
    var dragAccumulator by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        dragAccumulator = value
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = DjTextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(size)
                .testTag("knob_${label.lowercase()}")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Vertical drag: dragging up increases value, dragging down decreases
                        val delta = -dragAmount.y * 0.006f
                        var newVal = (dragAccumulator + delta).coerceIn(0.0f, 1.0f)
                        if (centerDetent && abs(newVal - 0.5f) < 0.035f) {
                            newVal = 0.5f
                        }
                        dragAccumulator = newVal
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                // Background track arc (from 135 deg to 405 deg = 270 deg span)
                val startAngle = 135f
                val sweepTotal = 270f
                val trackRadius = radius - 4.dp.toPx()

                drawArc(
                    color = DjPanelElevated,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                    size = androidx.compose.ui.geometry.Size(trackRadius * 2, trackRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active value arc
                val currentSweep = sweepTotal * value
                if (centerDetent) {
                    // Bi-directional from center (0.5)
                    val centerAngle = startAngle + sweepTotal * 0.5f
                    val sweepFromCenter = sweepTotal * (value - 0.5f)
                    drawArc(
                        color = accentColor,
                        startAngle = centerAngle,
                        sweepAngle = sweepFromCenter,
                        useCenter = false,
                        topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                        size = androidx.compose.ui.geometry.Size(trackRadius * 2, trackRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    drawArc(
                        color = accentColor,
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                        size = androidx.compose.ui.geometry.Size(trackRadius * 2, trackRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Knob Body (metallic dark brushed gradient)
                val bodyRadius = trackRadius - 4.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DjKnobCap, DjPanelDark),
                        center = center,
                        radius = bodyRadius
                    ),
                    radius = bodyRadius,
                    center = center
                )

                // Outer bezel stroke
                drawCircle(
                    color = DjPanelBorder,
                    radius = bodyRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Indicator line
                val indicatorAngleDeg = startAngle + currentSweep
                val indicatorAngleRad = Math.toRadians(indicatorAngleDeg.toDouble())
                val innerR = bodyRadius * 0.35f
                val outerR = bodyRadius * 0.9f

                val startP = Offset(
                    center.x + (innerR * cos(indicatorAngleRad)).toFloat(),
                    center.y + (innerR * sin(indicatorAngleRad)).toFloat()
                )
                val endP = Offset(
                    center.x + (outerR * cos(indicatorAngleRad)).toFloat(),
                    center.y + (outerR * sin(indicatorAngleRad)).toFloat()
                )

                drawLine(
                    color = if (value == 0.5f && centerDetent) DjLedYellow else DjKnobLine,
                    start = startP,
                    end = endP,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (displayValue != null) {
            Text(
                text = displayValue,
                fontSize = 8.sp,
                color = DjTextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
