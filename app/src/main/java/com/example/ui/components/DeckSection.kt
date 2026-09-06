package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.DeckUiState
import com.example.ui.FxUnitUiState
import com.example.ui.theme.*

/**
 * Pioneer CDJ-3000 Flagship Multi-Player Section
 * Layout:
 * 1. Top: CDJ-3000 9-Inch High-Resolution Touch Display (3-Band Color Waveform & Track Metadata)
 * 2. 8 Linear Backlit Hot Cue Buttons (A - H)
 * 3. 4-Beat Loop & Beat Jump (< 1, 1 >, < 4, 4 >)
 * 4. Mechanical Jog Wheel with On-Jog LCD Color Display + Precision 100mm Pitch Slider
 * 5. Large Circular Illuminated PLAY/PAUSE & CUE buttons + BEAT SYNC
 */
@Composable
fun DeckSection(
    deckState: DeckUiState,
    fxState: FxUnitUiState,
    accentColor: Color,
    jogSize: Dp = 150.dp,
    onPlayPause: () -> Unit,
    onCuePress: () -> Unit,
    onCueRelease: () -> Unit,
    onSync: () -> Unit,
    onHotCueTrigger: (Int) -> Unit,
    onHotCueClear: (Int) -> Unit,
    onJogTouch: (deltaAngle: Float, dtSec: Float) -> Unit,
    onJogRelease: () -> Unit,
    onJogNudge: (deltaAngle: Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchReset: () -> Unit,
    onPitchBend: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onSetLoopSize: (Double) -> Unit,
    onBeatJump: (Double) -> Unit = {},
    onToggleMasterTempo: () -> Unit = {},
    onCycleTempoRange: () -> Unit = {},
    onSeek: (Float) -> Unit,
    onFxDryWet: (Float) -> Unit = {},
    onFxParam1: (Float) -> Unit = {},
    onFxParam2: (Float) -> Unit = {},
    onFxParam3: (Float) -> Unit = {},
    onFxAssignA: () -> Unit = {},
    onFxAssignB: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CdjDeckChassis)
            .border(1.dp, CdjScreenBezel, RoundedCornerShape(6.dp))
            .padding(6.dp)
            .testTag("deck_section_${deckState.deckId}"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 1. CDJ-3000 9-Inch High-Resolution Touch Display with 3-Band Color Waveform
        WaveformView(
            deckId = deckState.deckId,
            trackTitle = deckState.trackTitle,
            artist = deckState.artist,
            bpm = deckState.currentBpm,
            musicalKey = deckState.musicalKey,
            positionMs = deckState.positionMs,
            durationMs = deckState.durationMs,
            progress = deckState.progress,
            waveformPoints = deckState.waveformPoints,
            hotCues = deckState.hotCues,
            isLooping = deckState.isLooping,
            loopBeats = deckState.loopBeats,
            accentColor = accentColor,
            onSeek = onSeek,
            height = 80.dp,
            pitchPercent = deckState.pitchPercent.toFloat(),
            quantizeActive = deckState.quantizeActive,
            masterTempoActive = deckState.isKeyLocked,
            slipActive = deckState.slipModeActive
        )

        // 2. Pioneer CDJ-3000 8 Backlit Hot Cues (A through H)
        HotCuePads(
            deckId = deckState.deckId,
            hotCues = deckState.hotCues,
            onCueTrigger = onHotCueTrigger,
            onCueClear = onHotCueClear
        )

        // 3. 4-Beat Loop & Beat Jump Section
        LoopSection(
            deckId = deckState.deckId,
            isLooping = deckState.isLooping,
            loopBeats = deckState.loopBeats,
            onToggleLoop = onToggleLoop,
            onSetLoopSize = onSetLoopSize,
            onBeatJump = onBeatJump
        )

        // 4. Central Performance Deck: CDJ-3000 Jog Wheel with On-Jog LCD & 100mm Pitch Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mechanical Jog Wheel with On-Jog Color LCD Display
            JogWheelView(
                deckId = deckState.deckId,
                rotationAngleDeg = deckState.jogAngleDeg,
                isPlaying = deckState.isPlaying,
                isScratching = deckState.isScratching,
                accentColor = accentColor,
                onJogTouch = onJogTouch,
                onJogRelease = onJogRelease,
                onJogNudge = onJogNudge,
                wheelSize = jogSize,
                trackBpm = deckState.currentBpm,
                isSync = deckState.isSyncActive,
                isMaster = false,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // CDJ-3000 Precision 100mm Pitch Slider with MT & Range switches
            TempoSlider(
                deckId = deckState.deckId,
                pitchPercent = deckState.pitchPercent.toFloat(),
                tempoRangeLabel = deckState.tempoRange,
                masterTempoActive = deckState.isKeyLocked,
                onPitchChange = onPitchChange,
                onReset = onPitchReset,
                onPitchBend = onPitchBend,
                onToggleMasterTempo = onToggleMasterTempo,
                onCycleTempoRange = onCycleTempoRange,
                height = 135.dp
            )
        }

        // 5. Pioneer Iconic Round PLAY/PAUSE and CUE Buttons + BEAT SYNC
        TransportBar(
            deckId = deckState.deckId,
            isPlaying = deckState.isPlaying,
            isCueActive = deckState.isCueActive,
            isSyncActive = deckState.isSyncActive,
            onPlayPause = onPlayPause,
            onCuePress = onCuePress,
            onCueRelease = onCueRelease,
            onSync = onSync
        )
    }
}
