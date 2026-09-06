package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.sin

class DjRepository(
    private val trackDao: TrackDao,
    private val cuePointDao: CuePointDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    fun getCuePointsForTrack(trackId: Long): Flow<List<CuePointEntity>> {
        return cuePointDao.getCuePointsForTrack(trackId)
    }

    suspend fun setCuePoint(trackId: Long, cueIndex: Int, positionMs: Long, colorHex: String) {
        cuePointDao.setCuePoint(
            CuePointEntity(
                trackId = trackId,
                cueIndex = cueIndex,
                positionMs = positionMs,
                colorHex = colorHex,
                label = "CUE $cueIndex"
            )
        )
    }

    suspend fun deleteCuePoint(trackId: Long, cueIndex: Int) {
        cuePointDao.deleteCuePoint(trackId, cueIndex)
    }

    suspend fun insertTrack(track: TrackEntity): Long {
        return trackDao.insertTrack(track)
    }

    suspend fun deleteTrack(trackId: Long) {
        trackDao.deleteTrackById(trackId)
    }

    suspend fun initDefaultTracksIfEmpty() {
        if (trackDao.getTrackCount() == 0) {
            val defaults = listOf(
                TrackEntity(
                    title = "Berlin Warehouse",
                    artist = "Subterranean",
                    bpm = 126.0,
                    initialKey = "8A (Am)",
                    durationMs = 240_000L,
                    genre = "Peak Techno",
                    waveformData = generateSampleWaveform(120, seed = 1)
                ),
                TrackEntity(
                    title = "Sunset Pulse",
                    artist = "Aura Groove",
                    bpm = 124.0,
                    initialKey = "11B (A)",
                    durationMs = 210_000L,
                    genre = "Melodic House",
                    waveformData = generateSampleWaveform(120, seed = 2)
                ),
                TrackEntity(
                    title = "Neon Horizon",
                    artist = "Kinetics",
                    bpm = 128.0,
                    initialKey = "4A (Fm)",
                    durationMs = 225_000L,
                    genre = "Electro Progressive",
                    waveformData = generateSampleWaveform(120, seed = 3)
                ),
                TrackEntity(
                    title = "Deep Resonance",
                    artist = "Modular Mind",
                    bpm = 122.0,
                    initialKey = "2A (Ebm)",
                    durationMs = 260_000L,
                    genre = "Deep Minimal",
                    waveformData = generateSampleWaveform(120, seed = 4)
                )
            )
            val ids = trackDao.insertTracks(defaults)
            // Pre-seed some hot cues for immediate performance
            if (ids.isNotEmpty()) {
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[0], cueIndex = 1, positionMs = 0, colorHex = "#00E5FF", label = "Intro"))
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[0], cueIndex = 2, positionMs = 15238, colorHex = "#00E676", label = "Bass In"))
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[0], cueIndex = 3, positionMs = 30476, colorHex = "#FFD600", label = "Break"))
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[0], cueIndex = 4, positionMs = 45714, colorHex = "#FF1744", label = "Drop"))
            }
            if (ids.size > 1) {
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[1], cueIndex = 1, positionMs = 0, colorHex = "#FF6D00", label = "Start"))
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[1], cueIndex = 2, positionMs = 15483, colorHex = "#FFAB00", label = "Synth"))
                cuePointDao.setCuePoint(CuePointEntity(trackId = ids[1], cueIndex = 3, positionMs = 30967, colorHex = "#00E5FF", label = "Drop"))
            }
        }
    }

    private fun generateSampleWaveform(count: Int, seed: Int): String {
        return (0 until count).map { i ->
            val v = (sin(i * 0.15 + seed) * 0.35 + sin(i * 0.05) * 0.25 + 0.4).coerceIn(0.1, 1.0)
            String.format(java.util.Locale.US, "%.2f", v)
        }.joinToString(",")
    }
}
