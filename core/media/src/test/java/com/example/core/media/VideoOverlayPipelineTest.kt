package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Asset
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.MediaType
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Deterministic test suite for Video Overlay / PiP preview and export pipeline (Cases A - K).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoOverlayPipelineTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun createTestAsset(
        id: String,
        uri: String = "content://media/external/video/media/$id",
        mediaType: MediaType = MediaType.VIDEO,
        width: Int = 1920,
        height: Int = 1080,
        durationMs: Long = 10_000L
    ): Asset {
        return Asset(
            id = id,
            uri = uri,
            mediaType = mediaType,
            width = width,
            height = height,
            durationMs = durationMs
        )
    }

    private fun createOverlayClip(
        id: String,
        assetId: String,
        startTimeMs: Long,
        durationMs: Long,
        inPointMs: Long = 0L,
        outPointMs: Long = durationMs,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
        transform: Transform = Transform.DEFAULT,
        isVisible: Boolean = true
    ): Clip {
        return Clip(
            id = id,
            trackId = "track_overlay_1",
            type = ClipType.VIDEO,
            assetId = assetId,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = inPointMs,
            outPointMs = outPointMs,
            speed = speed,
            volume = volume,
            transform = transform,
            isVisible = isVisible
        )
    }

    // A test frame extractor that records timestamps requested and returns a 100x100 test frame
    private class TestFrameExtractor : VideoOverlayGenerator.FrameExtractor {
        val requestedTimesUs = mutableListOf<Long>()
        val dummyBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        var isClosed = false

        override fun getFrameAtTime(timeUs: Long): Bitmap? {
            requestedTimesUs.add(timeUs)
            return dummyBitmap
        }

        override fun close() {
            isClosed = true
        }
    }

    // ── Case A: Single video overlay playback on timeline ───────────────────
    @Test
    fun testCaseA_SingleOverlayTimelinePlayback() {
        val asset = createTestAsset("asset_1")
        val clip = createOverlayClip(
            id = "clip_1",
            assetId = asset.id,
            startTimeMs = 2000L,
            durationMs = 4000L // 2000ms to 6000ms
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        // Before clip (at 1000ms): should return 1x1 empty bitmap, no frame extracted
        val beforeFrame = generator.getBitmap(1000_000L)
        assertEquals(1, beforeFrame.width)
        assertEquals(1, beforeFrame.height)
        assertTrue(extractor.requestedTimesUs.isEmpty())

        // During clip (at 3000ms): should extract frame and draw onto canvas
        val activeFrame = generator.getBitmap(3000_000L)
        assertEquals(1080, activeFrame.width)
        assertEquals(1920, activeFrame.height)
        assertEquals(1, extractor.requestedTimesUs.size)
        // Source offset: (3000 - 2000) + 0 = 1000ms -> 1,000,000us
        assertEquals(1000_000L, extractor.requestedTimesUs[0])

        // After clip (at 7000ms): should return empty bitmap
        val afterFrame = generator.getBitmap(7000_000L)
        assertEquals(1, afterFrame.width)
        assertEquals(1, afterFrame.height)

        generator.close()
    }

    // ── Case B: Overlay with timeline offset ─────────────────────────────────
    @Test
    fun testCaseB_OverlayWithTimelineOffset() {
        val asset = createTestAsset("asset_b")
        val clip = createOverlayClip(
            id = "clip_b",
            assetId = asset.id,
            startTimeMs = 5000L,
            durationMs = 3000L // 5000ms to 8000ms
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        // At 6500ms (1.5s into clip): source time should be 1500ms (1,500,000us)
        generator.getBitmap(6500_000L)
        assertEquals(1, extractor.requestedTimesUs.size)
        assertEquals(1500_000L, extractor.requestedTimesUs[0])

        generator.close()
    }

    // ── Case C: Overlay with source trimming ─────────────────────────────────
    @Test
    fun testCaseC_OverlayWithSourceTrimming() {
        val asset = createTestAsset("asset_c")
        // Trimmed source: starts at 2000ms in source file, outPoint at 5000ms (3000ms duration)
        val clip = createOverlayClip(
            id = "clip_c",
            assetId = asset.id,
            startTimeMs = 1000L,
            durationMs = 3000L,
            inPointMs = 2000L,
            outPointMs = 5000L
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        // At timeline 2500ms (1500ms into timeline clip):
        // Source offset = (2500 - 1000) + inPoint(2000) = 3500ms
        generator.getBitmap(2500_000L)
        assertEquals(1, extractor.requestedTimesUs.size)
        assertEquals(3500_000L, extractor.requestedTimesUs[0])

        generator.close()
    }

    // ── Case D: Moving overlay timeline scrub / seek ─────────────────────────
    @Test
    fun testCaseD_ScrubbingAndSeekingExtractsAccurateFrames() {
        val asset = createTestAsset("asset_d")
        val clip = createOverlayClip(
            id = "clip_d",
            assetId = asset.id,
            startTimeMs = 0L,
            durationMs = 10_000L
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        val scrubTimes = listOf(500L, 2000L, 1000L, 8500L, 4000L)
        for (timeMs in scrubTimes) {
            generator.getBitmap(timeMs * 1000L)
        }

        assertEquals(scrubTimes.size, extractor.requestedTimesUs.size)
        for (i in scrubTimes.indices) {
            assertEquals(scrubTimes[i] * 1000L, extractor.requestedTimesUs[i])
        }

        generator.close()
    }

    // ── Case E: Overlay position X/Y and transform ──────────────────────────
    @Test
    fun testCaseE_OverlayPositionAndTransform() {
        val asset = createTestAsset("asset_e")
        val customTransform = Transform(
            x = 150f,
            y = -200f,
            scaleX = 1.2f,
            scaleY = 1.2f,
            rotation = 45f,
            opacity = 0.8f
        )
        val clip = createOverlayClip(
            id = "clip_e",
            assetId = asset.id,
            startTimeMs = 0L,
            durationMs = 5000L,
            transform = customTransform
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        val frame = generator.getBitmap(1000_000L)
        assertNotNull(frame)
        assertEquals(1080, frame.width)
        assertEquals(1920, frame.height)

        generator.close()
    }

    // ── Case F: Multiple overlay clips at different timeline ranges ──────────
    @Test
    fun testCaseF_MultipleOverlaysSequentialAndOverlapping() {
        val asset1 = createTestAsset("asset_1")
        val asset2 = createTestAsset("asset_2")

        val clip1 = createOverlayClip("c1", asset1.id, startTimeMs = 1000L, durationMs = 3000L) // 1s - 4s
        val clip2 = createOverlayClip("c2", asset2.id, startTimeMs = 3000L, durationMs = 4000L) // 3s - 7s

        val extractorMap = mutableMapOf<String, TestFrameExtractor>()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip1, clip2),
            assets = mapOf(asset1.id to asset1, asset2.id to asset2),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { uri ->
                val id = if (uri.toString().contains("asset_1")) "asset_1" else "asset_2"
                extractorMap.getOrPut(id) { TestFrameExtractor() }
            }
        )

        // At 2s: only clip1 active
        generator.getBitmap(2000_000L)
        assertEquals(1, extractorMap["asset_1"]?.requestedTimesUs?.size ?: 0)
        assertEquals(0, extractorMap["asset_2"]?.requestedTimesUs?.size ?: 0)

        // At 3.5s: BOTH clip1 and clip2 active (overlapping PiPs)
        generator.getBitmap(3500_000L)
        assertEquals(2, extractorMap["asset_1"]?.requestedTimesUs?.size ?: 0)
        assertEquals(1, extractorMap["asset_2"]?.requestedTimesUs?.size ?: 0)

        // At 5s: only clip2 active
        generator.getBitmap(5000_000L)
        assertEquals(2, extractorMap["asset_1"]?.requestedTimesUs?.size ?: 0)
        assertEquals(2, extractorMap["asset_2"]?.requestedTimesUs?.size ?: 0)

        generator.close()
    }

    // ── Case G: Overlay audio sequencing via AudioTimelineMapper ────────────
    @Test
    fun testCaseG_OverlayAudioSequencingInExport() {
        val assetVideo = createTestAsset("asset_pip", durationMs = 10_000L)
        val clipWithAudio = createOverlayClip(
            id = "pip_audible",
            assetId = assetVideo.id,
            startTimeMs = 3000L,
            durationMs = 4000L,
            volume = 0.8f
        )
        val clipMuted = createOverlayClip(
            id = "pip_muted",
            assetId = assetVideo.id,
            startTimeMs = 8000L,
            durationMs = 2000L,
            volume = 0.0f
        )

        val assets = mapOf(assetVideo.id to assetVideo)
        val overlayClips = listOf(clipWithAudio, clipMuted)

        // Audible overlay filter
        val audibleClips = overlayClips.filter {
            (it.volume ?: 1f) > 0f && (it.type == ClipType.VIDEO || assets[it.assetId]?.mediaType == MediaType.VIDEO)
        }
        assertEquals(1, audibleClips.size)
        assertEquals("pip_audible", audibleClips[0].id)

        // Map through AudioTimelineMapper
        val segments = AudioTimelineMapper.mapClipsToSegments(audibleClips, assets, totalDurationMs = 10_000L)
        assertEquals(3, segments.size)

        // 1. Leading silence: 0 -> 3000ms
        assertTrue(segments[0] is AudioTimelineSegment.GapSegment)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(3000L, segments[0].durationMs)

        // 2. Audio clip: 3000 -> 7000ms
        assertTrue(segments[1] is AudioTimelineSegment.ClipSegment)
        val audioSeg = segments[1] as AudioTimelineSegment.ClipSegment
        assertEquals(3000L, audioSeg.startTimeMs)
        assertEquals(4000L, audioSeg.durationMs)
        assertEquals(0.8f, audioSeg.volume, 0.001f)

        // 3. Trailing silence: 7000 -> 10000ms
        assertTrue(segments[2] is AudioTimelineSegment.GapSegment)
        assertEquals(7000L, segments[2].startTimeMs)
        assertEquals(3000L, segments[2].durationMs)
    }

    // ── Case H: Canvas Coordinate Helper coordinate conversions ─────────────
    @Test
    fun testCaseH_CanvasCoordinateHelperPreservesSizingAcrossAspectRatios() {
        val testDimensions = listOf(
            AspectRatio.RATIO_9_16 to (1080 to 1920),
            AspectRatio.RATIO_16_9 to (1920 to 1080),
            AspectRatio.RATIO_1_1 to (1080 to 1080),
            AspectRatio.RATIO_4_5 to (1080 to 1350)
        )

        for ((ratio, dims) in testDimensions) {
            val (w, h) = CanvasCoordinateHelper.getDimensionsForAspectRatio(ratio)
            assertEquals(dims.first, w)
            assertEquals(dims.second, h)

            val transform = Transform(x = 100f, y = -100f)
            val previewT = CanvasCoordinateHelper.toPreviewCoordinates(
                transform = transform,
                canvasWidthPx = 360f,
                canvasHeightPx = 360f * (h.toFloat() / w.toFloat()),
                projectWidth = w,
                projectHeight = h
            )
            // Normalized preview offset should scale proportionally
            assertEquals(100f * (360f / w), previewT.x, 0.01f)
        }
    }

    // ── Case I: Memory safety and zero per-frame buffer allocations ──────────
    @Test
    fun testCaseI_ZeroPerFrameAllocationAndResourceCleanup() {
        val asset = createTestAsset("asset_mem")
        val clip = createOverlayClip("clip_mem", asset.id, startTimeMs = 0L, durationMs = 5000L)

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 720,
            videoHeight = 1280,
            frameExtractorFactory = { extractor }
        )

        // Calling getBitmap repeatedly should return the exact same reusable canvasBitmap instance!
        val bitmap1 = generator.getBitmap(1000_000L)
        val bitmap2 = generator.getBitmap(2000_000L)
        val bitmap3 = generator.getBitmap(3000_000L)

        assertSame("Canvas bitmap MUST be reused to prevent GC pauses and OOM", bitmap1, bitmap2)
        assertSame("Canvas bitmap MUST be reused across all frames", bitmap2, bitmap3)

        // Close should release extractors and recycle canvas
        generator.close()
        assertTrue(extractor.isClosed)
        assertTrue(bitmap1.isRecycled)
    }

    // ── Case J: Multi-Ratio Export Canvas Rendering ──────────────────────────
    @Test
    fun testCaseJ_MultiRatioExportCanvasRendering() {
        val asset = createTestAsset("asset_multi")
        val clip = createOverlayClip("clip_multi", asset.id, startTimeMs = 0L, durationMs = 5000L)

        val ratios = listOf(
            1080 to 1920, // 9:16
            1920 to 1080, // 16:9
            1080 to 1080, // 1:1
            1080 to 1350  // 4:5
        )

        for ((w, h) in ratios) {
            val generator = VideoOverlayGenerator(
                context = context,
                overlayClips = listOf(clip),
                assets = mapOf(asset.id to asset),
                videoWidth = w,
                videoHeight = h,
                frameExtractorFactory = { TestFrameExtractor() }
            )

            val frame = generator.getBitmap(1000_000L)
            assertEquals(w, frame.width)
            assertEquals(h, frame.height)
            generator.close()
        }
    }

    // ── Case K: Inactive overlay visibility ──────────────────────────────────
    @Test
    fun testCaseK_InvisibleClipIsIgnored() {
        val asset = createTestAsset("asset_invis")
        val invisibleClip = createOverlayClip(
            id = "clip_invis",
            assetId = asset.id,
            startTimeMs = 0L,
            durationMs = 5000L,
            isVisible = false
        )

        val extractor = TestFrameExtractor()
        val generator = VideoOverlayGenerator(
            context = context,
            overlayClips = listOf(invisibleClip),
            assets = mapOf(asset.id to asset),
            videoWidth = 1080,
            videoHeight = 1920,
            frameExtractorFactory = { extractor }
        )

        val frame = generator.getBitmap(2000_000L)
        // Should return empty 1x1 bitmap because clip is invisible
        assertEquals(1, frame.width)
        assertEquals(1, frame.height)
        assertTrue(extractor.requestedTimesUs.isEmpty())

        generator.close()
    }
}
