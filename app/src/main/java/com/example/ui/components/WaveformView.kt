package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.HotCueUiState
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Pioneer CDJ-3000 9-Inch High-Resolution Touch Display Simulation
 * Features 3-band color spectral waveform (Highs: Blue, Mids: Amber, Lows: White),
 * digital time readout, CDJ status indicators (Quantize, Master Tempo, Slip),
 * and mini full-track overview waveform with Hot Cue markers (A-H).
 */
@Composable
fun WaveformView(
    deckId: String,
    trackTitle: String,
    artist: String,
    bpm: Double,
    musicalKey: String,
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    waveformPoints: List<Float>,
    hotCues: List<HotCueUiState>,
    isLooping: Boolean,
    loopBeats: Double,
    accentColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 95.dp,
    pitchPercent: Float = 0.0f,
    quantizeActive: Boolean = true,
    masterTempoActive: Boolean = false,
    slipActive: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CdjScreenBg)
            .border(1.dp, CdjScreenBezel, RoundedCornerShape(6.dp))
            .padding(4.dp)
            .testTag("waveform_$deckId")
    ) {
        // 1. CDJ-3000 Top Display Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player number & Track Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // CDJ Player tag (e.g. PLAYER 1 / PLAYER 2)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (deckId == "A") Color(0xFF0091EA) else Color(0xFFFF6D00))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "PLAYER ${if (deckId == "A") "1" else "2"}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = trackTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = artist,
                    fontSize = 10.sp,
                    color = Color(0xFF8B92A2),
                    maxLines = 1
                )
            }

            // CDJ-3000 Badges: QUANTIZE, MASTER TEMPO, SLIP, KEY, BPM, REMAIN TIME
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (quantizeActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(CdjPlayGreen.copy(alpha = 0.2f))
                            .border(0.7.dp, CdjPlayGreen, RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "Q",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = CdjPlayGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (masterTempoActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(CdjKeyLockRed.copy(alpha = 0.2f))
                            .border(0.7.dp, CdjKeyLockRed, RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "MT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = CdjKeyLockRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Key readout
                Text(
                    text = musicalKey,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjHotCueA,
                    fontFamily = FontFamily.Monospace
                )

                // BPM
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", bpm),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                // Large CDJ Remaining Time
                val remMs = (durationMs - positionMs).coerceAtLeast(0L)
                val remSec = (remMs / 1000).toInt()
                val remFrac = ((remMs % 1000) / 10).toInt()
                Text(
                    text = String.format(java.util.Locale.US, "-%02d:%02d.%02d", remSec / 60, remSec % 60, remFrac),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CdjWaveformHighBlue
                )
            }
        }

        // 2. CDJ-3000 Main 3-Band Color Scrolling Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - 24.dp)
                .padding(horizontal = 2.dp)
                .pointerInput(deckId) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(ratio)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Screen Background
                drawRect(
                    color = CdjScreenBg,
                    size = size
                )

                // Subdued center baseline
                drawLine(
                    color = Color(0xFF1E222B),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Beat-Grid markers (every beat and bar)
                val beatIntervalMs = (60_000.0 / bpm).toFloat()
                if (durationMs > 0 && beatIntervalMs > 0) {
                    val totalBeats = (durationMs / beatIntervalMs).toInt().coerceAtMost(320)
                    for (b in 0 until totalBeats) {
                        val beatMs = b * beatIntervalMs
                        val beatX = (beatMs / durationMs) * w
                        val isBar = (b % 4 == 0)

                        drawLine(
                            color = if (isBar) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f),
                            start = Offset(beatX, if (isBar) 0f else midY - 6.dp.toPx()),
                            end = Offset(beatX, if (isBar) h else midY + 6.dp.toPx()),
                            strokeWidth = if (isBar) 1.5.dp.toPx() else 0.8.dp.toPx()
                        )
                    }
                }

                // 3-Band Waveform Bars (CDJ-3000 Color Style)
                // Highs: Blue | Mids: Amber | Lows: White
                val points = if (waveformPoints.isNotEmpty()) waveformPoints else generateFallbackPoints()
                val barCount = points.size
                val barWidth = (w / barCount).coerceAtLeast(1.5f)

                for (i in 0 until barCount) {
                    val x = i * barWidth
                    val amp = points[i].coerceIn(0.06f, 1.0f)
                    val barH = amp * (midY - 2.dp.toPx())

                    val isPast = (x / w) < progress
                    val alpha = if (isPast) 0.50f else 0.95f

                    // 1. High Frequency Blue Base
                    drawLine(
                        color = CdjWaveformHighBlue.copy(alpha = alpha),
                        start = Offset(x, midY - barH),
                        end = Offset(x, midY + barH),
                        strokeWidth = (barWidth * 0.8f).coerceAtLeast(1.2f)
                    )

                    // 2. Mid Frequency Amber Layer
                    val midH = barH * 0.65f
                    drawLine(
                        color = CdjWaveformMidAmber.copy(alpha = alpha),
                        start = Offset(x, midY - midH),
                        end = Offset(x, midY + midH),
                        strokeWidth = (barWidth * 0.8f).coerceAtLeast(1.2f)
                    )

                    // 3. Low Frequency / Kick Transient White Core
                    val kickH = barH * 0.32f
                    drawLine(
                        color = CdjWaveformLowWhite.copy(alpha = alpha),
                        start = Offset(x, midY - kickH),
                        end = Offset(x, midY + kickH),
                        strokeWidth = (barWidth * 0.8f).coerceAtLeast(1.2f)
                    )
                }

                // Hot Cue Markers (A through H) on Waveform
                for (cue in hotCues) {
                    if (cue.isSet && durationMs > 0) {
                        val cueX = (cue.positionMs.toFloat() / durationMs) * w
                        val cueColor = try {
                            Color(android.graphics.Color.parseColor(cue.colorHex))
                        } catch (e: Exception) {
                            accentColor
                        }

                        // Cue flag vertical line
                        drawLine(
                            color = cueColor,
                            start = Offset(cueX, 0f),
                            end = Offset(cueX, h),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Cue flag head with letter
                        drawRect(
                            color = cueColor,
                            topLeft = Offset(cueX, 0f),
                            size = Size(8.dp.toPx(), 7.dp.toPx())
                        )
                    }
                }

                // Active Loop Region
                if (isLooping && durationMs > 0) {
                    val loopDurationMs = (loopBeats * (60_000.0 / bpm)).toLong()
                    val loopStartX = progress * w
                    val loopWidth = (loopDurationMs.toFloat() / durationMs) * w

                    drawRect(
                        color = CdjCueAmber.copy(alpha = 0.28f),
                        topLeft = Offset(loopStartX, 0f),
                        size = Size(loopWidth.coerceAtLeast(4.dp.toPx()), h)
                    )
                    drawLine(
                        color = CdjCueAmber,
                        start = Offset(loopStartX, 0f),
                        end = Offset(loopStartX, h),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = CdjCueAmber,
                        start = Offset(loopStartX + loopWidth, 0f),
                        end = Offset(loopStartX + loopWidth, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // CDJ-3000 Red Center Playhead Needle
                val playheadX = progress * w
                drawLine(
                    color = CdjKeyLockRed,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, h),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(playheadX, midY)
                )
            }
        }

        // 3. Mini Track Overview Strip (at the bottom of CDJ-3000 display)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(horizontal = 2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Mini background
                drawRect(color = Color(0xFF101318), size = size)

                // Mini progress fill
                drawRect(
                    color = Color(0xFF283244),
                    topLeft = Offset(0f, 0f),
                    size = Size(progress * w, h)
                )

                // Mini Hot Cue pips
                for (cue in hotCues) {
                    if (cue.isSet && durationMs > 0) {
                        val cx = (cue.positionMs.toFloat() / durationMs) * w
                        val cColor = try {
                            Color(android.graphics.Color.parseColor(cue.colorHex))
                        } catch (e: Exception) {
                            accentColor
                        }
                        drawRect(
                            color = cColor,
                            topLeft = Offset(cx - 1.dp.toPx(), 0f),
                            size = Size(2.5.dp.toPx(), h)
                        )
                    }
                }

                // Mini Playhead pip
                drawRect(
                    color = CdjKeyLockRed,
                    topLeft = Offset(progress * w - 1.dp.toPx(), 0f),
                    size = Size(2.dp.toPx(), h)
                )
            }
        }
    }
}

private fun generateFallbackPoints(): List<Float> {
    return List(120) { i ->
        val x = i.toDouble() * 0.15
        val wave = 0.4 + 0.3 * sin(x) + 0.2 * sin(x * 2.3) + 0.1 * sin(x * 5.1)
        wave.toFloat().coerceIn(0.1f, 1.0f)
    }
}
