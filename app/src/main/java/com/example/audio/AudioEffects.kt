package com.example.audio

import kotlin.math.*

/**
 * Allen & Heath Xone:96 4-Band British Equalizer
 * HI: 10 kHz High-Shelf (+6dB / -infinity kill)
 * HM: 2.7 kHz High-Mid Peaking (+10dB / -30dB)
 * LM: 270 Hz Low-Mid Peaking (+10dB / -30dB)
 * LO: 60 Hz Low-Shelf (+6dB / -infinity kill)
 */
class XoneChannelEq(private val sampleRate: Float = 44100f) {
    private val lowFilter = BiquadFilter()
    private val lowMidFilter = BiquadFilter()
    private val highMidFilter = BiquadFilter()
    private val highFilter = BiquadFilter()

    // 0.0 to 1.0 (0.5 = 0dB unity)
    var lowNorm = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    var lowMidNorm = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    var highMidNorm = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    var highNorm = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    var killLow = false
        set(value) {
            field = value
            updateCoefficients()
        }

    var killHigh = false
        set(value) {
            field = value
            updateCoefficients()
        }

    init {
        updateCoefficients()
    }

    private fun shelfDb(norm: Float, isKill: Boolean): Float {
        if (isKill || norm <= 0.02f) return -70.0f
        return if (norm <= 0.5f) {
            // 0.0 -> -70 dB kill, 0.5 -> 0 dB
            -50.0f * (1.0f - norm * 2.0f)
        } else {
            // 0.5 -> 0 dB, 1.0 -> +6 dB
            6.0f * ((norm - 0.5f) * 2.0f)
        }
    }

    private fun bellDb(norm: Float): Float {
        return if (norm <= 0.5f) {
            // 0.0 -> -30 dB, 0.5 -> 0 dB
            -30.0f * (1.0f - norm * 2.0f)
        } else {
            // 0.5 -> 0 dB, 1.0 -> +10 dB
            10.0f * ((norm - 0.5f) * 2.0f)
        }
    }

    private fun updateCoefficients() {
        val lowDb = shelfDb(lowNorm, killLow)
        val lowMidDb = bellDb(lowMidNorm)
        val highMidDb = bellDb(highMidNorm)
        val highDb = shelfDb(highNorm, killHigh)

        lowFilter.configure(BiquadFilter.Type.LOW_SHELF, sampleRate, 60f, 0.707f, lowDb)
        lowMidFilter.configure(BiquadFilter.Type.PEAKING_EQ, sampleRate, 270f, 1.2f, lowMidDb)
        highMidFilter.configure(BiquadFilter.Type.PEAKING_EQ, sampleRate, 2700f, 1.2f, highMidDb)
        highFilter.configure(BiquadFilter.Type.HIGH_SHELF, sampleRate, 10000f, 0.707f, highDb)
    }

    fun processLeft(input: Float): Float {
        val f1 = lowFilter.processLeft(input)
        val f2 = lowMidFilter.processLeft(f1)
        val f3 = highMidFilter.processLeft(f2)
        return highFilter.processLeft(f3)
    }

    fun processRight(input: Float): Float {
        val f1 = lowFilter.processRight(input)
        val f2 = lowMidFilter.processRight(f1)
        val f3 = highMidFilter.processRight(f2)
        return highFilter.processRight(f3)
    }
}

/**
 * Legacy 3-band adapter delegating to 4-band EQ for backward compatibility
 */
class ChannelEq(sampleRate: Float = 44100f) {
    val xone = XoneChannelEq(sampleRate)

    var lowGainNorm: Float
        get() = xone.lowNorm
        set(value) { xone.lowNorm = value }

    var midGainNorm: Float
        get() = (xone.lowMidNorm + xone.highMidNorm) / 2f
        set(value) {
            xone.lowMidNorm = value
            xone.highMidNorm = value
        }

    var highGainNorm: Float
        get() = xone.highNorm
        set(value) { xone.highNorm = value }

    var killLow: Boolean
        get() = xone.killLow
        set(value) { xone.killLow = value }

    var killMid: Boolean = false
        set(value) {
            field = value
            if (value) {
                xone.lowMidNorm = 0f
                xone.highMidNorm = 0f
            }
        }

    var killHigh: Boolean
        get() = xone.killHigh
        set(value) { xone.killHigh = value }

    fun processLeft(input: Float): Float = xone.processLeft(input)
    fun processRight(input: Float): Float = xone.processRight(input)
}

/**
 * Allen & Heath Xone:96 Voltage-Controlled Filter (VCF)
 * Features HPF / BPF / LPF modes, sweepable Frequency (20Hz - 20kHz),
 * Resonance Q control, and Xone:96 signature CRUNCH harmonic distortion!
 */
