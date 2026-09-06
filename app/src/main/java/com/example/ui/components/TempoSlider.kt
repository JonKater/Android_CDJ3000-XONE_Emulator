package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * Pioneer CDJ-3000 Precision 100mm Long-Throw Pitch Slider
 * With 0% Center LED, Master Tempo (MT), and Tempo Range selector.
 */
@Composable
fun TempoSlider(
    deckId: String,
    pitchPercent: Float, // e.g. -10.0f to +10.0f (0.0 is center)
    pitchRange: Float = 10.0f,
    tempoRangeLabel: String = "±10%",
    masterTempoActive: Boolean = false,
    onPitchChange: (Float) -> Unit,
    onReset: () -> Unit,
    onPitchBend: (Float) -> Unit,
    onToggleMasterTempo: () -> Unit = {},
    onCycleTempoRange: () -> Unit = {},
    modifier: Modifier = Modifier,
    height: Dp = 145.dp
) {
    Column(
        modifier = modifier.testTag("tempo_slider_$deckId"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CDJ-3000 Master Tempo (MT) & Tempo Range Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // MASTER TEMPO (Key Lock) Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (masterTempoActive) CdjKeyLockRed else Color(0xFF1E222A))
                    .border(0.7.dp, if (masterTempoActive) CdjKeyLockRed else Color(0xFF323846), RoundedCornerShape(2.dp))
                    .clickable { onToggleMasterTempo() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("master_tempo_$deckId"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MASTER\nTEMPO",
                    fontSize = 6.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = if (masterTempoActive) Color.White else Color(0xFF9AA0B0),
                    fontFamily = FontFamily.Monospace
                )
            }

            // TEMPO RANGE Cycle Button (±6%, ±10%, ±16%, WIDE)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E222A))
                    .border(0.7.dp, Color(0xFF323846), RoundedCornerShape(2.dp))
                    .clickable { onCycleTempoRange() }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .testTag("tempo_range_$deckId"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tempoRangeLabel,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjCueAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Readout & Reset Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF161920))
                .border(0.7.dp, Color(0xFF282D38), RoundedCornerShape(2.dp))
                .clickable { onReset() }
                .padding(horizontal = 4.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // 0% Green LED Indicator
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (abs(pitchPercent) < 0.1f) CdjPlayGreen else Color(0xFF2A342B))
            )
            Text(
                text = String.format(java.util.Locale.US, "%+4.2f%%", pitchPercent),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (abs(pitchPercent) < 0.1f) CdjPlayGreen else Color.White
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Fader Track and Pioneer CDJ Thumb
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(height)
                .pointerInput(deckId) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Moving down increases tempo (+)
                        val deltaPercent = (dragAmount.y / size.height) * (pitchRange * 2.0f)
                        var newP = (pitchPercent + deltaPercent).coerceIn(-pitchRange, pitchRange)
                        if (abs(newP) < 0.15f) newP = 0.0f // Center detent snap
                        onPitchChange(newP)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Track Slot (Deep groove with dust felt line)
                drawRoundRect(
                    color = Color(0xFF0C0D11),
                    topLeft = Offset((w - 4.dp.toPx()) / 2f, 4.dp.toPx()),
                    size = Size(4.dp.toPx(), h - 8.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // CDJ-3000 Calibration ticks (+, 0, -)
                val tickVals = listOf(-1.0f, -0.66f, -0.33f, 0.0f, 0.33f, 0.66f, 1.0f)
                for (tv in tickVals) {
                    val y = midY + (tv * (h / 2f - 12.dp.toPx()))
                    val isCenter = (tv == 0.0f)
                    drawLine(
                        color = if (isCenter) CdjPlayGreen else Color(0xFF383F4E),
                        start = Offset(2.dp.toPx(), y),
                        end = Offset(if (isCenter) 10.dp.toPx() else 6.dp.toPx(), y),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                    drawLine(
                        color = if (isCenter) CdjPlayGreen else Color(0xFF383F4E),
                        start = Offset(w - (if (isCenter) 10.dp.toPx() else 6.dp.toPx()), y),
                        end = Offset(w - 2.dp.toPx(), y),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Thumb Cap Position
                val normPos = (pitchPercent / pitchRange).coerceIn(-1.0f, 1.0f)
                val thumbY = midY + (normPos * (h / 2f - 12.dp.toPx()))
                val thumbW = 28.dp.toPx()
                val thumbH = 15.dp.toPx()

                // Draw CDJ Fader Cap (brushed aluminum bevel)
                drawRoundRect(
                    color = Color(0xFF2E333D),
                    topLeft = Offset((w - thumbW) / 2f, thumbY - thumbH / 2f),
                    size = Size(thumbW, thumbH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
                // Center white/green hairline
                drawLine(
                    color = if (pitchPercent == 0.0f) CdjPlayGreen else Color.White,
                    start = Offset((w - thumbW) / 2f + 2.dp.toPx(), thumbY),
                    end = Offset((w + thumbW) / 2f - 2.dp.toPx(), thumbY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Momentary Pitch Bend buttons (+ / -)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E222A))
                    .border(0.7.dp, Color(0xFF323846), RoundedCornerShape(3.dp))
                    .clickable { onPitchBend(-1.0f) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "−", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E222A))
                    .border(0.7.dp, Color(0xFF323846), RoundedCornerShape(3.dp))
                    .clickable { onPitchBend(1.0f) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
