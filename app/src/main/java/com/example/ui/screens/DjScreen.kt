package com.example.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TrackEntity
import com.example.ui.DjViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

enum class PortraitTab {
    DUAL_DECK,
    DECK_A,
    MIXER,
    DECK_B,
    SAMPLER
}

@Composable
fun DjScreen(
    viewModel: DjViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp >= 720

    val deckA by viewModel.deckAState.collectAsStateWithLifecycle()
    val deckB by viewModel.deckBState.collectAsStateWithLifecycle()
    val mixer by viewModel.mixerState.collectAsStateWithLifecycle()
    val fx1 by viewModel.fx1State.collectAsStateWithLifecycle()
    val fx2 by viewModel.fx2State.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()

    var showBrowserDialog by remember { mutableStateOf(false) }
    var selectedPortraitTab by remember { mutableStateOf(PortraitTab.DUAL_DECK) }

    // SAF File Picker for local user audio files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "Imported Track"
            viewModel.loadTrackToDeck(
                TrackEntity(
                    title = fileName.substringAfterLast("/").substringBeforeLast("."),
                    artist = "Local Library",
                    bpm = 126.0,
                    initialKey = "8A",
                    durationMs = 210_000L,
                    filePath = it.toString(),
                    genre = "Local Audio"
                ),
                deckId = "A"
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0C10)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0A0C10))
        ) {
            // Top App Bar: CDJ-3000 & Xone:96 Identity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171E))
                    .border(0.5.dp, Color(0xFF242A36))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(CdjWaveformHighBlue)
                    )
                    Text(
                        text = "CDJ-3000",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "×",
                        fontSize = 10.sp,
                        color = Color(0xFF8890A0)
                    )
                    Text(
                        text = "XONE:96",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = CdjCueAmber,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Import Local Audio File
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E222C))
                            .border(1.dp, Color(0xFF343C4E), RoundedCornerShape(3.dp))
                            .clickable { filePickerLauncher.launch("audio/*") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("import_audio_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = CdjWaveformHighBlue, modifier = Modifier.size(12.dp))
                            Text("IMPORT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Open Browser Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CdjWaveformHighBlue)
                            .clickable { showBrowserDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("top_browse_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Text("BROWSE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (isLandscape) {
                // ==================== LANDSCAPE / TABLET LAYOUT ====================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Left: Pioneer CDJ-3000 Multi-Player (Deck A)
                    DeckSection(
                        deckState = deckA,
                        fxState = fx1,
                        accentColor = CdjWaveformHighBlue,
                        jogSize = 135.dp,
                        onPlayPause = { viewModel.playPause("A") },
                        onCuePress = { viewModel.cuePress("A") },
                        onCueRelease = { viewModel.cueRelease("A") },
                        onSync = { viewModel.sync("A") },
                                    onPhraseSyncToggle = { viewModel.togglePhraseSync("A") },
                                    onQuantizeToggle = { viewModel.toggleQuantize("A") },
                                    onSnapToggle = { viewModel.toggleSnap("A") },
                        onHotCueTrigger = { viewModel.onHotCueTrigger("A", it) },
                        onHotCueClear = { viewModel.clearHotCue("A", it) },
                        onJogTouch = { delta, dt -> viewModel.onJogTouch("A", delta, dt) },
                        onJogRelease = { viewModel.onJogRelease("A") },
                        onJogNudge = { viewModel.onJogNudge("A", it) },
                        onPitchChange = { viewModel.setPitchPercent("A", it) },
                        onPitchReset = { viewModel.resetPitch("A") },
                        onPitchBend = { viewModel.pitchBend("A", it) },
                        onToggleLoop = { viewModel.toggleLoop("A") },
                        onSetLoopSize = { viewModel.setLoopSize("A", it) },
                        onBeatJump = { viewModel.beatJump("A", it) },
                        onToggleMasterTempo = { viewModel.toggleMasterTempo("A") },
                        onCycleTempoRange = { viewModel.cycleTempoRange("A") },
                        onSeek = { viewModel.seekDeckTo("A", it) },
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )

                    // Center: Allen & Heath Xone:96 4-Band Club Mixer
                    MixerSection(
                        deckAState = deckA,
                        deckBState = deckB,
                        mixerState = mixer,
                        onGainA = { viewModel.setGain("A", it) },
                        onGainB = { viewModel.setGain("B", it) },
                        onEqHighA = { viewModel.setEqHigh("A", it) },
                        onEqHighMidA = { viewModel.setEqHighMid("A", it) },
                        onEqLowMidA = { viewModel.setEqLowMid("A", it) },
                        onEqLowA = { viewModel.setEqLow("A", it) },
                        onFilterAssignA = { viewModel.setFilterAssign("A", it) },
                        onCrossfaderAssignA = { viewModel.setCrossfaderAssign("A", it) },
                        onEqHighB = { viewModel.setEqHigh("B", it) },
                        onEqHighMidB = { viewModel.setEqHighMid("B", it) },
                        onEqLowMidB = { viewModel.setEqLowMid("B", it) },
                        onEqLowB = { viewModel.setEqLow("B", it) },
                        onFilterAssignB = { viewModel.setFilterAssign("B", it) },
                        onCrossfaderAssignB = { viewModel.setCrossfaderAssign("B", it) },
                        onFaderA = { viewModel.setChannelFader("A", it) },
                        onFaderB = { viewModel.setChannelFader("B", it) },
                        onCueListenA = { viewModel.toggleCueListen("A") },
                        onCueListenB = { viewModel.toggleCueListen("B") },
                        onCrossfader = { viewModel.setCrossfader(it) },
                        onCrossfaderCurve = { viewModel.setCrossfaderCurve(it) },
                        onMasterVolume = { viewModel.setMasterVolume(it) },
                        onBoothVolume = { viewModel.setBoothVolume(it) },
                        onVcf1Freq = { viewModel.setVcf1Freq(it) },
                        onVcf1Res = { viewModel.setVcf1Res(it) },
                        onVcf1Crunch = { viewModel.setVcf1Crunch(it) },
                        onVcf1Type = { viewModel.setVcf1Type(it) },
                        onToggleVcf1 = { viewModel.toggleVcf1() },
                        onVcf2Freq = { viewModel.setVcf2Freq(it) },
                        onVcf2Res = { viewModel.setVcf2Res(it) },
                        onVcf2Crunch = { viewModel.setVcf2Crunch(it) },
                        onVcf2Type = { viewModel.setVcf2Type(it) },
                        onToggleVcf2 = { viewModel.toggleVcf2() },
                        onOpenBrowser = { showBrowserDialog = true },
                        onRecordToggle = { viewModel.toggleRecording() },
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )

                    // Right: Pioneer CDJ-3000 Multi-Player (Deck B)
                    DeckSection(
                        deckState = deckB,
                        fxState = fx2,
                        accentColor = CdjCueAmber,
                        jogSize = 135.dp,
                        onPlayPause = { viewModel.playPause("B") },
                        onCuePress = { viewModel.cuePress("B") },
                        onCueRelease = { viewModel.cueRelease("B") },
                        onSync = { viewModel.sync("B") },
                                    onPhraseSyncToggle = { viewModel.togglePhraseSync("B") },
                                    onQuantizeToggle = { viewModel.toggleQuantize("B") },
                                    onSnapToggle = { viewModel.toggleSnap("B") },
                        onHotCueTrigger = { viewModel.onHotCueTrigger("B", it) },
                        onHotCueClear = { viewModel.clearHotCue("B", it) },
                        onJogTouch = { delta, dt -> viewModel.onJogTouch("B", delta, dt) },
                        onJogRelease = { viewModel.onJogRelease("B") },
                        onJogNudge = { viewModel.onJogNudge("B", it) },
                        onPitchChange = { viewModel.setPitchPercent("B", it) },
                        onPitchReset = { viewModel.resetPitch("B") },
                        onPitchBend = { viewModel.pitchBend("B", it) },
                        onToggleLoop = { viewModel.toggleLoop("B") },
                        onSetLoopSize = { viewModel.setLoopSize("B", it) },
                        onBeatJump = { viewModel.beatJump("B", it) },
                        onToggleMasterTempo = { viewModel.toggleMasterTempo("B") },
                        onCycleTempoRange = { viewModel.cycleTempoRange("B") },
                        onSeek = { viewModel.seekDeckTo("B", it) },
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                // ==================== PORTRAIT MOBILE LAYOUT ====================
                Column(modifier = Modifier.fillMaxSize()) {
                    // Navigation Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14171E))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton("CDJ 1", selectedPortraitTab == PortraitTab.DECK_A, CdjWaveformHighBlue, Modifier.weight(1f)) {
                            selectedPortraitTab = PortraitTab.DECK_A
                        }
                        TabButton("XONE MIX", selectedPortraitTab == PortraitTab.MIXER, CdjCueAmber, Modifier.weight(1f)) {
                            selectedPortraitTab = PortraitTab.MIXER
                        }
                        TabButton("CDJ 2", selectedPortraitTab == PortraitTab.DECK_B, CdjCueAmber, Modifier.weight(1f)) {
                            selectedPortraitTab = PortraitTab.DECK_B
                        }
                        TabButton("FX / SAMPLER", selectedPortraitTab == PortraitTab.SAMPLER, CdjPlayGreen, Modifier.weight(1f)) {
                            selectedPortraitTab = PortraitTab.SAMPLER
                        }
                    }

                    // Dual Mini CDJ-3000 3-Band Waveforms Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1115))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        WaveformView(
                            deckId = "A",
                            trackTitle = deckA.trackTitle,
                            artist = deckA.artist,
                            bpm = deckA.currentBpm,
                            musicalKey = deckA.musicalKey,
                            positionMs = deckA.positionMs,
                            durationMs = deckA.durationMs,
                            progress = deckA.progress,
                            waveformPoints = deckA.waveformPoints,
                            hotCues = deckA.hotCues,
                            isLooping = deckA.isLooping,
                            loopBeats = deckA.loopBeats,
                            accentColor = CdjWaveformHighBlue,
                            onSeek = { viewModel.seekDeckTo("A", it) },
                            height = 42.dp,
                            pitchPercent = deckA.pitchPercent.toFloat(),
                            quantizeActive = deckA.quantizeActive,
                            snapActive = deckA.snapActive,
                            phraseSyncActive = deckA.phraseSyncActive,
                            phraseBeat = deckA.phraseBeat,
                            phraseBar = deckA.phraseBar,
                            masterTempoActive = deckA.isKeyLocked,
                            slipActive = deckA.slipModeActive
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        WaveformView(
                            deckId = "B",
                            trackTitle = deckB.trackTitle,
                            artist = deckB.artist,
                            bpm = deckB.currentBpm,
                            musicalKey = deckB.musicalKey,
                            positionMs = deckB.positionMs,
                            durationMs = deckB.durationMs,
                            progress = deckB.progress,
                            waveformPoints = deckB.waveformPoints,
                            hotCues = deckB.hotCues,
                            isLooping = deckB.isLooping,
                            loopBeats = deckB.loopBeats,
                            accentColor = CdjCueAmber,
                            onSeek = { viewModel.seekDeckTo("B", it) },
                            height = 42.dp,
                            pitchPercent = deckB.pitchPercent.toFloat(),
                            quantizeActive = deckB.quantizeActive,
                            snapActive = deckB.snapActive,
                            phraseSyncActive = deckB.phraseSyncActive,
                            phraseBeat = deckB.phraseBeat,
                            phraseBar = deckB.phraseBar,
                            masterTempoActive = deckB.isKeyLocked,
                            slipActive = deckB.slipModeActive
                        )
                    }

                    // Selected Tab Content Body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        when (selectedPortraitTab) {
                            PortraitTab.DUAL_DECK, PortraitTab.DECK_A -> {
                                DeckSection(
                                    deckState = deckA,
                                    fxState = fx1,
                                    accentColor = CdjWaveformHighBlue,
                                    jogSize = 145.dp,
                                    onPlayPause = { viewModel.playPause("A") },
                                    onCuePress = { viewModel.cuePress("A") },
                                    onCueRelease = { viewModel.cueRelease("A") },
                                    onSync = { viewModel.sync("A") },
                                    onPhraseSyncToggle = { viewModel.togglePhraseSync("A") },
                                    onQuantizeToggle = { viewModel.toggleQuantize("A") },
                                    onSnapToggle = { viewModel.toggleSnap("A") },
                                    onHotCueTrigger = { viewModel.onHotCueTrigger("A", it) },
                                    onHotCueClear = { viewModel.clearHotCue("A", it) },
                                    onJogTouch = { delta, dt -> viewModel.onJogTouch("A", delta, dt) },
                                    onJogRelease = { viewModel.onJogRelease("A") },
                                    onJogNudge = { viewModel.onJogNudge("A", it) },
                                    onPitchChange = { viewModel.setPitchPercent("A", it) },
                                    onPitchReset = { viewModel.resetPitch("A") },
                                    onPitchBend = { viewModel.pitchBend("A", it) },
                                    onToggleLoop = { viewModel.toggleLoop("A") },
                                    onSetLoopSize = { viewModel.setLoopSize("A", it) },
                                    onBeatJump = { viewModel.beatJump("A", it) },
                                    onToggleMasterTempo = { viewModel.toggleMasterTempo("A") },
                                    onCycleTempoRange = { viewModel.cycleTempoRange("A") },
                                    onSeek = { viewModel.seekDeckTo("A", it) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                            PortraitTab.DECK_B -> {
                                DeckSection(
                                    deckState = deckB,
                                    fxState = fx2,
                                    accentColor = CdjCueAmber,
                                    jogSize = 145.dp,
                                    onPlayPause = { viewModel.playPause("B") },
                                    onCuePress = { viewModel.cuePress("B") },
                                    onCueRelease = { viewModel.cueRelease("B") },
                                    onSync = { viewModel.sync("B") },
                                    onPhraseSyncToggle = { viewModel.togglePhraseSync("B") },
                                    onQuantizeToggle = { viewModel.toggleQuantize("B") },
                                    onSnapToggle = { viewModel.toggleSnap("B") },
                                    onHotCueTrigger = { viewModel.onHotCueTrigger("B", it) },
                                    onHotCueClear = { viewModel.clearHotCue("B", it) },
                                    onJogTouch = { delta, dt -> viewModel.onJogTouch("B", delta, dt) },
                                    onJogRelease = { viewModel.onJogRelease("B") },
                                    onJogNudge = { viewModel.onJogNudge("B", it) },
                                    onPitchChange = { viewModel.setPitchPercent("B", it) },
                                    onPitchReset = { viewModel.resetPitch("B") },
                                    onPitchBend = { viewModel.pitchBend("B", it) },
                                    onToggleLoop = { viewModel.toggleLoop("B") },
                                    onSetLoopSize = { viewModel.setLoopSize("B", it) },
                                    onBeatJump = { viewModel.beatJump("B", it) },
                                    onToggleMasterTempo = { viewModel.toggleMasterTempo("B") },
                                    onCycleTempoRange = { viewModel.cycleTempoRange("B") },
                                    onSeek = { viewModel.seekDeckTo("B", it) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                            PortraitTab.MIXER -> {
                                MixerSection(
                                    deckAState = deckA,
                                    deckBState = deckB,
                                    mixerState = mixer,
                                    onGainA = { viewModel.setGain("A", it) },
                                    onGainB = { viewModel.setGain("B", it) },
                                    onEqHighA = { viewModel.setEqHigh("A", it) },
                                    onEqHighMidA = { viewModel.setEqHighMid("A", it) },
                                    onEqLowMidA = { viewModel.setEqLowMid("A", it) },
                                    onEqLowA = { viewModel.setEqLow("A", it) },
                                    onFilterAssignA = { viewModel.setFilterAssign("A", it) },
                                    onCrossfaderAssignA = { viewModel.setCrossfaderAssign("A", it) },
                                    onEqHighB = { viewModel.setEqHigh("B", it) },
                                    onEqHighMidB = { viewModel.setEqHighMid("B", it) },
                                    onEqLowMidB = { viewModel.setEqLowMid("B", it) },
                                    onEqLowB = { viewModel.setEqLow("B", it) },
                                    onFilterAssignB = { viewModel.setFilterAssign("B", it) },
                                    onCrossfaderAssignB = { viewModel.setCrossfaderAssign("B", it) },
                                    onFaderA = { viewModel.setChannelFader("A", it) },
                                    onFaderB = { viewModel.setChannelFader("B", it) },
                                    onCueListenA = { viewModel.toggleCueListen("A") },
                                    onCueListenB = { viewModel.toggleCueListen("B") },
                                    onCrossfader = { viewModel.setCrossfader(it) },
                                    onCrossfaderCurve = { viewModel.setCrossfaderCurve(it) },
                                    onMasterVolume = { viewModel.setMasterVolume(it) },
                                    onBoothVolume = { viewModel.setBoothVolume(it) },
                                    onVcf1Freq = { viewModel.setVcf1Freq(it) },
                                    onVcf1Res = { viewModel.setVcf1Res(it) },
                                    onVcf1Crunch = { viewModel.setVcf1Crunch(it) },
                                    onVcf1Type = { viewModel.setVcf1Type(it) },
                                    onToggleVcf1 = { viewModel.toggleVcf1() },
                                    onVcf2Freq = { viewModel.setVcf2Freq(it) },
                                    onVcf2Res = { viewModel.setVcf2Res(it) },
                                    onVcf2Crunch = { viewModel.setVcf2Crunch(it) },
                                    onVcf2Type = { viewModel.setVcf2Type(it) },
                                    onToggleVcf2 = { viewModel.toggleVcf2() },
                                    onOpenBrowser = { showBrowserDialog = true },
                                    onRecordToggle = { viewModel.toggleRecording() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                            PortraitTab.SAMPLER -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SamplerPanel(
                                        sampler = viewModel.audioEngine.sampler,
                                        onTriggerSample = { viewModel.triggerSample(it) }
                                    )

                                    FxPanel(
                                        fxState = fx1,
                                        onDryWet = { viewModel.setFxDryWet(1, it) },
                                        onParam1 = { viewModel.setFxParam1(1, it) },
                                        onParam2 = { viewModel.setFxParam2(1, it) },
                                        onParam3 = { viewModel.setFxParam3(1, it) },
                                        onToggleAssignA = { viewModel.toggleFxAssign(1, "A") },
                                        onToggleAssignB = { viewModel.toggleFxAssign(1, "B") }
                                    )

                                    FxPanel(
                                        fxState = fx2,
                                        onDryWet = { viewModel.setFxDryWet(2, it) },
                                        onParam1 = { viewModel.setFxParam1(2, it) },
                                        onParam2 = { viewModel.setFxParam2(2, it) },
                                        onParam3 = { viewModel.setFxParam3(2, it) },
                                        onToggleAssignA = { viewModel.toggleFxAssign(2, "A") },
                                        onToggleAssignB = { viewModel.toggleFxAssign(2, "B") }
                                    )
                                }
                            }
                        }
                    }

                    // Persistent Bottom Quick-Crossfader & Mini-Transport bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14171E))
                            .border(1.dp, Color(0xFF282F3E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        HorizontalCrossfader(
                            value = mixer.crossfader,
                            curve = mixer.crossfaderCurve,
                            onValueChange = { viewModel.setCrossfader(it) },
                            onCurveToggle = {
                                val next = if (mixer.crossfaderCurve == com.example.audio.AudioEngine.CrossfaderCurve.SMOOTH)
                                    com.example.audio.AudioEngine.CrossfaderCurve.SCRATCH
                                else
                                    com.example.audio.AudioEngine.CrossfaderCurve.SMOOTH
                                viewModel.setCrossfaderCurve(next)
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Quick Dual Play / Cue transport buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Deck A Quick Play
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TransportButton(
                                    title = if (deckA.isPlaying) "PAUSE" else "PLAY 1",
                                    isActive = deckA.isPlaying,
                                    activeColor = CdjPlayGreen,
                                    modifier = Modifier.width(90.dp),
                                    onClick = { viewModel.playPause("A") }
                                )
                                TransportButton(
                                    title = "SYNC",
                                    isActive = deckA.isSyncActive,
                                    activeColor = CdjCueAmber,
                                    modifier = Modifier.width(60.dp),
                                    onClick = { viewModel.sync("A") }
                                )
                            }

                            // Deck B Quick Play
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TransportButton(
                                    title = "SYNC",
                                    isActive = deckB.isSyncActive,
                                    activeColor = CdjCueAmber,
                                    modifier = Modifier.width(60.dp),
                                    onClick = { viewModel.sync("B") }
                                )
                                TransportButton(
                                    title = if (deckB.isPlaying) "PAUSE" else "PLAY 2",
                                    isActive = deckB.isPlaying,
                                    activeColor = CdjPlayGreen,
                                    modifier = Modifier.width(90.dp),
                                    onClick = { viewModel.playPause("B") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Library Browser Dialog
        if (showBrowserDialog) {
            LibraryBrowserDialog(
                tracks = tracks,
                onLoadToDeckA = { viewModel.loadTrackToDeck(it, "A") },
                onLoadToDeckB = { viewModel.loadTrackToDeck(it, "B") },
                onAddTrack = {
                    viewModel.loadTrackToDeck(it, "A")
                },
                onDismiss = { showBrowserDialog = false }
            )
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) accentColor else Color(0xFF1B1F2A))
            .border(1.dp, if (isSelected) accentColor else Color(0xFF2C3344), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) Color.Black else Color.White,
            letterSpacing = 0.5.sp
        )
    }
}
