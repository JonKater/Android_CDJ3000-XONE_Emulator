package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.AudioTrackDeck
import com.example.audio.XoneVcfFilter
import com.example.data.CuePointEntity
import com.example.data.DjDatabase
import com.example.data.DjRepository
import com.example.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.math.max

private val cdjHotCueColors = listOf(
    "#00E5FF", // A - Cyan
    "#76FF03", // B - Lime Green
    "#FF9100", // C - Orange
    "#E040FB", // D - Magenta
    "#FFD600", // E - Yellow
    "#2979FF", // F - Electric Blue
    "#FF1744", // G - Bright Red
    "#AA00FF"  // H - Purple
)

private val cdjHotCueLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H")

fun defaultCdjHotCues(): List<HotCueUiState> {
    return List(8) { idx ->
        HotCueUiState(
            index = idx + 1,
            isSet = false,
            positionMs = 0L,
            colorHex = cdjHotCueColors[idx],
            label = cdjHotCueLabels[idx]
        )
    }
}

data class DeckUiState(
    val deckId: String,
    val trackTitle: String = "NO TRACK LOADED",
    val artist: String = "Select track from browser",
    val baseBpm: Double = 126.0,
    val currentBpm: Double = 126.0,
    val musicalKey: String = "8A",
    val isPlaying: Boolean = false,
    val isCueActive: Boolean = false,
    val isSyncActive: Boolean = false,
    val isMasterActive: Boolean = false,
    val isKeyLocked: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 1L,
    val progress: Float = 0.0f,
    val jogAngleDeg: Float = 0.0f,
    val isScratching: Boolean = false,
    val pitchPercent: Float = 0.0f,
    val tempoRange: String = "±10%",
    val jogFeel: Float = 0.5f,
    val vinylSpeed: Float = 0.5f,
    val quantizeActive: Boolean = true,
    val slipModeActive: Boolean = false,
    val snapActive: Boolean = true,
    val phraseSyncActive: Boolean = false,
    val phraseBar: Int = 1,
    val phraseBeat: Int = 1,
    val gainNorm: Float = 0.5f,
    // Allen & Heath Xone:96 4-Band EQ
    val eqHighNorm: Float = 0.5f,
    val eqHighMidNorm: Float = 0.5f,
    val eqLowMidNorm: Float = 0.5f,
    val eqLowNorm: Float = 0.5f,
    val filterNorm: Float = 0.5f,
    // Xone Filter routing: 0 = OFF, 1 = FILTER 1, 2 = FILTER 2
    val filterAssign: Int = 0,
    val crossfaderAssign: String = "X", // "X", "OFF", "Y"
    val killHigh: Boolean = false,
    val killMid: Boolean = false,
    val killLow: Boolean = false,
    val faderVolume: Float = 0.85f,
    val cueListen: Boolean = false,
    val isLooping: Boolean = false,
    val loopBeats: Double = 4.0,
    val peakMeter: Float = 0.0f,
    val waveformPoints: List<Float> = emptyList(),
    val hotCues: List<HotCueUiState> = defaultCdjHotCues()
)

data class HotCueUiState(
    val index: Int,
    val isSet: Boolean = false,
    val positionMs: Long = 0L,
    val colorHex: String = "#00E5FF",
    val label: String = "A"
)

data class MixerUiState(
    val crossfader: Float = 0.5f,
    val crossfaderCurve: AudioEngine.CrossfaderCurve = AudioEngine.CrossfaderCurve.SMOOTH,
    val masterVolume: Float = 0.9f,
    val boothVolume: Float = 0.8f,
    val masterMeterL: Float = 0.0f,
    val masterMeterR: Float = 0.0f,
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    // Xone:96 VCF Filter 1
    val vcf1Active: Boolean = true,
    val vcf1Type: XoneVcfFilter.FilterType = XoneVcfFilter.FilterType.HPF,
    val vcf1Freq: Float = 0.1f,
    val vcf1Res: Float = 0.35f,
    val vcf1Crunch: Float = 0.0f,
    // Xone:96 VCF Filter 2
    val vcf2Active: Boolean = true,
    val vcf2Type: XoneVcfFilter.FilterType = XoneVcfFilter.FilterType.LPF,
    val vcf2Freq: Float = 0.9f,
    val vcf2Res: Float = 0.35f,
    val vcf2Crunch: Float = 0.0f
)

