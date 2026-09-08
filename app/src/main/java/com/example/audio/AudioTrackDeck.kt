package com.example.audio

import kotlin.math.*

class AudioTrackDeck(
    val deckId: String,
    val sampleRate: Int = 44100
) {
    // Audio Buffer (Stereo interleaved: L0, R0, L1, R1, ...)
    private var pcmBuffer: FloatArray = FloatArray(0)
    var totalSamples: Long = 0
        private set

    // Playback state
    var isPlaying: Boolean = false
    var isCueHeld: Boolean = false
    var currentSamplePos: Double = 0.0
    var cueSamplePos: Long = 0

    // Pitch & Tempo
    var bpm: Double = 126.0
    var pitchPercent: Double = 0.0 // -16.0 to +16.0%
    var pitchRange: Double = 10.0 // +/- 10%
    var pitchBendOffset: Double = 0.0 // Temporary +/- nudge
    var isKeyLock: Boolean = false
    var isSync: Boolean = false

    // Jog wheel & Scratch physics
    var isScratching: Boolean = false
    var scratchVelocity: Double = 0.0 // Multiplier of normal speed (-5.0 to +5.0)
    var jogWheelAngleDeg: Float = 0.0f
    private var scratchInertia: Double = 0.0

    // Looping
    var isLooping: Boolean = false
    var loopStartSample: Long = 0
    var loopEndSample: Long = 0
    var loopBeatLength: Double = 4.0 // 4 beats loop by default

    // Xone:96 Filter Assign
    enum class FilterAssign {
        OFF,
        FILTER_1,
        FILTER_2
    }
    var filterAssign: FilterAssign = FilterAssign.OFF

    // CDJ-3000 Performance Features
    var jogFeel: Float = 0.5f // 0.0 (Light) to 1.0 (Heavy)
    var vinylSpeedAdjust: Float = 0.5f
    var quantizeActive: Boolean = true
    var snapActive: Boolean = true
    var slipModeActive: Boolean = false
    private var slipSamplePos: Double = 0.0

    private var pendingSeekSamplePos: Long? = null
    private var actionBeatSampleThreshold: Long = -1L

    fun getNearestBeatSample(samplePos: Double): Long {
        val samplesPerBeat = sampleRate * 60.0 / bpm
        val beatIndex = round(samplePos / samplesPerBeat)
        return (beatIndex * samplesPerBeat).toLong().coerceIn(0L, totalSamples)
    }

    fun getNextBeatSample(samplePos: Double): Long {
        val samplesPerBeat = sampleRate * 60.0 / bpm
        val beatIndex = ceil(samplePos / samplesPerBeat)
        return (beatIndex * samplesPerBeat).toLong().coerceIn(0L, totalSamples)
    }

    fun seekToMsQuantized(ms: Long) {
        val targetSample = ((ms / 1000.0) * sampleRate).toLong().coerceIn(0L, totalSamples)
        if (quantizeActive && isPlaying) {
            actionBeatSampleThreshold = getNextBeatSample(currentSamplePos)
            pendingSeekSamplePos = targetSample
        } else {
            currentSamplePos = targetSample.toDouble()
        }
    }

    // Signal chain
    var gain: Float = 1.0f // 0.0 to 2.0
    var volumeFader: Float = 0.85f // Channel fader (0.0 to 1.0)
    var isCueListen: Boolean = false // Headphone Cue monitoring
    val xoneEq = XoneChannelEq(sampleRate.toFloat())
    val eq = ChannelEq(sampleRate.toFloat())
    val filter = ChannelFilter(sampleRate.toFloat())

    // VU meter level tracking
    var currentPeakL: Float = 0.0f
        private set
    var currentPeakR: Float = 0.0f
        private set

    val positionMs: Long
        get() = ((currentSamplePos / sampleRate) * 1000).toLong()

    val durationMs: Long
        get() = ((totalSamples / sampleRate) * 1000)

    val currentEffectiveBpm: Double
        get() = bpm * (1.0 + (pitchPercent / 100.0))

    fun loadProceduralTrack(trackBpm: Double, trackGenre: String, keyNote: String) {
        bpm = trackBpm
        val durationSeconds = 180 // 3 minutes of looping groove
        val totalStereoFrames = sampleRate * durationSeconds
        val buffer = FloatArray(totalStereoFrames * 2)

        // Generate synthetic drum and bass sequence
        val samplesPerBeat = (sampleRate * 60.0 / trackBpm)
        val samplesPerBar = samplesPerBeat * 4.0

        val baseFreq = when {
            keyNote.contains("Am") || keyNote.contains("8A") -> 55.0 // A1
            keyNote.contains("Fm") || keyNote.contains("4A") -> 43.65 // F1
            keyNote.contains("Ebm") || keyNote.contains("2A") -> 38.89 // Eb1
            else -> 48.99 // G1
        }

        for (frame in 0 until totalStereoFrames) {
            val t = frame.toDouble() / sampleRate
            val beatPhase = (frame % samplesPerBeat) / samplesPerBeat
            val barPhase = (frame % samplesPerBar) / samplesPerBar
            val beatIndex = ((frame % samplesPerBar) / samplesPerBeat).toInt()

            var left = 0.0
            var right = 0.0

            // 1. Four-on-the-floor Kick drum (every beat)
            val kickDecay = exp(-beatPhase * 18.0)
            if (kickDecay > 0.001) {
                val kickPitch = 50.0 + 110.0 * exp(-beatPhase * 40.0)
                val kickOsc = sin(2.0 * PI * kickPitch * (beatPhase * (60.0 / trackBpm)))
                val kickSample = kickOsc * kickDecay * 0.7
                left += kickSample
                right += kickSample
            }

            // 2. Snare / Clap on beats 2 and 4 (indices 1 and 3)
            if (beatIndex == 1 || beatIndex == 3) {
                val snarePhase = beatPhase
                val snareDecay = exp(-snarePhase * 12.0)
                if (snareDecay > 0.001) {
                    val noise = (Math.random() * 2.0 - 1.0)
                    val tone = sin(2.0 * PI * 180.0 * snarePhase) * 0.3
                    val snareSample = (noise * 0.7 + tone) * snareDecay * 0.45
                    left += snareSample * 0.9
                    right += snareSample * 0.95
                }
            }

            // 3. Off-beat Hi-Hat (on the "&" between beats)
            val hatPhase = (beatPhase + 0.5) % 1.0
            val hatDecay = exp(-hatPhase * 35.0)
            if (hatDecay > 0.001) {
                val hatNoise = (Math.random() * 2.0 - 1.0)
                val hatSample = hatNoise * hatDecay * 0.35
                left += hatSample * 0.7
                right += hatSample * 1.1
            }

            // 4. Rolling Sub-bassline
            val bassPattern = when ((beatPhase * 4.0).toInt()) {
                0 -> 1.0
                1 -> 0.8
                2 -> 1.0
                else -> 0.6
            }
            val bassOctave = if (barPhase > 0.75) 1.5 else 1.0
            val bassFreq = baseFreq * bassOctave
            val bassEnv = exp(-(beatPhase % 0.25) * 8.0) * bassPattern
            val bassOsc = sin(2.0 * PI * bassFreq * t) + 0.3 * sin(2.0 * PI * bassFreq * 2.0 * t)
            val bassSample = bassOsc * bassEnv * 0.45
            left += bassSample
            right += bassSample

            // 5. Melodic Synth stab / arpeggio (every 16th note)
            val synthNote = when (((frame / (samplesPerBeat / 4.0)).toInt()) % 16) {
                0, 3 -> baseFreq * 4.0
                5, 7 -> baseFreq * 4.75
                10, 12 -> baseFreq * 6.0
                14 -> baseFreq * 5.33
                else -> 0.0
            }
            if (synthNote > 0.0) {
                val subPhase = (frame % (samplesPerBeat / 4.0)) / (samplesPerBeat / 4.0)
                val synthEnv = exp(-subPhase * 10.0)
                val saw = (sin(2.0 * PI * synthNote * t) + 0.5 * sin(4.0 * PI * synthNote * t))
                val synthSample = saw * synthEnv * 0.22
                // Stereo pan modulation
                left += synthSample * (0.5 + 0.4 * sin(barPhase * 2.0 * PI))
                right += synthSample * (0.5 - 0.4 * sin(barPhase * 2.0 * PI))
            }

            buffer[frame * 2] = left.toFloat().coerceIn(-1.0f, 1.0f)
            buffer[frame * 2 + 1] = right.toFloat().coerceIn(-1.0f, 1.0f)
        }

        synchronized(this) {
            pcmBuffer = buffer
            totalSamples = totalStereoFrames.toLong()
            currentSamplePos = 0.0
            cueSamplePos = 0
            isLooping = false
        }
    }

    fun loadCustomPcm(samples: FloatArray, customBpm: Double) {
        synchronized(this) {
            pcmBuffer = samples
            totalSamples = (samples.size / 2).toLong()
            currentSamplePos = 0.0
            cueSamplePos = 0
            bpm = customBpm
            isLooping = false
        }
    }

    fun setLoop(beats: Double) {
        loopBeatLength = beats
        val samplesPerBeat = sampleRate * 60.0 / bpm
        val loopLengthSamples = (samplesPerBeat * beats).toLong()

        if (quantizeActive) {
            loopStartSample = getNearestBeatSample(currentSamplePos)
        } else {
            loopStartSample = currentSamplePos.toLong()
        }
        
        loopEndSample = (loopStartSample + loopLengthSamples).coerceAtMost(totalSamples)
        isLooping = true
    }

    fun toggleLoop() {
        if (isLooping) {
            isLooping = false
        } else {
            setLoop(loopBeatLength)
        }
    }

    /**
     * CDJ-3000 Beat Jump: jump forward or backward by beat count
     */
    fun beatJump(beats: Double) {
        val samplesPerBeat = sampleRate * 60.0 / bpm
        val deltaSamples = beats * samplesPerBeat
        currentSamplePos = (currentSamplePos + deltaSamples).coerceIn(0.0, totalSamples.toDouble())
    }

    fun seekToMs(ms: Long) {
        val targetSample = ((ms / 1000.0) * sampleRate).toLong().coerceIn(0L, totalSamples)
        currentSamplePos = targetSample.toDouble()
    }

    fun cuePress() {
        if (!isPlaying) {
            // In paused state, pressing CUE sets new cue point
            cueSamplePos = currentSamplePos.toLong()
            isCueHeld = true
        } else {
            // During playback, jump back to cue and pause
            isPlaying = false
            currentSamplePos = cueSamplePos.toDouble()
        }
    }

    fun cueRelease() {
        if (isCueHeld) {
            isCueHeld = false
            currentSamplePos = cueSamplePos.toDouble()
        }
    }

    fun playPause() {
        isPlaying = !isPlaying
        if (isPlaying) {
            isCueHeld = false
        }
    }

    /**
     * Called during jog wheel touch
     * @param angularVelocityDegPerSec Speed of rotation in degrees/second
     */
    fun onJogTouch(deltaAngleDeg: Float, deltaTimeSec: Float) {
        isScratching = true
        jogWheelAngleDeg = (jogWheelAngleDeg + deltaAngleDeg) % 360f
        if (deltaTimeSec > 0.001f) {
            val degPerSec = deltaAngleDeg / deltaTimeSec
            // 33.33 RPM = 200 deg/sec normal vinyl speed
            scratchVelocity = degPerSec / 200.0
            scratchInertia = scratchVelocity
        }
    }

    fun onJogRelease() {
        isScratching = false
        // Inertia will decay in the audio process loop
    }

    fun onJogNudge(deltaDeg: Float) {
        jogWheelAngleDeg = (jogWheelAngleDeg + deltaDeg) % 360f
        pitchBendOffset = (deltaDeg * 0.08).coerceIn(-15.0, 15.0)
    }

    /**
     * Renders next audio frame (Stereo Left & Right) into the provided buffers
     */
    fun renderNextSample(): Pair<Float, Float> {
        if (totalSamples == 0L || pcmBuffer.isEmpty()) {
            return 0f to 0f
        }

        // Determine effective speed multiplier
        val activePlay = isPlaying || isCueHeld || isScratching || abs(scratchInertia) > 0.05

        if (!activePlay) {
            return 0f to 0f
        }

        val speedMultiplier: Double = if (isScratching) {
            scratchVelocity
        } else if (abs(scratchInertia) > 0.05) {
            scratchInertia *= 0.9995 // Smooth vinyl deceleration
            scratchInertia
        } else {
            val effectivePitchPercent = pitchPercent + pitchBendOffset
            1.0 + (effectivePitchPercent / 100.0)
        }

        // Decay pitch bend nudge back to 0
        if (pitchBendOffset != 0.0) {
            pitchBendOffset *= 0.999
            if (abs(pitchBendOffset) < 0.001) pitchBendOffset = 0.0
        }

        // Read interpolated sample from buffer
        val readIndex = currentSamplePos
        val indexFloor = floor(readIndex).toLong()
        val frac = (readIndex - indexFloor).toFloat()

        val s0Idx = (indexFloor.coerceIn(0L, totalSamples - 1) * 2).toInt()
        val s1Idx = ((indexFloor + 1).coerceIn(0L, totalSamples - 1) * 2).toInt()

        val rawL = pcmBuffer[s0Idx] * (1.0f - frac) + pcmBuffer[s1Idx] * frac
        val rawR = pcmBuffer[s0Idx + 1] * (1.0f - frac) + pcmBuffer[s1Idx + 1] * frac

        // Advance playhead
        currentSamplePos += speedMultiplier

        // Update jog rotation visual feedback during playback
        if (!isScratching && isPlaying) {
            jogWheelAngleDeg = (jogWheelAngleDeg + (speedMultiplier * 0.4).toFloat()) % 360f
        }

        // Apply Quantize scheduled seeks
        if (pendingSeekSamplePos != null && currentSamplePos >= actionBeatSampleThreshold) {
            currentSamplePos = pendingSeekSamplePos!!.toDouble()
            pendingSeekSamplePos = null
        }

        // Handle Looping
        if (isLooping && loopEndSample > loopStartSample) {
            if (currentSamplePos >= loopEndSample) {
                currentSamplePos = loopStartSample.toDouble() + (currentSamplePos - loopEndSample)
            } else if (currentSamplePos < loopStartSample) {
                currentSamplePos = loopStartSample.toDouble()
            }
        } else {
            // End of track loop or stop
            if (currentSamplePos >= totalSamples) {
                currentSamplePos = 0.0
                if (!isLooping) isPlaying = false
            } else if (currentSamplePos < 0.0) {
                currentSamplePos = 0.0
            }
        }

        // Apply Channel Gain
        val gainedL = rawL * gain
        val gainedR = rawR * gain

        // Apply Allen & Heath Xone 4-Band British EQ
        val eqL = xoneEq.processLeft(gainedL)
        val eqR = xoneEq.processRight(gainedR)

        // Apply Channel Fader Volume
        val outL = eqL * volumeFader
        val outR = eqR * volumeFader

        // Track peak levels for VU meter
        val peakL = abs(outL)
        val peakR = abs(outR)
        currentPeakL = max(currentPeakL * 0.999f, peakL)
        currentPeakR = max(currentPeakR * 0.999f, peakR)

        return outL to outR
    }
}