class XoneVcfFilter(val sampleRate: Float = 44100f) {
    enum class FilterType {
        HPF,
        BPF,
        LPF
    }

    var isEnabled: Boolean = false
    var filterType: FilterType = FilterType.LPF
        set(value) {
            field = value
            updateCoefficients()
        }

    // 0.0 (20 Hz) to 1.0 (20,000 Hz) logarithmic sweep
    var frequencyNorm: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    // Resonance Q (0.0 = mild, 1.0 = self-oscillating wild)
    var resonanceNorm: Float = 0.35f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateCoefficients()
        }

    // CRUNCH: Xone:96 harmonic analog saturation/overdrive
    var crunch: Float = 0.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private val stage1 = BiquadFilter()
    private val stage2 = BiquadFilter()

    init {
        updateCoefficients()
    }

    private fun updateCoefficients() {
        // Logarithmic frequency mapping from 25 Hz to 18,000 Hz
        val minF = 25.0
        val maxF = 18500.0
        val freq = (minF * (maxF / minF).pow(frequencyNorm.toDouble())).toFloat().coerceIn(20f, 20000f)
        val q = 0.707f + resonanceNorm * 3.5f

        when (filterType) {
            FilterType.HPF -> {
                stage1.configure(BiquadFilter.Type.HIGH_PASS, sampleRate, freq, q)
                stage2.configure(BiquadFilter.Type.HIGH_PASS, sampleRate, freq, 0.707f)
            }
            FilterType.LPF -> {
                stage1.configure(BiquadFilter.Type.LOW_PASS, sampleRate, freq, q)
                stage2.configure(BiquadFilter.Type.LOW_PASS, sampleRate, freq, 0.707f)
            }
            FilterType.BPF -> {
                // BPF approximated by cascading HPF and LPF around center frequency
                val bw = 0.5f + (1.0f - resonanceNorm) * 0.8f
                val hpFreq = (freq * (1.0f - bw * 0.4f)).coerceAtLeast(20f)
                val lpFreq = (freq * (1.0f + bw * 0.4f)).coerceAtMost(20000f)
                stage1.configure(BiquadFilter.Type.HIGH_PASS, sampleRate, hpFreq, q)
                stage2.configure(BiquadFilter.Type.LOW_PASS, sampleRate, lpFreq, q)
            }
        }
    }

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!isEnabled) return sampleL to sampleR

        // 1. Filter stages
        var fL = stage2.processLeft(stage1.processLeft(sampleL))
        var fR = stage2.processRight(stage1.processRight(sampleR))

        // 2. CRUNCH analog saturation (Xone:96 signature drive)
        if (crunch > 0.01f) {
            val drive = 1.0f + (crunch * 3.5f)
            // Asymmetric warm clipping with subtle second harmonics
            val drivenL = fL * drive
            val drivenR = fR * drive
            fL = tanh((drivenL + 0.05f * drivenL * drivenL).toDouble()).toFloat()
            fR = tanh((drivenR + 0.05f * drivenR * drivenR).toDouble()).toFloat()
        }

        return fL to fR
    }
}

/**
 * Traktor style Bi-directional Filter (HPF / LPF)
 * 0.0 -> Extreme Low Pass (80 Hz)
 * 0.5 -> Neutral / Bypassed
 * 1.0 -> Extreme High Pass (8000 Hz)
 */
class ChannelFilter(private val sampleRate: Float = 44100f) {
    private val filter1 = BiquadFilter()
    private val filter2 = BiquadFilter()

    var position = 0.5f // 0.0 (LPF) .. 0.5 (Off) .. 1.0 (HPF)
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateFilter()
        }

    init {
        updateFilter()
    }

    private fun updateFilter() {
        if (position in 0.47f..0.53f) {
            // Center deadband - flat bypass
            filter1.reset()
            filter2.reset()
            return
        }

        if (position < 0.47f) {
            // Low Pass: sweeps from 18000Hz down to 80Hz
            val norm = position / 0.47f // 0.0 to 1.0
            val cutoff = (80.0 * 2.0.pow(norm * 8.0)).toFloat().coerceIn(60f, 20000f)
            filter1.configure(BiquadFilter.Type.LOW_PASS, sampleRate, cutoff, 1.2f)
            filter2.configure(BiquadFilter.Type.LOW_PASS, sampleRate, cutoff, 0.8f)
        } else {
            // High Pass: sweeps from 30Hz up to 7000Hz
            val norm = (position - 0.53f) / 0.47f // 0.0 to 1.0
            val cutoff = (30.0 * 2.0.pow(norm * 8.0)).toFloat().coerceIn(30f, 12000f)
            filter1.configure(BiquadFilter.Type.HIGH_PASS, sampleRate, cutoff, 1.2f)
            filter2.configure(BiquadFilter.Type.HIGH_PASS, sampleRate, cutoff, 0.8f)
        }
    }

    fun processLeft(input: Float): Float {
        if (position in 0.47f..0.53f) return input
        return filter2.processLeft(filter1.processLeft(input))
    }

    fun processRight(input: Float): Float {
        if (position in 0.47f..0.53f) return input
        return filter2.processRight(filter1.processRight(input))
    }
}

