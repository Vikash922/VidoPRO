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

class AudioTimelineMapperTest {

    private fun createAsset(id: String, durationMs: Long = 10000L): Asset {
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

    @Test
    fun testContiguousClipsProduceNoGaps() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

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

    @Test
    fun testLeadingGapProperlyMapped() {
        val asset = createAsset("asset_1")
        val assets = mapOf(asset.id to asset)

        // Clip starts at 3000ms, lasting 4000ms
        val clip = createAudioClip("clip_1", 3000L, 4000L, asset.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clip), assets, 7000L)

        assertEquals(2, segments.size)
        // Leading gap from 0 to 3000ms
        assertTrue(segments[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(3000L, segments[0].durationMs)

        // Clip from 3000 to 7000ms
        assertTrue(segments[1] is AudioTimelineSegment.ClipSegment)
        assertEquals(3000L, segments[1].startTimeMs)
        assertEquals(4000L, segments[1].durationMs)
    }

    @Test
    fun testMiddleGapProperlyMapped() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        // Audio A: 0-5s, Gap: 5-10s, Audio B: 10-15s (The exact user prompt issue)
        val clipA = createAudioClip("clip_a", 0L, 5000L, assetA.id)
        val clipB = createAudioClip("clip_b", 10000L, 5000L, assetB.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB), assets, 15000L)

        assertEquals(3, segments.size)
        // Segment 0: Audio A [0-5s]
        val segA = segments[0] as AudioTimelineSegment.ClipSegment
        assertEquals(0L, segA.startTimeMs)
        assertEquals(5000L, segA.durationMs)
        assertEquals("clip_a", segA.clip.id)

        // Segment 1: Middle Gap [5-10s]
        val segGap = segments[1] as AudioTimelineSegment.GapSegment
        assertEquals(5000L, segGap.startTimeMs)
        assertEquals(5000L, segGap.durationMs)

        // Segment 2: Audio B [10-15s]
        val segB = segments[2] as AudioTimelineSegment.ClipSegment
        assertEquals(10000L, segB.startTimeMs)
        assertEquals(5000L, segB.durationMs)
        assertEquals("clip_b", segB.clip.id)
    }

    @Test
    fun testTrailingGapProperlyMapped() {
        val asset = createAsset("asset_1")
        val assets = mapOf(asset.id to asset)

        // Clip: 0 to 4s, Total timeline: 10s
        val clip = createAudioClip("clip_1", 0L, 4000L, asset.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clip), assets, 10000L)

        assertEquals(2, segments.size)
        assertTrue(segments[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(4000L, segments[0].durationMs)

        // Trailing gap from 4s to 10s
        assertTrue(segments[1] is AudioTimelineSegment.GapSegment)
        assertEquals(4000L, segments[1].startTimeMs)
        assertEquals(6000L, segments[1].durationMs)
    }

    @Test
    fun testLeadingMiddleAndTrailingGapsTogether() {
        val asset = createAsset("asset_1")
        val assets = mapOf(asset.id to asset)

        // Leading gap: 0-2s, Clip A: 2-5s, Middle gap: 5-8s, Clip B: 8-10s, Trailing gap: 10-12s
        val clipA = createAudioClip("clip_a", 2000L, 3000L, asset.id)
        val clipB = createAudioClip("clip_b", 8000L, 2000L, asset.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB), assets, 12000L)

        assertEquals(5, segments.size)
        assertEquals(AudioTimelineSegment.GapSegment(0L, 2000L), segments[0])
        assertEquals(2000L, (segments[1] as AudioTimelineSegment.ClipSegment).startTimeMs)
        assertEquals(3000L, segments[1].durationMs)
        assertEquals(AudioTimelineSegment.GapSegment(5000L, 3000L), segments[2])
        assertEquals(8000L, (segments[3] as AudioTimelineSegment.ClipSegment).startTimeMs)
        assertEquals(2000L, segments[3].durationMs)
        assertEquals(AudioTimelineSegment.GapSegment(10000L, 2000L), segments[4])

        // Verify total duration equals sum of all segments
        val totalSum = segments.sumOf { it.durationMs }
        assertEquals(12000L, totalSum)
    }

