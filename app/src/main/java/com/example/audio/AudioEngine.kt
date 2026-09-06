package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class AudioEngine(val sampleRate: Int = 44100) {

    enum class CrossfaderCurve {
        SMOOTH, // Constant power sine/cosine
        SCRATCH // Sharp cut curve
    }

    val deckA = AudioTrackDeck("A", sampleRate)
    val deckB = AudioTrackDeck("B", sampleRate)
    val sampler = AudioSampler(sampleRate)

    // Allen & Heath Xone:96 Dual VCF Filters with CRUNCH
    val vcf1 = XoneVcfFilter(sampleRate.toFloat())
    val vcf2 = XoneVcfFilter(sampleRate.toFloat())

    val fx1 = FxProcessor(sampleRate.toFloat())
    val fx2 = FxProcessor(sampleRate.toFloat())

    // FX Routing assignments
    var fx1AssignDeckA: Boolean = true
    var fx1AssignDeckB: Boolean = false
    var fx2AssignDeckA: Boolean = false
    var fx2AssignDeckB: Boolean = true

    // Mixer & Master Controls
    var crossfader: Float = 0.5f // 0.0 (Deck A) .. 0.5 (Center) .. 1.0 (Deck B)
    var crossfaderCurve: CrossfaderCurve = CrossfaderCurve.SMOOTH

    var masterVolume: Float = 0.9f
    var cueMix: Float = 0.5f // Headphone Cue / Master blend
    var cueVolume: Float = 0.8f

    // Metering state (0.0 to 1.0)
    var meterDeckA: Float = 0.0f
        private set
    var meterDeckB: Float = 0.0f
        private set
    var meterMasterL: Float = 0.0f
        private set
    var meterMasterR: Float = 0.0f
        private set

    // XRun and latency diagnostics
    var underrunCount: Int = 0
        private set

    // AudioTrack and Threading
    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    // Recording buffer
    var isRecording: Boolean = false
    private val recordingBuffer = ArrayList<Short>()

    fun start() {
        if (isRunning.get()) return

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferFrames = max(512, minBufSize / (2 * 4))

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferFrames * 2 * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack?.play()
        isRunning.set(true)

        audioThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runAudioLoop(bufferFrames)
        }.apply {
            name = "TraktorAudioRenderThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore on shutdown
        }
        audioTrack = null
    }

    private fun runAudioLoop(chunkSizeFrames: Int) {
        val floatBuffer = FloatArray(chunkSizeFrames * 2)

        var peakPeakA = 0.0f
        var peakPeakB = 0.0f
        var peakPeakML = 0.0f
        var peakPeakMR = 0.0f

        while (isRunning.get()) {
            // Calculate crossfader gains
            val cf = crossfader.coerceIn(0f, 1f)
            val gainA: Float
            val gainB: Float

            if (crossfaderCurve == CrossfaderCurve.SMOOTH) {
                // Constant power curve
                gainA = cos((cf * PI * 0.5).toDouble()).toFloat()
                gainB = sin((cf * PI * 0.5).toDouble()).toFloat()
            } else {
                // Scratch cut curve (instant full level within 8%)
                gainA = (1.0f - (cf - 0.5f).coerceAtLeast(0f) * 2.0f).coerceIn(0f, 1f)
                gainB = (cf * 2.0f).coerceIn(0f, 1f)
            }

            for (frame in 0 until chunkSizeFrames) {
                // 1. Render Deck A
                var (sAL, sAR) = deckA.renderNextSample()
                when (deckA.filterAssign) {
                    AudioTrackDeck.FilterAssign.FILTER_1 -> {
                        val f = vcf1.process(sAL, sAR)
                        sAL = f.first
                        sAR = f.second
                    }
                    AudioTrackDeck.FilterAssign.FILTER_2 -> {
                        val f = vcf2.process(sAL, sAR)
                        sAL = f.first
                        sAR = f.second
                    }
                    AudioTrackDeck.FilterAssign.OFF -> {}
                }
                if (fx1AssignDeckA) {
                    val p = fx1.process(sAL, sAR)
                    sAL = p.first
                    sAR = p.second
                }
                if (fx2AssignDeckA) {
                    val p = fx2.process(sAL, sAR)
                    sAL = p.first
                    sAR = p.second
                }

                // 2. Render Deck B
                var (sBL, sBR) = deckB.renderNextSample()
                when (deckB.filterAssign) {
                    AudioTrackDeck.FilterAssign.FILTER_1 -> {
                        val f = vcf1.process(sBL, sBR)
                        sBL = f.first
                        sBR = f.second
                    }
                    AudioTrackDeck.FilterAssign.FILTER_2 -> {
                        val f = vcf2.process(sBL, sBR)
                        sBL = f.first
                        sBR = f.second
                    }
                    AudioTrackDeck.FilterAssign.OFF -> {}
                }
                if (fx1AssignDeckB) {
                    val p = fx1.process(sBL, sBR)
                    sBL = p.first
                    sBR = p.second
                }
                if (fx2AssignDeckB) {
                    val p = fx2.process(sBL, sBR)
                    sBL = p.first
                    sBR = p.second
                }

                // 3. Render Sampler
                val (sampL, sampR) = sampler.renderNextSample()

                // Peak tracking for Deck A & B
                val levelA = max(abs(sAL), abs(sAR))
                val levelB = max(abs(sBL), abs(sBR))
                if (levelA > peakPeakA) peakPeakA = levelA
                if (levelB > peakPeakB) peakPeakB = levelB

                // 4. Mix through Crossfader + Sampler
                var mixL = (sAL * gainA) + (sBL * gainB) + sampL
                var mixR = (sAR * gainA) + (sBR * gainB) + sampR

                // Master Volume
                mixL *= masterVolume
                mixR *= masterVolume

                // Soft Limiter / Tanh Saturation to prevent harsh digital clipping
                mixL = tanh(mixL.toDouble()).toFloat()
                mixR = tanh(mixR.toDouble()).toFloat()

                // Track Master peaks
                if (abs(mixL) > peakPeakML) peakPeakML = abs(mixL)
                if (abs(mixR) > peakPeakMR) peakPeakMR = abs(mixR)

                val outIdx = frame * 2
                floatBuffer[outIdx] = mixL
                floatBuffer[outIdx + 1] = mixR

                // Recording
                if (isRecording) {
                    val pcmShortL = (mixL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                    val pcmShortR = (mixR * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                    synchronized(recordingBuffer) {
                        recordingBuffer.add(pcmShortL)
                        recordingBuffer.add(pcmShortR)
                    }
                }
            }

            // Write to AudioTrack
            val written = audioTrack?.write(
                floatBuffer,
                0,
                floatBuffer.size,
                AudioTrack.WRITE_BLOCKING
            ) ?: 0

            // Check for underruns
            audioTrack?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    underrunCount = it.underrunCount
                }
            }

            // Smooth decay on meters
            meterDeckA = (meterDeckA * 0.7f + peakPeakA * 0.3f).coerceIn(0f, 1f)
            meterDeckB = (meterDeckB * 0.7f + peakPeakB * 0.3f).coerceIn(0f, 1f)
            meterMasterL = (meterMasterL * 0.7f + peakPeakML * 0.3f).coerceIn(0f, 1f)
            meterMasterR = (meterMasterR * 0.7f + peakPeakMR * 0.3f).coerceIn(0f, 1f)

            peakPeakA *= 0.5f
            peakPeakB *= 0.5f
            peakPeakML *= 0.5f
            peakPeakMR *= 0.5f
        }
    }

    /**
     * Beat Sync Engine: Syncs slave deck BPM and phase to master deck
     */
    fun syncDecks(masterDeck: AudioTrackDeck, slaveDeck: AudioTrackDeck) {
        slaveDeck.bpm = masterDeck.currentEffectiveBpm
        slaveDeck.pitchPercent = 0.0

        // Phase alignment
        val masterPhase = (masterDeck.positionMs % (60000.0 / masterDeck.currentEffectiveBpm))
        val slaveBeatDuration = 60000.0 / slaveDeck.bpm
        val currentSlaveBeat = slaveDeck.positionMs / slaveBeatDuration.toLong()
        val targetMs = (currentSlaveBeat * slaveBeatDuration + masterPhase).toLong()
        slaveDeck.seekToMs(targetMs)
    }

    fun toggleRecording(): Boolean {
        isRecording = !isRecording
        if (!isRecording) {
            // Finished recording session
        }
        return isRecording
    }

    fun getRecordedSampleCount(): Int {
        return synchronized(recordingBuffer) { recordingBuffer.size }
    }
}