/**
 * Multi-FX Module (Delay, Flanger, Beat Repeat)
 */
class FxProcessor(private val sampleRate: Float = 44100f) {
    enum class Mode {
        DELAY,
        FLANGER,
        REVERB
    }

    var mode: Mode = Mode.DELAY
    var dryWet: Float = 0.0f // 0.0 = Dry, 1.0 = Wet
    var param1: Float = 0.5f // Delay time / Flanger rate
    var param2: Float = 0.5f // Feedback / Resonance
    var param3: Float = 0.5f // Damping / Modulation depth

    // Delay buffer (max 1.5 seconds)
    private val bufferSize = (sampleRate * 1.5f).toInt()
    private val delayBufferL = FloatArray(bufferSize)
    private val delayBufferR = FloatArray(bufferSize)
    private var writePos = 0

    // Flanger LFO phase
    private var lfoPhase = 0.0f

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (dryWet <= 0.001f) {
            return sampleL to sampleR
        }

        var wetL = sampleL
        var wetR = sampleR

        when (mode) {
            Mode.DELAY -> {
                // Delay time based on param1 (e.g. 50ms to 800ms)
                val delaySamples = ((0.05f + param1 * 0.75f) * sampleRate).toInt().coerceIn(100, bufferSize - 1)
                var readPos = writePos - delaySamples
                if (readPos < 0) readPos += bufferSize

                val delayedL = delayBufferL[readPos]
                val delayedR = delayBufferR[readPos]

                val feedback = (param2 * 0.85f).coerceIn(0f, 0.95f)
                delayBufferL[writePos] = (sampleL + delayedL * feedback).coerceIn(-2f, 2f)
                delayBufferR[writePos] = (sampleR + delayedR * feedback).coerceIn(-2f, 2f)

                wetL = delayedL
                wetR = delayedR
            }

            Mode.FLANGER -> {
                // LFO sweeps delay between 1ms and 8ms
                val lfoSpeed = 0.1f + param1 * 2.5f // Hz
                lfoPhase += (2.0f * PI.toFloat() * lfoSpeed) / sampleRate
                if (lfoPhase > 2.0f * PI) lfoPhase -= (2.0f * PI.toFloat())

                val delayTimeMs = 2.0f + (sin(lfoPhase) * 0.5f + 0.5f) * 6.0f * param3
                val delaySamples = (delayTimeMs * 0.001f * sampleRate).toInt().coerceIn(10, 800)

                var readPos = writePos - delaySamples
                if (readPos < 0) readPos += bufferSize

                val delayedL = delayBufferL[readPos]
                val delayedR = delayBufferR[readPos]

                val feedback = (param2 * 0.7f).coerceIn(0f, 0.9f)
                delayBufferL[writePos] = sampleL + delayedL * feedback
                delayBufferR[writePos] = sampleR + delayedR * feedback

                wetL = (sampleL + delayedL) * 0.7f
                wetR = (sampleR + delayedR) * 0.7f
            }

            Mode.REVERB -> {
                // Fast comb-filter diffusion approximation
                val d1 = (0.035f * sampleRate).toInt()
                val d2 = (0.051f * sampleRate).toInt()

                var r1 = writePos - d1; if (r1 < 0) r1 += bufferSize
                var r2 = writePos - d2; if (r2 < 0) r2 += bufferSize

                val feedback = (0.3f + param2 * 0.55f)
                val echo = (delayBufferL[r1] + delayBufferR[r2]) * 0.5f

                delayBufferL[writePos] = sampleL + echo * feedback
                delayBufferR[writePos] = sampleR + echo * feedback

                wetL = sampleL * 0.5f + echo * 0.8f
                wetR = sampleR * 0.5f + echo * 0.8f
            }
        }

        writePos = (writePos + 1) % bufferSize

        val outL = sampleL * (1.0f - dryWet) + wetL * dryWet
        val outR = sampleR * (1.0f - dryWet) + wetR * dryWet
        return outL to outR
    }
}