data class FxUnitUiState(
    val unitNumber: Int,
    val modeName: String = "DELAY",
    val dryWet: Float = 0.0f,
    val param1: Float = 0.5f,
    val param2: Float = 0.5f,
    val param3: Float = 0.5f,
    val assignDeckA: Boolean = false,
    val assignDeckB: Boolean = false
)

class DjViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine(44100)
    private val repository: DjRepository

    val tracks: StateFlow<List<TrackEntity>>

    private val _deckAState = MutableStateFlow(DeckUiState(deckId = "A"))
    val deckAState: StateFlow<DeckUiState> = _deckAState.asStateFlow()

    private val _deckBState = MutableStateFlow(DeckUiState(deckId = "B"))
    val deckBState: StateFlow<DeckUiState> = _deckBState.asStateFlow()

    private val _mixerState = MutableStateFlow(MixerUiState())
    val mixerState: StateFlow<MixerUiState> = _mixerState.asStateFlow()

    private val _fx1State = MutableStateFlow(FxUnitUiState(1, assignDeckA = true))
    val fx1State: StateFlow<FxUnitUiState> = _fx1State.asStateFlow()

    private val _fx2State = MutableStateFlow(FxUnitUiState(2, assignDeckB = true))
    val fx2State: StateFlow<FxUnitUiState> = _fx2State.asStateFlow()

    // Loaded tracks tracking
    private var loadedTrackA: TrackEntity? = null
    private var loadedTrackB: TrackEntity? = null
    private var cueCollectJobA: Job? = null
    private var cueCollectJobB: Job? = null

    init {
        val db = DjDatabase.getDatabase(application)
        repository = DjRepository(db.trackDao(), db.cuePointDao())
        tracks = repository.allTracks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Start Audio Engine
        audioEngine.start()

        viewModelScope.launch(Dispatchers.IO) {
            repository.initDefaultTracksIfEmpty()
        }

        // Auto-load first 2 tracks when tracks become available
        viewModelScope.launch {
            tracks.filter { it.isNotEmpty() }.first().let { trackList ->
                if (loadedTrackA == null && trackList.isNotEmpty()) {
                    loadTrackToDeck(trackList[0], "A")
                }
                if (loadedTrackB == null && trackList.size > 1) {
                    loadTrackToDeck(trackList[1], "B")
                }
            }
        }

        // Periodic high-precision UI telemetry loop (30 FPS)
        viewModelScope.launch(Dispatchers.Default) {
            var recTicks = 0
            while (isActive) {
                updateDecksTelemetry()
                delay(33) // ~30 fps update
                if (_mixerState.value.isRecording) {
                    recTicks++
                    if (recTicks % 30 == 0) {
                        _mixerState.update { it.copy(recordingDurationSec = it.recordingDurationSec + 1) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }

    private fun updateDecksTelemetry() {
        val dA = audioEngine.deckA
        val dB = audioEngine.deckB

        // Maintain BPM Lock if Sync is active
        if (_deckAState.value.isSyncActive && !dA.isScratching) {
            val targetBpm = dB.currentEffectiveBpm
            if (dA.bpm > 0) {
                dA.pitchPercent = ((targetBpm / dA.bpm) - 1.0) * 100.0
            }
        } else if (_deckBState.value.isSyncActive && !dB.isScratching) {
            val targetBpm = dA.currentEffectiveBpm
            if (dB.bpm > 0) {
                dB.pitchPercent = ((targetBpm / dB.bpm) - 1.0) * 100.0
            }
        }

        val durA = max(1L, dA.durationMs)
        val posA = dA.positionMs
        val progA = (posA.toFloat() / durA).coerceIn(0f, 1f)
        
        val beatsA = if (dA.bpm > 0) posA / (60000.0 / dA.bpm) else 0.0
        val beatInBarA = (beatsA.toLong() % 4).toInt() + 1
        val barInPhraseA = ((beatsA.toLong() / 4) % 8).toInt() + 1

        _deckAState.update {
            it.copy(
                isPlaying = dA.isPlaying,
                isCueActive = dA.isCueHeld,
                positionMs = posA,
                durationMs = durA,
                progress = progA,
                currentBpm = dA.currentEffectiveBpm,
                jogAngleDeg = dA.jogWheelAngleDeg,
                isScratching = dA.isScratching,
                pitchPercent = dA.pitchPercent.toFloat(),
                peakMeter = audioEngine.meterDeckA,
                isLooping = dA.isLooping,
                loopBeats = dA.loopBeatLength,
                phraseBeat = beatInBarA,
                phraseBar = barInPhraseA,
                snapActive = dA.snapActive,
                quantizeActive = dA.quantizeActive
            )
        }

        val durB = max(1L, dB.durationMs)
        val posB = dB.positionMs
        val progB = (posB.toFloat() / durB).coerceIn(0f, 1f)

        val beatsB = if (dB.bpm > 0) posB / (60000.0 / dB.bpm) else 0.0
        val beatInBarB = (beatsB.toLong() % 4).toInt() + 1
        val barInPhraseB = ((beatsB.toLong() / 4) % 8).toInt() + 1

        _deckBState.update {
            it.copy(
                isPlaying = dB.isPlaying,
                isCueActive = dB.isCueHeld,
                positionMs = posB,
                durationMs = durB,
                progress = progB,
                currentBpm = dB.currentEffectiveBpm,
                jogAngleDeg = dB.jogWheelAngleDeg,
                isScratching = dB.isScratching,
                pitchPercent = dB.pitchPercent.toFloat(),
                peakMeter = audioEngine.meterDeckB,
                isLooping = dB.isLooping,
                loopBeats = dB.loopBeatLength,
                phraseBeat = beatInBarB,
                phraseBar = barInPhraseB,
                snapActive = dB.snapActive,
                quantizeActive = dB.quantizeActive
            )
        }

        _mixerState.update {
            it.copy(
                masterMeterL = audioEngine.meterMasterL,
                masterMeterR = audioEngine.meterMasterR
            )
        }
    }

    fun loadTrackToDeck(track: TrackEntity, deckId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
            deck.loadProceduralTrack(track.bpm, track.genre, track.initialKey)

            val points = track.waveformData.split(",").mapNotNull { it.trim().toFloatOrNull() }

            if (deckId == "A") {
                loadedTrackA = track
                _deckAState.update {
                    it.copy(
                        trackTitle = track.title,
                        artist = track.artist,
                        baseBpm = track.bpm,
                        currentBpm = track.bpm,
                        musicalKey = track.initialKey,
                        durationMs = track.durationMs,
                        waveformPoints = points
                    )
                }
                cueCollectJobA?.cancel()
                cueCollectJobA = launch {
                    repository.getCuePointsForTrack(track.id).collect { cues ->
                        updateDeckHotCues(deckId, cues)
                    }
                }
            } else {
                loadedTrackB = track
                _deckBState.update {
                    it.copy(
                        trackTitle = track.title,
                        artist = track.artist,
                        baseBpm = track.bpm,
                        currentBpm = track.bpm,
                        musicalKey = track.initialKey,
                        durationMs = track.durationMs,
                        waveformPoints = points
                    )
                }
                cueCollectJobB?.cancel()
                cueCollectJobB = launch {
                    repository.getCuePointsForTrack(track.id).collect { cues ->
                        updateDeckHotCues(deckId, cues)
                    }
                }
            }
        }
    }

    private fun updateDeckHotCues(deckId: String, cues: List<CuePointEntity>) {
        val cueMap = cues.associateBy { it.cueIndex }
        val updated = (1..8).map { idx ->
            val ent = cueMap[idx]
            val defaultColor = cdjHotCueColors.getOrElse(idx - 1) { "#00E5FF" }
            val defaultLabel = cdjHotCueLabels.getOrElse(idx - 1) { ('A' + idx - 1).toString() }
            if (ent != null) {
                HotCueUiState(
                    index = idx,
                    isSet = true,
                    positionMs = ent.positionMs,
                    colorHex = if (ent.colorHex.isNotEmpty()) ent.colorHex else defaultColor,
                    label = defaultLabel
                )
            } else {
                HotCueUiState(
                    index = idx,
                    isSet = false,
                    colorHex = defaultColor,
                    label = defaultLabel
                )
            }
        }

        if (deckId == "A") {
            _deckAState.update { it.copy(hotCues = updated) }
        } else {
            _deckBState.update { it.copy(hotCues = updated) }
        }
    }

    // Transport Controls
    fun playPause(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.playPause()
    }

    fun cuePress(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.cuePress()
    }

    fun cueRelease(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.cueRelease()
    }

    fun sync(deckId: String) {
        val usePhraseSync = if (deckId == "A") _deckAState.value.phraseSyncActive else _deckBState.value.phraseSyncActive
        if (deckId == "A") {
            val isActive = !_deckAState.value.isSyncActive
            if (isActive) audioEngine.syncDecks(masterDeck = audioEngine.deckB, slaveDeck = audioEngine.deckA, usePhraseSync)
            _deckAState.update { it.copy(isSyncActive = isActive) }
        } else {
            val isActive = !_deckBState.value.isSyncActive
            if (isActive) audioEngine.syncDecks(masterDeck = audioEngine.deckA, slaveDeck = audioEngine.deckB, usePhraseSync)
            _deckBState.update { it.copy(isSyncActive = isActive) }
        }
    }

    // Hot Cue Actions (8 Pioneer CDJ-3000 Hot Cues: A-H)
    fun onHotCueTrigger(deckId: String, cueIndex: Int) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        val state = if (deckId == "A") _deckAState.value else _deckBState.value
        val cue = state.hotCues.find { it.index == cueIndex }
        val loadedTrack = if (deckId == "A") loadedTrackA else loadedTrackB

        if (cue != null && cue.isSet) {
            // Jump to cue position
            if (deck.quantizeActive && deck.isPlaying) {
                deck.seekToMsQuantized(cue.positionMs)
            } else {
                deck.seekToMs(cue.positionMs)
            }
        } else if (loadedTrack != null) {
            // Set current position as new Hot Cue (with Snap if active)
            var targetSample = deck.currentSamplePos
            if (deck.snapActive) {
                targetSample = deck.getNearestBeatSample(deck.currentSamplePos).toDouble()
            }
            val posMs = ((targetSample / deck.sampleRate) * 1000).toLong()

            val color = cdjHotCueColors.getOrElse(cueIndex - 1) { "#00E5FF" }
            val label = cdjHotCueLabels.getOrElse(cueIndex - 1) { ('A' + cueIndex - 1).toString() }
            viewModelScope.launch {
                repository.setCuePoint(
                    trackId = loadedTrack.id,
                    cueIndex = cueIndex,
                    positionMs = posMs,
                    colorHex = color
                )
            }
        }
    }

    fun clearHotCue(deckId: String, cueIndex: Int) {
        val loadedTrack = (if (deckId == "A") loadedTrackA else loadedTrackB) ?: return
        viewModelScope.launch {
            repository.deleteCuePoint(loadedTrack.id, cueIndex)
        }
    }

    // CDJ-3000 Beat Jump
    fun beatJump(deckId: String, beats: Double) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.beatJump(beats)
    }

    fun toggleMasterTempo(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        val target = if (deckId == "A") !_deckAState.value.isKeyLocked else !_deckBState.value.isKeyLocked
        if (deckId == "A") _deckAState.update { it.copy(isKeyLocked = target) }
        else _deckBState.update { it.copy(isKeyLocked = target) }
    }

    fun toggleQuantize(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.quantizeActive = !deck.quantizeActive
        if (deckId == "A") _deckAState.update { it.copy(quantizeActive = deck.quantizeActive) }
        else _deckBState.update { it.copy(quantizeActive = deck.quantizeActive) }
    }

    fun toggleSnap(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.snapActive = !deck.snapActive
        if (deckId == "A") _deckAState.update { it.copy(snapActive = deck.snapActive) }
        else _deckBState.update { it.copy(snapActive = deck.snapActive) }
    }

    fun togglePhraseSync(deckId: String) {
        if (deckId == "A") _deckAState.update { it.copy(phraseSyncActive = !it.phraseSyncActive) }
        else _deckBState.update { it.copy(phraseSyncActive = !it.phraseSyncActive) }
    }

    fun toggleSlipMode(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.slipModeActive = !deck.slipModeActive
        if (deckId == "A") _deckAState.update { it.copy(slipModeActive = deck.slipModeActive) }
        else _deckBState.update { it.copy(slipModeActive = deck.slipModeActive) }
    }

    fun cycleTempoRange(deckId: String) {
        val ranges = listOf("±6%", "±10%", "±16%", "WIDE")
        if (deckId == "A") {
            val currIdx = ranges.indexOf(_deckAState.value.tempoRange).coerceAtLeast(0)
            val nextRange = ranges[(currIdx + 1) % ranges.size]
            _deckAState.update { it.copy(tempoRange = nextRange) }
        } else {
            val currIdx = ranges.indexOf(_deckBState.value.tempoRange).coerceAtLeast(0)
            val nextRange = ranges[(currIdx + 1) % ranges.size]
            _deckBState.update { it.copy(tempoRange = nextRange) }
        }
    }

    // Jog Wheel Actions
    fun onJogTouch(deckId: String, deltaAngle: Float, deltaTimeSec: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.onJogTouch(deltaAngle, deltaTimeSec)
    }

    fun onJogRelease(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.onJogRelease()
    }

    fun onJogNudge(deckId: String, deltaAngle: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.onJogNudge(deltaAngle)
    }

    // Pitch & Tempo Slider
    fun setPitchPercent(deckId: String, percent: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.pitchPercent = percent.toDouble()
        if (deckId == "A") _deckAState.update { it.copy(isSyncActive = false) }
        else _deckBState.update { it.copy(isSyncActive = false) }
    }

    fun resetPitch(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.pitchPercent = 0.0
        if (deckId == "A") _deckAState.update { it.copy(isSyncActive = false) }
        else _deckBState.update { it.copy(isSyncActive = false) }
    }

    fun pitchBend(deckId: String, direction: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.pitchBendOffset = (direction * 4.0).toDouble()
    }

    // Looping
    fun toggleLoop(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.toggleLoop()
    }

    fun setLoopSize(deckId: String, beats: Double) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.setLoop(beats)
    }

    // Allen & Heath Xone:96 4-Band EQ Knobs
    fun setEqHigh(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.xoneEq.highNorm = norm
        deck.eq.highGainNorm = norm
        if (deckId == "A") _deckAState.update { it.copy(eqHighNorm = norm) }
        else _deckBState.update { it.copy(eqHighNorm = norm) }
    }

    fun setEqHighMid(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.xoneEq.highMidNorm = norm
        if (deckId == "A") _deckAState.update { it.copy(eqHighMidNorm = norm) }
        else _deckBState.update { it.copy(eqHighMidNorm = norm) }
    }

    fun setEqLowMid(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.xoneEq.lowMidNorm = norm
        if (deckId == "A") _deckAState.update { it.copy(eqLowMidNorm = norm) }
        else _deckBState.update { it.copy(eqLowMidNorm = norm) }
    }

    fun setEqLow(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.xoneEq.lowNorm = norm
        deck.eq.lowGainNorm = norm
        if (deckId == "A") _deckAState.update { it.copy(eqLowNorm = norm) }
        else _deckBState.update { it.copy(eqLowNorm = norm) }
    }

    // Legacy EQ mid wrapper
    fun setEqMid(deckId: String, norm: Float) {
        setEqHighMid(deckId, norm)
        setEqLowMid(deckId, norm)
    }

    fun setFilterAssign(deckId: String, assign: Int) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.filterAssign = when (assign) {
            1 -> AudioTrackDeck.FilterAssign.FILTER_1
            2 -> AudioTrackDeck.FilterAssign.FILTER_2
            else -> AudioTrackDeck.FilterAssign.OFF
        }
        if (deckId == "A") _deckAState.update { it.copy(filterAssign = assign) }
        else _deckBState.update { it.copy(filterAssign = assign) }
    }

    fun setCrossfaderAssign(deckId: String, assign: String) {
        if (deckId == "A") _deckAState.update { it.copy(crossfaderAssign = assign) }
        else _deckBState.update { it.copy(crossfaderAssign = assign) }
    }

    // Xone:96 Dual VCF Filters
    fun setVcf1Freq(freq: Float) {
        audioEngine.vcf1.frequencyNorm = freq
        _mixerState.update { it.copy(vcf1Freq = freq) }
    }

    fun setVcf1Res(res: Float) {
        audioEngine.vcf1.resonanceNorm = res
        _mixerState.update { it.copy(vcf1Res = res) }
    }

    fun setVcf1Crunch(crunch: Float) {
        audioEngine.vcf1.crunch = crunch
        _mixerState.update { it.copy(vcf1Crunch = crunch) }
    }

    fun setVcf1Type(type: XoneVcfFilter.FilterType) {
        audioEngine.vcf1.filterType = type
        _mixerState.update { it.copy(vcf1Type = type) }
    }

    fun toggleVcf1() {
        audioEngine.vcf1.isEnabled = !audioEngine.vcf1.isEnabled
        _mixerState.update { it.copy(vcf1Active = audioEngine.vcf1.isEnabled) }
    }

    fun setVcf2Freq(freq: Float) {
        audioEngine.vcf2.frequencyNorm = freq
        _mixerState.update { it.copy(vcf2Freq = freq) }
    }

    fun setVcf2Res(res: Float) {
        audioEngine.vcf2.resonanceNorm = res
        _mixerState.update { it.copy(vcf2Res = res) }
    }

    fun setVcf2Crunch(crunch: Float) {
        audioEngine.vcf2.crunch = crunch
        _mixerState.update { it.copy(vcf2Crunch = crunch) }
    }

    fun setVcf2Type(type: XoneVcfFilter.FilterType) {
        audioEngine.vcf2.filterType = type
        _mixerState.update { it.copy(vcf2Type = type) }
    }

    fun toggleVcf2() {
        audioEngine.vcf2.isEnabled = !audioEngine.vcf2.isEnabled
        _mixerState.update { it.copy(vcf2Active = audioEngine.vcf2.isEnabled) }
    }

    fun setFilter(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.filter.position = norm
        if (deckId == "A") _deckAState.update { it.copy(filterNorm = norm) }
        else _deckBState.update { it.copy(filterNorm = norm) }
    }

    fun setGain(deckId: String, norm: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.gain = norm * 2.0f // 0.0 to 2.0
        if (deckId == "A") _deckAState.update { it.copy(gainNorm = norm) }
        else _deckBState.update { it.copy(gainNorm = norm) }
    }

    fun setChannelFader(deckId: String, volume: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.volumeFader = volume
        if (deckId == "A") _deckAState.update { it.copy(faderVolume = volume) }
        else _deckBState.update { it.copy(faderVolume = volume) }
    }

    fun toggleCueListen(deckId: String) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        deck.isCueListen = !deck.isCueListen
        if (deckId == "A") _deckAState.update { it.copy(cueListen = deck.isCueListen) }
        else _deckBState.update { it.copy(cueListen = deck.isCueListen) }
    }

    fun setBoothVolume(vol: Float) {
        _mixerState.update { it.copy(boothVolume = vol) }
    }

    // Mixer & Crossfader
    fun setCrossfader(pos: Float) {
        audioEngine.crossfader = pos
        _mixerState.update { it.copy(crossfader = pos) }
    }

    fun setCrossfaderCurve(curve: AudioEngine.CrossfaderCurve) {
        audioEngine.crossfaderCurve = curve
        _mixerState.update { it.copy(crossfaderCurve = curve) }
    }

    fun setMasterVolume(vol: Float) {
        audioEngine.masterVolume = vol
        _mixerState.update { it.copy(masterVolume = vol) }
    }

    fun toggleRecording() {
        val isRec = audioEngine.toggleRecording()
        _mixerState.update { it.copy(isRecording = isRec, recordingDurationSec = 0) }
    }

    // FX Controls
    fun setFxDryWet(unit: Int, value: Float) {
        val fx = if (unit == 1) audioEngine.fx1 else audioEngine.fx2
        fx.dryWet = value
        if (unit == 1) _fx1State.update { it.copy(dryWet = value) }
        else _fx2State.update { it.copy(dryWet = value) }
    }

    fun setFxParam1(unit: Int, value: Float) {
        val fx = if (unit == 1) audioEngine.fx1 else audioEngine.fx2
        fx.param1 = value
        if (unit == 1) _fx1State.update { it.copy(param1 = value) }
        else _fx2State.update { it.copy(param1 = value) }
    }

    fun setFxParam2(unit: Int, value: Float) {
        val fx = if (unit == 1) audioEngine.fx1 else audioEngine.fx2
        fx.param2 = value
        if (unit == 1) _fx1State.update { it.copy(param2 = value) }
        else _fx2State.update { it.copy(param2 = value) }
    }

    fun setFxParam3(unit: Int, value: Float) {
        val fx = if (unit == 1) audioEngine.fx1 else audioEngine.fx2
        fx.param3 = value
        if (unit == 1) _fx1State.update { it.copy(param3 = value) }
        else _fx2State.update { it.copy(param3 = value) }
    }

    fun toggleFxAssign(unit: Int, deck: String) {
        if (unit == 1) {
            if (deck == "A") {
                audioEngine.fx1AssignDeckA = !audioEngine.fx1AssignDeckA
                _fx1State.update { it.copy(assignDeckA = audioEngine.fx1AssignDeckA) }
            } else {
                audioEngine.fx1AssignDeckB = !audioEngine.fx1AssignDeckB
                _fx1State.update { it.copy(assignDeckB = audioEngine.fx1AssignDeckB) }
            }
        } else {
            if (deck == "A") {
                audioEngine.fx2AssignDeckA = !audioEngine.fx2AssignDeckA
                _fx2State.update { it.copy(assignDeckA = audioEngine.fx2AssignDeckA) }
            } else {
                audioEngine.fx2AssignDeckB = !audioEngine.fx2AssignDeckB
                _fx2State.update { it.copy(assignDeckB = audioEngine.fx2AssignDeckB) }
            }
        }
    }

    // Sampler Actions
    fun triggerSample(index: Int) {
        audioEngine.sampler.triggerSlot(index)
    }

    fun stopSample(index: Int) {
        audioEngine.sampler.stopSlot(index)
    }

    fun seekDeckTo(deckId: String, progress: Float) {
        val deck = if (deckId == "A") audioEngine.deckA else audioEngine.deckB
        val targetMs = (deck.durationMs * progress).toLong()
        deck.seekToMs(targetMs)
    }
}
