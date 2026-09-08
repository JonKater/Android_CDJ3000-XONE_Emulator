package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.audio.XoneVcfFilter
import com.example.ui.DeckUiState
import com.example.ui.MixerUiState
import com.example.ui.theme.*

/**
 * Allen & Heath Xone:96 4-Band Analogue Club Mixer Section
 * Features:
 * - 4-Band British EQ (HI, HI-MID, LO-MID, LO) with Kill & Boost
 * - Dual Xone VCF Filter Bank (HPF, BPF, LPF, Res, Crunch harmonic overdrive)
 * - Channel Filter Assignment (FLT 1 / OFF / FLT 2)
 * - Xone Cue Monitoring & 60mm VCA Faders
 * - Mini-InnoFADER Crossfader with Fast Cut / Smooth curve switch
 */
@Composable
fun MixerSection(
    deckAState: DeckUiState,
    deckBState: DeckUiState,
    mixerState: MixerUiState,
    onGainA: (Float) -> Unit,
    onGainB: (Float) -> Unit,
    onEqHighA: (Float) -> Unit,
    onEqHighMidA: (Float) -> Unit = {},
    onEqLowMidA: (Float) -> Unit = {},
    onEqLowA: (Float) -> Unit,
    onEqMidA: (Float) -> Unit = {},
    onFilterA: (Float) -> Unit = {},
    onFilterAssignA: (Int) -> Unit = {},
    onCrossfaderAssignA: (String) -> Unit = {},
    onEqHighB: (Float) -> Unit,
    onEqHighMidB: (Float) -> Unit = {},
    onEqLowMidB: (Float) -> Unit = {},
    onEqLowB: (Float) -> Unit,
    onEqMidB: (Float) -> Unit = {},
    onFilterB: (Float) -> Unit = {},
    onFilterAssignB: (Int) -> Unit = {},
    onCrossfaderAssignB: (String) -> Unit = {},
    onFaderA: (Float) -> Unit,
    onFaderB: (Float) -> Unit,
    onCueListenA: () -> Unit,
    onCueListenB: () -> Unit,
    onCrossfader: (Float) -> Unit,
    onCrossfaderCurve: (AudioEngine.CrossfaderCurve) -> Unit,
    onMasterVolume: (Float) -> Unit,
    onBoothVolume: (Float) -> Unit = {},
    onVcf1Freq: (Float) -> Unit = {},
    onVcf1Res: (Float) -> Unit = {},
    onVcf1Crunch: (Float) -> Unit = {},
    onVcf1Type: (XoneVcfFilter.FilterType) -> Unit = {},
    onToggleVcf1: () -> Unit = {},
    onVcf2Freq: (Float) -> Unit = {},
    onVcf2Res: (Float) -> Unit = {},
    onVcf2Crunch: (Float) -> Unit = {},
    onVcf2Type: (XoneVcfFilter.FilterType) -> Unit = {},
    onToggleVcf2: () -> Unit = {},
    onOpenBrowser: () -> Unit,
    onRecordToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(XonePanelBg)
            .border(1.dp, XonePanelBorder, RoundedCornerShape(4.dp))
            .padding(6.dp)
            .testTag("mixer_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Section: Master, Booth, Record, Browse
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Master Level
            RotaryKnob(
                value = mixerState.masterVolume,
                onValueChange = onMasterVolume,
                label = "MASTER",
                size = 34.dp,
                accentColor = XoneLedGreen,
                centerDetent = false
            )

            // Booth Level
            RotaryKnob(
                value = mixerState.boothVolume,
                onValueChange = onBoothVolume,
                label = "BOOTH",
                size = 32.dp,
                accentColor = XoneLedGreen,
                centerDetent = false
            )

            // Central BROWSE Button (styled like Xone brushed metal button)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF222630))
                    .border(1.dp, CdjWaveformHighBlue, RoundedCornerShape(3.dp))
                    .clickable { onOpenBrowser() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("browse_library_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LIBRARY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = CdjWaveformHighBlue,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // REC Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (mixerState.isRecording) CdjKeyLockRed else Color(0xFF222630))
                    .border(1.dp, if (mixerState.isRecording) CdjKeyLockRed else Color(0xFF384050), RoundedCornerShape(3.dp))
                    .clickable { onRecordToggle() }
                    .padding(horizontal = 7.dp, vertical = 5.dp)
                    .testTag("record_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        tint = if (mixerState.isRecording) Color.White else CdjKeyLockRed,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (mixerState.isRecording) {
                            val sec = mixerState.recordingDurationSec
                            String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60)
                        } else "REC",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (mixerState.isRecording) Color.White else Color(0xFFE0E0E0)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Allen & Heath 4-Band Channel Strips (CH 1/A & CH 2/B)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Channel A (Left Strip)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CH 1",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = CdjWaveformHighBlue,
                    fontFamily = FontFamily.Monospace
                )

                // GAIN
                RotaryKnob(value = deckAState.gainNorm, onValueChange = onGainA, label = "GAIN", size = 30.dp, accentColor = Color.White)

                // 4-BAND BRITISH EQ (XONE:96)
                // HI (+6 / -∞)
                RotaryKnob(value = deckAState.eqHighNorm, onValueChange = onEqHighA, label = "HI", size = 31.dp, accentColor = XoneEqHiBlue)
                // HI-MID (+10 / -30)
                RotaryKnob(value = deckAState.eqHighMidNorm, onValueChange = onEqHighMidA, label = "MID 1", size = 31.dp, accentColor = XoneEqMidWhite)
                // LO-MID (+10 / -30)
                RotaryKnob(value = deckAState.eqLowMidNorm, onValueChange = onEqLowMidA, label = "MID 2", size = 31.dp, accentColor = XoneEqMidWhite)
                // LO (+6 / -∞)
                RotaryKnob(value = deckAState.eqLowNorm, onValueChange = onEqLowA, label = "LO", size = 31.dp, accentColor = XoneEqLoRed)

                // Filter Assignment (1 / OFF / 2)
                XoneFilterAssignSwitch(
                    currentAssign = deckAState.filterAssign,
                    onAssignChange = onFilterAssignA,
                    label = "FILTER"
                )
            }

            // Center: 9-Segment Dual Peak VU Meters
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VuMeterView(level = deckAState.peakMeter, height = 155.dp, width = 5.dp)
                    VuMeterView(level = deckBState.peakMeter, height = 155.dp, width = 5.dp)
                }
            }

            // Channel B (Right Strip)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CH 2",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = CdjCueAmber,
                    fontFamily = FontFamily.Monospace
                )

                // GAIN
                RotaryKnob(value = deckBState.gainNorm, onValueChange = onGainB, label = "GAIN", size = 30.dp, accentColor = Color.White)

                // 4-BAND BRITISH EQ (XONE:96)
                // HI
                RotaryKnob(value = deckBState.eqHighNorm, onValueChange = onEqHighB, label = "HI", size = 31.dp, accentColor = XoneEqHiBlue)
                // HI-MID
                RotaryKnob(value = deckBState.eqHighMidNorm, onValueChange = onEqHighMidB, label = "MID 1", size = 31.dp, accentColor = XoneEqMidWhite)
                // LO-MID
                RotaryKnob(value = deckBState.eqLowMidNorm, onValueChange = onEqLowMidB, label = "MID 2", size = 31.dp, accentColor = XoneEqMidWhite)
                // LO
                RotaryKnob(value = deckBState.eqLowNorm, onValueChange = onEqLowB, label = "LO", size = 31.dp, accentColor = XoneEqLoRed)

                // Filter Assignment (1 / OFF / 2)
                XoneFilterAssignSwitch(
                    currentAssign = deckBState.filterAssign,
                    onAssignChange = onFilterAssignB,
                    label = "FILTER"
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Allen & Heath Dual VCF Filters Module (Filter 1 & Filter 2)
        XoneDualVcfFilterBank(
            mixerState = mixerState,
            onVcf1Freq = onVcf1Freq,
            onVcf1Res = onVcf1Res,
            onVcf1Crunch = onVcf1Crunch,
            onVcf1Type = onVcf1Type,
            onToggleVcf1 = onToggleVcf1,
            onVcf2Freq = onVcf2Freq,
            onVcf2Res = onVcf2Res,
            onVcf2Crunch = onVcf2Crunch,
            onVcf2Type = onVcf2Type,
            onToggleVcf2 = onToggleVcf2
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Channel Cue Listen Buttons (CH 1 / CH 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (deckAState.cueListen) XoneCueBlue else Color(0xFF1E222A))
                    .border(1.dp, if (deckAState.cueListen) XoneCueBlue else Color(0xFF384050), RoundedCornerShape(3.dp))
                    .clickable { onCueListenA() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Cue A",
                        tint = if (deckAState.cueListen) Color.White else Color(0xFF9AA0B0),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "CUE 1",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (deckAState.cueListen) Color.White else Color(0xFF9AA0B0)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (deckBState.cueListen) XoneCueBlue else Color(0xFF1E222A))
                    .border(1.dp, if (deckBState.cueListen) XoneCueBlue else Color(0xFF384050), RoundedCornerShape(3.dp))
                    .clickable { onCueListenB() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Cue B",
                        tint = if (deckBState.cueListen) Color.White else Color(0xFF9AA0B0),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "CUE 2",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (deckBState.cueListen) Color.White else Color(0xFF9AA0B0)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 5. 60mm VCA Channel Faders (CH 1 & CH 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalChannelFader(
                value = deckAState.faderVolume,
                onValueChange = onFaderA,
                label = "CH 1",
                accentColor = CdjWaveformHighBlue,
                height = 75.dp
            )

            VerticalChannelFader(
                value = deckBState.faderVolume,
                onValueChange = onFaderB,
                label = "CH 2",
                accentColor = CdjCueAmber,
                height = 75.dp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Mini-InnoFADER Crossfader Section with Curve switch
        HorizontalCrossfader(
            value = mixerState.crossfader,
            curve = mixerState.crossfaderCurve,
            onValueChange = onCrossfader,
            onCurveToggle = {
                val next = if (mixerState.crossfaderCurve == AudioEngine.CrossfaderCurve.SMOOTH)
                    AudioEngine.CrossfaderCurve.SCRATCH
                else
                    AudioEngine.CrossfaderCurve.SMOOTH
                onCrossfaderCurve(next)
            }
        )
    }
}

/**
 * Xone:96 3-way Filter Assignment Switch (FLT 1 / OFF / FLT 2)
 */
@Composable
fun XoneFilterAssignSwitch(
    currentAssign: Int, // 0 = OFF, 1 = FILTER 1, 2 = FILTER 2
    onAssignChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF888F9E),
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF14171E))
                .border(0.7.dp, Color(0xFF2C3240), RoundedCornerShape(2.dp))
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            listOf(1 to "1", 0 to "OFF", 2 to "2").forEach { (id, txt) ->
                val isSelected = (currentAssign == id)
                val selBg = if (isSelected) {
                    if (id == 1) XoneVcfBlue else if (id == 2) XoneVcfRed else Color(0xFF404856)
                } else Color.Transparent

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(selBg)
                        .clickable { onAssignChange(id) }
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = txt,
                        fontSize = 7.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF7A8394),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Allen & Heath Xone Dual VCF Filter Bank:
 * Filter 1 & Filter 2 with HPF, BPF, LPF, Frequency, Resonance, Crunch saturation, and ON button.
 */
@Composable
fun XoneDualVcfFilterBank(
    mixerState: MixerUiState,
    onVcf1Freq: (Float) -> Unit,
    onVcf1Res: (Float) -> Unit,
    onVcf1Crunch: (Float) -> Unit,
    onVcf1Type: (XoneVcfFilter.FilterType) -> Unit,
    onToggleVcf1: () -> Unit,
    onVcf2Freq: (Float) -> Unit,
    onVcf2Res: (Float) -> Unit,
    onVcf2Crunch: (Float) -> Unit,
    onVcf2Type: (XoneVcfFilter.FilterType) -> Unit,
    onToggleVcf2: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF181B22))
            .border(1.dp, Color(0xFF2C3342), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "XONE:VCF 1",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = if (mixerState.vcf1Active) XoneVcfBlue else Color(0xFF8890A0),
                fontFamily = FontFamily.Monospace
            )

            // VCF 1 Filter Type Selectors (HPF, BPF, LPF)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    XoneVcfFilter.FilterType.HPF to "HPF",
                    XoneVcfFilter.FilterType.BPF to "BPF",
                    XoneVcfFilter.FilterType.LPF to "LPF"
                ).forEach { (fType, label) ->
                    val isSel = (mixerState.vcf1Type == fType)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSel) XoneVcfBlue else Color(0xFF222630))
                            .border(0.6.dp, if (isSel) XoneVcfBlue else Color(0xFF384050), RoundedCornerShape(2.dp))
                            .clickable { onVcf1Type(fType) }
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else Color(0xFF8B92A2),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // VCF 1 ON/OFF Illuminated Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (mixerState.vcf1Active) XoneVcfBlue else Color(0xFF222630))
                    .border(0.8.dp, if (mixerState.vcf1Active) Color.White else Color(0xFF384050), RoundedCornerShape(2.dp))
                    .clickable { onToggleVcf1() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (mixerState.vcf1Active) "VCF 1 ON" else "VCF 1",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = if (mixerState.vcf1Active) Color.White else Color(0xFF8B92A2),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // VCF 1 Knobs: FREQ, RES, CRUNCH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RotaryKnob(
                value = mixerState.vcf1Freq,
                onValueChange = onVcf1Freq,
                label = "FREQ",
                size = 28.dp,
                accentColor = XoneVcfBlue,
                centerDetent = false
            )
            RotaryKnob(
                value = mixerState.vcf1Res,
                onValueChange = onVcf1Res,
                label = "RES",
                size = 28.dp,
                accentColor = XoneCueAmber,
                centerDetent = false
            )
            RotaryKnob(
                value = mixerState.vcf1Crunch,
                onValueChange = onVcf1Crunch,
                label = "CRUNCH",
                size = 28.dp,
                accentColor = XoneVcfRed,
                centerDetent = false
            )
        }
    }
}

