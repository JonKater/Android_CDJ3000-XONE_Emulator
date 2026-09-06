package com.example.audio

import kotlin.math.*

class AudioSampler(private val sampleRate: Int = 44100) {

    data class SampleSlot(
        val id: Int,
        val name: String,
        val colorHex: String,
        var isLooping: Boolean = false,
        var isPlaying: Boolean = false,
        var playhead: Int = 0,
        var buffer: FloatArray = FloatArray(0),
        var volume: Float = 0.8f
    )

    val slots = listOf(
        SampleSlot(1, "808 KICK", "#FF5252"),
        SampleSlot(2, "CLAP HIT", "#FFD740"),
        SampleSlot(3, "HI-HAT", "#40C4FF"),
        SampleSlot(4, "SYNTH STAB", "#E040FB")
    )

    var masterSamplerVolume: Float = 0.85f

    init {
        generateDefaultSamples()
    }

    private fun generateDefaultSamples() {
        // Slot 1: 808 Kick
        val kickLen = (sampleRate * 0.35).toInt()
        val kickBuf = FloatArray(kickLen)
        for (i in 0 until kickLen) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 14.0)
            val pitch = 45.0 + 90.0 * exp(-t * 30.0)
            kickBuf[i] = (sin(2.0 * PI * pitch * t) * decay * 0.85).toFloat()
        }
        slots[0].buffer = kickBuf

        // Slot 2: Clap Hit
        val clapLen = (sampleRate * 0.25).toInt()
        val clapBuf = FloatArray(clapLen)
        for (i in 0 until clapLen) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 18.0)
            val noise = (Math.random() * 2.0 - 1.0)
            clapBuf[i] = (noise * decay * 0.7).toFloat()
        }
        slots[1].buffer = clapBuf

        // Slot 3: Hi-Hat
        val hatLen = (sampleRate * 0.18).toInt()
        val hatBuf = FloatArray(hatLen)
        for (i in 0 until hatLen) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 30.0)
            val noise = (Math.random() * 2.0 - 1.0)
            hatBuf[i] = (noise * decay * 0.5).toFloat()
        }
        slots[2].buffer = hatBuf

        // Slot 4: Synth Stab
        val stabLen = (sampleRate * 0.4).toInt()
        val stabBuf = FloatArray(stabLen)
        for (i in 0 until stabLen) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 8.0)
            val f1 = 440.0
            val f2 = 554.37
            val f3 = 659.25
            val chord = (sin(2 * PI * f1 * t) + sin(2 * PI * f2 * t) + sin(2 * PI * f3 * t)) / 3.0
            stabBuf[i] = (chord * decay * 0.75).toFloat()
        }
        slots[3].buffer = stabBuf
    }

    fun triggerSlot(index: Int) {
        if (index in slots.indices) {
            val slot = slots[index]
            slot.playhead = 0
            slot.isPlaying = true
        }
    }

    fun stopSlot(index: Int) {
        if (index in slots.indices) {
            slots[index].isPlaying = false
        }
    }

    fun renderNextSample(): Pair<Float, Float> {
        var mix = 0.0f
        for (slot in slots) {
            if (slot.isPlaying && slot.buffer.isNotEmpty()) {
                val sample = slot.buffer[slot.playhead] * slot.volume
                mix += sample
                slot.playhead++
                if (slot.playhead >= slot.buffer.size) {
                    if (slot.isLooping) {
                        slot.playhead = 0
                    } else {
                        slot.isPlaying = false
                        slot.playhead = 0
                    }
                }
            }
        }
        val out = mix * masterSamplerVolume
        return out to out
    }
}
