package com.example.core.media

import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive deterministic test suite for audio timeline positioning and gap handling
 * covering test cases A through I as mandated by the VidoPRO specification.
 */
class AudioTimelineMapperTest {

    private fun createAsset(id: String, durationMs: Long = 20000L): Asset {
        return Asset(
            id = id,
            uri = "content://media/$id",
            durationMs = durationMs
        )
    }

    private fun createAudioClip(
        id: String,
        startTimeMs: Long,
        durationMs: Long,
        assetId: String,
        inPointMs: Long = 0L,
        outPointMs: Long = durationMs,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
        isVisible: Boolean = true
    ): Clip {
        return Clip(
            id = id,
            trackId = "track_audio_1",
            type = ClipType.AUDIO,
            assetId = assetId,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = inPointMs,
            outPointMs = outPointMs,
            speed = speed,
            volume = volume,
            isVisible = isVisible
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE A: One audio clip
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseA_OneAudioClip() {
        val asset = createAsset("asset_a")
        val assets = mapOf(asset.id to asset)

        // Clip A: 2s -> 6s (duration 4s), Total video timeline: 10s
        val clipA = createAudioClip("clip_a", 2000L, 4000L, asset.id)
        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA), assets, 10000L)

        assertEquals(3, segments.size)

        // Leading silence: 0s -> 2s
        assertTrue(segments[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(2000L, segments[0].durationMs)

        // Clip A audio: 2s -> 6s
        assertTrue(segments[1] is AudioTimelineSegment.ClipSegment)
        assertEquals(2000L, segments[1].startTimeMs)
        assertEquals(4000L, segments[1].durationMs)

        // Trailing silence: 6s -> 10s
        assertTrue(segments[2] is AudioTimelineSegment.GapSegment)
        assertEquals(6000L, segments[2].startTimeMs)
        assertEquals(4000L, segments[2].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE B: Two consecutive audio clips
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseB_TwoConsecutiveAudioClips() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        // Clip A: 0s -> 5s, Clip B: 5s -> 10s (seamless, no gap)
        val clipA = createAudioClip("clip_a", 0L, 5000L, assetA.id)
        val clipB = createAudioClip("clip_b", 5000L, 5000L, assetB.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB), assets, 10000L)

        assertEquals(2, segments.size)
        assertTrue(segments[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(5000L, segments[0].durationMs)

        assertTrue(segments[1] is AudioTimelineSegment.ClipSegment)
        assertEquals(5000L, segments[1].startTimeMs)
        assertEquals(5000L, segments[1].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE C: Two audio clips with a gap (The canonical example)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseC_TwoAudioClipsWithGap() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        // Clip A: 0s -> 5s, Gap: 5s -> 8s, Clip B: 8s -> 13s
        val clipA = createAudioClip("clip_a", 0L, 5000L, assetA.id)
        val clipB = createAudioClip("clip_b", 8000L, 5000L, assetB.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB), assets, 13000L)

        assertEquals(3, segments.size)

        // Clip A: 0s -> 5s
        val segA = segments[0] as AudioTimelineSegment.ClipSegment
        assertEquals(0L, segA.startTimeMs)
        assertEquals(5000L, segA.durationMs)
        assertEquals("clip_a", segA.clip.id)

        // Gap: 5s -> 8s (3s silence)
        val segGap = segments[1] as AudioTimelineSegment.GapSegment
        assertEquals(5000L, segGap.startTimeMs)
        assertEquals(3000L, segGap.durationMs)

        // Clip B: 8s -> 13s
        val segB = segments[2] as AudioTimelineSegment.ClipSegment
        assertEquals(8000L, segB.startTimeMs)
        assertEquals(5000L, segB.durationMs)
        assertEquals("clip_b", segB.clip.id)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE D: Two overlapping audio clips
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseD_TwoOverlappingAudioClips() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        // Clip A: 0s -> 5s
        // Clip B: 3s -> 8s (overlaps with Clip A between 3s and 5s)
        val clipA = createAudioClip("clip_a", 0L, 5000L, assetA.id)
        val clipB = createAudioClip("clip_b", 3000L, 5000L, assetB.id)

        val layers = AudioTimelineMapper.mapClipsToLayers(listOf(clipA, clipB), assets, 8000L)

        // Must partition into 2 non-overlapping concurrent layers
        assertEquals(2, layers.size)

        // Layer 0: Clip A [0-5s] + Gap [5-8s]
        val layer0 = layers[0]
        assertEquals(2, layer0.size)
        assertTrue(layer0[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, layer0[0].startTimeMs)
        assertEquals(5000L, layer0[0].durationMs)
        assertTrue(layer0[1] is AudioTimelineSegment.GapSegment)
        assertEquals(5000L, layer0[1].startTimeMs)
        assertEquals(3000L, layer0[1].durationMs)

        // Layer 1: Gap [0-3s] + Clip B [3-8s]
        val layer1 = layers[1]
        assertEquals(2, layer1.size)
        assertTrue(layer1[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, layer1[0].startTimeMs)
        assertEquals(3000L, layer1[0].durationMs)
        assertTrue(layer1[1] is AudioTimelineSegment.ClipSegment)
        assertEquals(3000L, layer1[1].startTimeMs)
        assertEquals(5000L, layer1[1].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE E: Moved audio clip
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseE_MovedAudioClip() {
        val asset = createAsset("asset_a")
        val assets = mapOf(asset.id to asset)

        // Initially at 2s -> 7s (duration 5s)
        val originalClip = createAudioClip("clip_a", 2000L, 5000L, asset.id)

        // User moves clip to 7s -> 12s
        val movedClip = originalClip.copy(startTimeMs = 7000L)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(movedClip), assets, 12000L)

        assertEquals(2, segments.size)
        // Leading gap reflects new 7s position
        assertTrue(segments[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(7000L, segments[0].durationMs)

        // Audio plays from 7s to 12s
        assertTrue(segments[1] is AudioTimelineSegment.ClipSegment)
        assertEquals(7000L, segments[1].startTimeMs)
        assertEquals(5000L, segments[1].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE F: Trimmed audio clip
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseF_TrimmedAudioClip() {
        val asset = createAsset("asset_a", 20000L)
        val assets = mapOf(asset.id to asset)

        // Clip trimmed: Starts at 2s on timeline, duration 6s, source trimmed [3s -> 9s]
        val trimmedClip = createAudioClip(
            id = "clip_trimmed",
            startTimeMs = 2000L,
            durationMs = 6000L,
            assetId = asset.id,
            inPointMs = 3000L,
            outPointMs = 9000L,
            speed = 1.0f
        )

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(trimmedClip), assets, 10000L)

        assertEquals(3, segments.size)

        // Leading gap: 0s -> 2s
        assertTrue(segments[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(2000L, segments[0].durationMs)

        // Clip: 2s -> 8s, with source trim [3s -> 9s]
        val clipSeg = segments[1] as AudioTimelineSegment.ClipSegment
        assertEquals(2000L, clipSeg.startTimeMs)
        assertEquals(6000L, clipSeg.durationMs)
        assertEquals(3000L, clipSeg.sourceInPointMs)
        assertEquals(9000L, clipSeg.sourceOutPointMs)

        // Trailing gap: 8s -> 10s
        assertTrue(segments[2] is AudioTimelineSegment.GapSegment)
        assertEquals(8000L, segments[2].startTimeMs)
        assertEquals(2000L, segments[2].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE G: Audio clip after project reload
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseG_AudioClipAfterProjectReload() {
        val asset = createAsset("asset_reloaded", 15000L)
        val assets = mapOf(asset.id to asset)

        // Simulated entity persistence reload
        val restoredClip = Clip(
            id = "persisted_clip",
            trackId = "track_audio_persisted",
            type = ClipType.AUDIO,
            assetId = asset.id,
            startTimeMs = 4000L,
            durationMs = 6000L,
            inPointMs = 1000L,
            outPointMs = 7000L,
            speed = 1.0f,
            volume = 0.9f,
            isVisible = true,
            groupId = "group_1"
        )

        val restoredTrack = Track(
            id = "track_audio_persisted",
            projectId = "proj_reload",
            type = TrackType.AUDIO,
            order = 1,
            isVisible = true,
            clips = listOf(restoredClip)
        )

        val restoredProject = Project(
            id = "proj_reload",
            name = "Reloaded Project",
            durationMs = 12000L,
            tracks = listOf(restoredTrack)
        )

        val mappedTracks = AudioTimelineMapper.mapProjectAudioTracks(restoredProject, assets, 12000L)
        assertEquals(1, mappedTracks.size)

        val segments = mappedTracks[0]
        assertEquals(3, segments.size)

        // Gap [0-4s]
        assertEquals(AudioTimelineSegment.GapSegment(0L, 4000L), segments[0])

        // Clip [4-10s]
        val seg = segments[1] as AudioTimelineSegment.ClipSegment
        assertEquals(4000L, seg.startTimeMs)
        assertEquals(6000L, seg.durationMs)
        assertEquals(1000L, seg.sourceInPointMs)
        assertEquals(7000L, seg.sourceOutPointMs)
        assertEquals(0.9f, seg.volume, 0.001f)

        // Gap [10-12s]
        assertEquals(AudioTimelineSegment.GapSegment(10000L, 2000L), segments[2])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE H: Audio timeline longer than video
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseH_AudioTimelineLongerThanVideo() {
        val assetAudio = createAsset("asset_audio", 20000L)
        val assets = mapOf(assetAudio.id to assetAudio)

        // Video is 5s, but audio clip extends to 12s
        val videoDurationMs = 5000L
        val audioClip = createAudioClip("clip_long_audio", 0L, 12000L, assetAudio.id)

        val effectiveDuration = maxOf(videoDurationMs, audioClip.endTimeMs) // 12000ms
        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(audioClip), assets, effectiveDuration)

        assertEquals(1, segments.size)
        val clipSeg = segments[0] as AudioTimelineSegment.ClipSegment
        assertEquals(0L, clipSeg.startTimeMs)
        assertEquals(12000L, clipSeg.durationMs)
        assertEquals(12000L, clipSeg.endTimeMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE I: Audio timeline shorter than video
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testCaseI_AudioTimelineShorterThanVideo() {
        val assetAudio = createAsset("asset_audio", 10000L)
        val assets = mapOf(assetAudio.id to assetAudio)

        // Video is 15s, but audio clip is only 5s [0s -> 5s]
        val videoDurationMs = 15000L
        val audioClip = createAudioClip("clip_short_audio", 0L, 5000L, assetAudio.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(audioClip), assets, videoDurationMs)

        assertEquals(2, segments.size)
        // Audio plays 0s -> 5s
        assertTrue(segments[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(5000L, segments[0].durationMs)

        // Trailing silence 5s -> 15s (10 seconds of silence)
        assertTrue(segments[1] is AudioTimelineSegment.GapSegment)
        assertEquals(5000L, segments[1].startTimeMs)
        assertEquals(10000L, segments[1].durationMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Muted Clips and Tracks Verification
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testMutedClipsTurnIntoGaps() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        val clipA = createAudioClip("clip_a", 0L, 3000L, assetA.id)
        val clipB = createAudioClip("clip_b", 3000L, 3000L, assetB.id, isVisible = false) // Muted
        val clipC = createAudioClip("clip_c", 6000L, 3000L, assetA.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB, clipC), assets, 9000L)

        assertEquals(3, segments.size)
        assertTrue(segments[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(3000L, segments[0].durationMs)

        // Middle gap replaces muted Clip B
        assertTrue(segments[1] is AudioTimelineSegment.GapSegment)
        assertEquals(3000L, segments[1].startTimeMs)
        assertEquals(3000L, segments[1].durationMs)

        assertTrue(segments[2] is AudioTimelineSegment.ClipSegment)
        assertEquals(6000L, segments[2].startTimeMs)
        assertEquals(3000L, segments[2].durationMs)
    }
}