@Composable
fun VerticalChannelFader(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 75.dp
) {
    val currentValue by rememberUpdatedState(value)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(height)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = currentValue }
                    ) { change, dragAmount ->
                        change.consume()
                        // Moving up increases volume (0 at bottom, 1 at top)
                        val delta = -dragAmount.y / size.height
                        dragAccumulator += delta
                        val newVal = dragAccumulator.coerceIn(0f, 1f)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Slot with dB markers
                drawRoundRect(
                    color = Color(0xFF0C0D11),
                    topLeft = Offset((w - 4.dp.toPx()) / 2f, 2.dp.toPx()),
                    size = Size(4.dp.toPx(), h - 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Calibration ticks on both sides of fader
                val ticks = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
                for (t in ticks) {
                    val y = 4.dp.toPx() + (1.0f - t) * (h - 8.dp.toPx())
                    drawLine(
                        color = Color(0xFF384050),
                        start = Offset(4.dp.toPx(), y),
                        end = Offset(8.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF384050),
                        start = Offset(w - 8.dp.toPx(), y),
                        end = Offset(w - 4.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Fader Cap (0 is bottom, 1 is top)
                val thumbY = (1.0f - value) * (h - 14.dp.toPx()) + 7.dp.toPx()
                val capW = 24.dp.toPx()
                val capH = 13.dp.toPx()

                // Xone styled fader cap
                drawRoundRect(
                    color = Color(0xFF282D36),
                    topLeft = Offset((w - capW) / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawLine(
                    color = Color.White,
                    start = Offset((w - capW) / 2f + 2.dp.toPx(), thumbY),
                    end = Offset((w + capW) / 2f - 2.dp.toPx(), thumbY),
                    strokeWidth = 1.8.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun HorizontalCrossfader(
    value: Float, // 0.0f to 1.0f (0.5 is center)
    curve: AudioEngine.CrossfaderCurve,
    onValueChange: (Float) -> Unit,
    onCurveToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("crossfader_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "X",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = CdjWaveformHighBlue
            )

            // Curve switch badge (Fast Cut / Smooth)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E222A))
                    .border(0.7.dp, Color(0xFF384050), RoundedCornerShape(2.dp))
                    .clickable { onCurveToggle() }
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (curve == AudioEngine.CrossfaderCurve.SMOOTH) "SMOOTH (HOUSE)" else "FAST (CUT)",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjCueAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Y",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = CdjCueAmber
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("crossfader_slider")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = currentValue }
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / size.width
                        dragAccumulator += delta
                        val newVal = dragAccumulator.coerceIn(0f, 1f)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Track Slot
                drawRoundRect(
                    color = Color(0xFF0C0D11),
                    topLeft = Offset(8.dp.toPx(), midY - 2.dp.toPx()),
                    size = Size(w - 16.dp.toPx(), 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Center Detent tick
                drawLine(
                    color = Color(0xFFFFD600),
                    start = Offset(w / 2f, midY - 5.dp.toPx()),
                    end = Offset(w / 2f, midY + 5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Crossfader Cap
                val thumbX = 16.dp.toPx() + value * (w - 32.dp.toPx())
                val capW = 16.dp.toPx()
                val capH = 20.dp.toPx()

                drawRoundRect(
                    color = Color(0xFF282D36),
                    topLeft = Offset(thumbX - capW / 2f, midY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawLine(
                    color = if (value in 0.48f..0.52f) Color(0xFFFFD600) else Color.White,
                    start = Offset(thumbX, midY - capH / 2f + 2.dp.toPx()),
                    end = Offset(thumbX, midY + capH / 2f - 2.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )
            }
        }
    }
}