    @Test
    fun testClipTrimAndSpeedPreserved() {
        val asset = createAsset("asset_1", 20000L)
        val assets = mapOf(asset.id to asset)

        val clip = createAudioClip(
            id = "clip_trimmed",
            startTimeMs = 1000L,
            durationMs = 4000L,
            assetId = asset.id,
            inPointMs = 2000L,
            outPointMs = 8000L,
            speed = 1.5f,
            volume = 0.8f
        )

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clip), assets, 6000L)

        assertEquals(3, segments.size)
        val seg = segments[1] as AudioTimelineSegment.ClipSegment
        assertEquals(2000L, seg.sourceInPointMs)
        assertEquals(8000L, seg.sourceOutPointMs)
        assertEquals(1.5f, seg.speed, 0.001f)
        assertEquals(0.8f, seg.volume, 0.001f)
    }

    @Test
    fun testMutedClipsProduceGaps() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        // Clip B is invisible / muted (or volume 0)
        val clipA = createAudioClip("clip_a", 0L, 3000L, assetA.id)
        val clipB = createAudioClip("clip_b", 3000L, 3000L, assetB.id, isVisible = false)
        val clipC = createAudioClip("clip_c", 6000L, 3000L, assetA.id)

        val segments = AudioTimelineMapper.mapClipsToSegments(listOf(clipA, clipB, clipC), assets, 9000L)

        // Clip B should be omitted and treated as a middle gap [3-6s]
        assertEquals(3, segments.size)
        assertTrue(segments[0] is AudioTimelineSegment.ClipSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(3000L, segments[0].durationMs)

        assertTrue(segments[1] is AudioTimelineSegment.GapSegment)
        assertEquals(3000L, segments[1].startTimeMs)
        assertEquals(3000L, segments[1].durationMs)

        assertTrue(segments[2] is AudioTimelineSegment.ClipSegment)
        assertEquals(6000L, segments[2].startTimeMs)
        assertEquals(3000L, segments[2].durationMs)
    }

    @Test
    fun testOverlappingClipsPartitioning() {
        val asset = createAsset("asset_1")

        // Clip 1: [0, 5000]
        // Clip 2: [3000, 8000] (overlaps with Clip 1)
        // Clip 3: [6000, 10000] (does not overlap with Clip 1 after 5000)
        val clip1 = createAudioClip("c1", 0L, 5000L, asset.id)
        val clip2 = createAudioClip("c2", 3000L, 5000L, asset.id)
        val clip3 = createAudioClip("c3", 6000L, 4000L, asset.id)

        val layers = AudioTimelineMapper.partitionIntoNonOverlappingLayers(listOf(clip1, clip2, clip3))

        assertEquals(2, layers.size)
        // Layer 0 should contain clip 1 and clip 3
        assertEquals(listOf("c1", "c3"), layers[0].map { it.id })
        // Layer 1 should contain clip 2
        assertEquals(listOf("c2"), layers[1].map { it.id })
    }

    @Test
    fun testMutedTrackReturnsEmptySegments() {
        val asset = createAsset("asset_1")
        val track = Track(
            id = "track_audio_muted",
            projectId = "proj_1",
            type = TrackType.AUDIO,
            order = 1,
            isVisible = false, // Muted track!
            clips = listOf(createAudioClip("c1", 0L, 5000L, asset.id))
        )

        val segments = AudioTimelineMapper.mapTrackToSegments(track, mapOf(asset.id to asset), 5000L)
        assertTrue(segments.isEmpty())
    }

    @Test
    fun testProjectAudioTracksMapping() {
        val assetA = createAsset("asset_a")
        val assetB = createAsset("asset_b")
        val assets = mapOf(assetA.id to assetA, assetB.id to assetB)

        val track1 = Track(
            id = "track_voice",
            projectId = "proj_1",
            type = TrackType.AUDIO,
            order = 1,
            isVisible = true,
            clips = listOf(createAudioClip("c_v1", 2000L, 4000L, assetA.id)) // 2-6s
        )

        val track2 = Track(
            id = "track_music",
            projectId = "proj_1",
            type = TrackType.AUDIO,
            order = 2,
            isVisible = true,
            clips = listOf(createAudioClip("c_m1", 0L, 10000L, assetB.id)) // 0-10s
        )

        val project = Project(
            id = "proj_1",
            name = "Audio Test Project",
            durationMs = 10000L,
            tracks = listOf(track1, track2)
        )

        val mapped = AudioTimelineMapper.mapProjectAudioTracks(project, assets, 10000L)
        assertEquals(2, mapped.size)

        // Track 1: [Gap 0-2s, Clip 2-6s, Gap 6-10s]
        assertEquals(3, mapped[0].size)
        assertTrue(mapped[0][0] is AudioTimelineSegment.GapSegment)
        assertEquals(2000L, mapped[0][0].durationMs)
        assertTrue(mapped[0][1] is AudioTimelineSegment.ClipSegment)
        assertEquals(4000L, mapped[0][1].durationMs)
        assertTrue(mapped[0][2] is AudioTimelineSegment.GapSegment)
        assertEquals(4000L, mapped[0][2].durationMs)

        // Track 2: [Clip 0-10s]
        assertEquals(1, mapped[1].size)
        assertTrue(mapped[1][0] is AudioTimelineSegment.ClipSegment)
        assertEquals(10000L, mapped[1][0].durationMs)
    }
}
