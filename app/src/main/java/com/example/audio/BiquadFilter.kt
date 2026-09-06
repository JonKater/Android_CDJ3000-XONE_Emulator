package com.example.audio

import kotlin.math.*

/**
 * Standard Audio EQ Cookbook Biquad Filter (Robert Bristow-Johnson)
 * Supports LowPass, HighPass, BandPass, PeakingEQ, LowShelf, HighShelf
 */
class BiquadFilter {
    enum class Type {
        LOW_PASS,
        HIGH_PASS,
        PEAKING_EQ,
        LOW_SHELF,
        HIGH_SHELF
    }

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // State memory for Stereo channels (Channel 0 = L, Channel 1 = R)
    private var x1L = 0.0f
    private var x2L = 0.0f
    private var y1L = 0.0f
    private var y2L = 0.0f

    private var x1R = 0.0f
    private var x2R = 0.0f
    private var y1R = 0.0f
    private var y2R = 0.0f

    fun reset() {
        x1L = 0f; x2L = 0f; y1L = 0f; y2L = 0f
        x1R = 0f; x2R = 0f; y1R = 0f; y2R = 0f
    }

    fun configure(
        type: Type,
        sampleRate: Float,
        centerFreq: Float,
        q: Float = 0.707f,
        gainDb: Float = 0.0f
    ) {
        val nyquist = sampleRate * 0.495f
        val f0 = centerFreq.coerceIn(20.0f, nyquist)
        val omega = 2.0 * PI * f0 / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * max(0.01f, q))
        val a = 10.0.pow(gainDb / 40.0) // sqrt of 10^(db/20)

        var b0D = 1.0
        var b1D = 0.0
        var b2D = 0.0
        var a0D = 1.0
        var a1D = 0.0
        var a2D = 0.0

        when (type) {
            Type.LOW_PASS -> {
                b0D = (1.0 - cs) / 2.0
                b1D = 1.0 - cs
                b2D = (1.0 - cs) / 2.0
                a0D = 1.0 + alpha
                a1D = -2.0 * cs
                a2D = 1.0 - alpha
            }
            Type.HIGH_PASS -> {
                b0D = (1.0 + cs) / 2.0
                b1D = -(1.0 + cs)
                b2D = (1.0 + cs) / 2.0
                a0D = 1.0 + alpha
                a1D = -2.0 * cs
                a2D = 1.0 - alpha
            }
            Type.PEAKING_EQ -> {
                b0D = 1.0 + alpha * a
                b1D = -2.0 * cs
                b2D = 1.0 - alpha * a
                a0D = 1.0 + alpha / a
                a1D = -2.0 * cs
                a2D = 1.0 - alpha / a
            }
            Type.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                b0D = a * ((a + 1.0) - (a - 1.0) * cs + 2.0 * sqrtA * alpha)
                b1D = 2.0 * a * ((a - 1.0) - (a + 1.0) * cs)
                b2D = a * ((a + 1.0) - (a - 1.0) * cs - 2.0 * sqrtA * alpha)
                a0D = (a + 1.0) + (a - 1.0) * cs + 2.0 * sqrtA * alpha
                a1D = -2.0 * ((a - 1.0) + (a + 1.0) * cs)
                a2D = (a + 1.0) + (a - 1.0) * cs - 2.0 * sqrtA * alpha
            }
            Type.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                b0D = a * ((a + 1.0) + (a - 1.0) * cs + 2.0 * sqrtA * alpha)
                b1D = -2.0 * a * ((a - 1.0) + (a + 1.0) * cs)
                b2D = a * ((a + 1.0) + (a - 1.0) * cs - 2.0 * sqrtA * alpha)
                a0D = (a + 1.0) - (a - 1.0) * cs + 2.0 * sqrtA * alpha
                a1D = 2.0 * ((a - 1.0) - (a + 1.0) * cs)
                a2D = (a + 1.0) - (a - 1.0) * cs - 2.0 * sqrtA * alpha
            }
        }

        val invA0 = 1.0f / a0D.toFloat()
        b0 = (b0D * invA0).toFloat()
        b1 = (b1D * invA0).toFloat()
        b2 = (b2D * invA0).toFloat()
        a1 = (a1D * invA0).toFloat()
        a2 = (a2D * invA0).toFloat()
    }

    fun processLeft(input: Float): Float {
        val output = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = input
        y2L = y1L
        y1L = output
        return output
    }

    fun processRight(input: Float): Float {
        val output = b0 * input + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = input
        y2R = y1R
        y1R = output
        return output
    }
}
