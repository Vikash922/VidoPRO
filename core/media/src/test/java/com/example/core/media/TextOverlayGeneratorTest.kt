package com.example.core.media

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.TextClipData
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.concurrent.Future

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextOverlayGeneratorTest {

    private fun createTextClip(
        id: String,
        startTimeMs: Long,
        durationMs: Long,
        text: String,
        alignment: String = "CENTER",
        fontSize: Float = 24f,
        textColor: String = "#FFFFFF",
        transform: Transform = Transform.DEFAULT
    ): Clip {
        return Clip(
            id = id,
            trackId = "track_text",
            type = ClipType.TEXT,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = 0L,
            outPointMs = durationMs,
            transform = transform,
            textData = TextClipData(
                clipId = id,
                text = text,
                alignment = alignment,
                fontSize = fontSize,
                textColor = textColor
            )
        )
    }

    @Test
    fun testEmptyTextClipsReturns1x1EmptyBitmapWithoutAllocatingFullBuffer() {
        val generator = TextOverlayGenerator(
            textClips = emptyList(),
            videoWidth = 1080,
            videoHeight = 1920
        )

        val bitmap = generator.getBitmap(0L)
        assertNotNull(bitmap)
        assertEquals(1, bitmap.width)
        assertEquals(1, bitmap.height)
        assertEquals(0, generator.allocationCount)
        assertEquals(0, generator.drawCount)

        generator.release()
    }

    @Test
    fun testSingleTextClipReusesBufferAcrossConsecutiveFramesWithoutReallocation() {
        val clip = createTextClip("clip_1", 0L, 5000L, "Hello VidoPRO")
        val generator = TextOverlayGenerator(listOf(clip), 1080, 1920)

        var initialBitmap = generator.getBitmap(0L)
        assertEquals(1080, initialBitmap.width)
        assertEquals(1920, initialBitmap.height)
        assertEquals(1, generator.allocationCount)
        assertEquals(1, generator.drawCount)

        // Simulate 90 frames (3 seconds at 30 fps)
        for (i in 1..90) {
            val presentationTimeUs = (i * 33333L) // ~30 fps
            val frameBitmap = generator.getBitmap(presentationTimeUs)
            assertSame("Expected identical bitmap instance reused across frames", initialBitmap, frameBitmap)
        }

        // Must still be exactly 1 allocation and 1 draw call!
        assertEquals(1, generator.allocationCount)
        assertEquals(1, generator.drawCount)

        generator.release()
        assertTrue(initialBitmap.isRecycled)
    }

    @Test
    fun testTextTransitionRedrawsWithoutReallocating() {
        val clip1 = createTextClip("clip_1", 0L, 2000L, "Scene 1")
        val clip2 = createTextClip("clip_2", 2000L, 2000L, "Scene 2")
        val generator = TextOverlayGenerator(listOf(clip1, clip2), 1080, 1920)

        // Frame in Clip 1
        val bitmap1 = generator.getBitmap(1_000_000L) // 1000ms
        assertEquals(1, generator.allocationCount)
        assertEquals(1, generator.drawCount)

        // Another frame in Clip 1 -> no draw, no alloc
        val bitmap1Next = generator.getBitmap(1_500_000L)
        assertSame(bitmap1, bitmap1Next)
        assertEquals(1, generator.allocationCount)
        assertEquals(1, generator.drawCount)

        // Frame in Clip 2 -> redraws in place, NO new allocation
        val bitmap2 = generator.getBitmap(2_500_000L) // 2500ms
        assertSame("Buffer must be reused across clip transitions", bitmap1, bitmap2)
        assertEquals(1, generator.allocationCount)
        assertEquals(2, generator.drawCount)

        // Frame past all clips -> returns empty bitmap, no extra allocation
        val empty = generator.getBitmap(5_000_000L)
        assertEquals(1, empty.width)
        assertEquals(1, empty.height)
        assertEquals(1, generator.allocationCount)

        generator.release()
    }

    @Test
    fun testOverlappingTextClipsRenderSimultaneously() {
        val baseTitle = createTextClip("base", 0L, 3000L, "Main Title")
        val overlaySubtitle = createTextClip("sub", 1000L, 1000L, "Subtitle")
        val generator = TextOverlayGenerator(listOf(baseTitle, overlaySubtitle), 1080, 1920)

        // 500ms: only base
        generator.getBitmap(500_000L)
        assertEquals(1, generator.allocationCount)
        assertEquals(1, generator.drawCount)

        // 1500ms: both clips active -> redraws with both
        generator.getBitmap(1_500_000L)
        assertEquals(1, generator.allocationCount)
        assertEquals(2, generator.drawCount)

        // 2500ms: only base active again -> redraws
        generator.getBitmap(2_500_000L)
        assertEquals(1, generator.allocationCount)
        assertEquals(3, generator.drawCount)

        generator.release()
    }

    @Test
    fun testDifferentCanvasSizes() {
        val clip = createTextClip("clip_720", 0L, 2000L, "720p Text")
        val generator720p = TextOverlayGenerator(listOf(clip), 720, 1280)
        val bitmap720 = generator720p.getBitmap(500_000L)
        assertEquals(720, bitmap720.width)
        assertEquals(1280, bitmap720.height)
        generator720p.release()

        val generator1080p = TextOverlayGenerator(listOf(clip), 1080, 1920)
        val bitmap1080 = generator1080p.getBitmap(500_000L)
        assertEquals(1080, bitmap1080.width)
        assertEquals(1920, bitmap1080.height)
        generator1080p.release()
    }

    @Test
    fun testReleaseRecyclesBuffers() {
        val clip = createTextClip("clip_rel", 0L, 2000L, "Test Recycle")
        val generator = TextOverlayGenerator(listOf(clip), 1080, 1920)
        val bitmap = generator.getBitmap(500_000L)

        generator.release()
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun testThreadSafetyUnderConcurrentCalls() {
        val clip = createTextClip("clip_c", 0L, 10000L, "Concurrent Test")
        val generator = TextOverlayGenerator(listOf(clip), 1080, 1920)

        val executor = Executors.newFixedThreadPool(4)
        val futures = mutableListOf<Future<*>>()

        for (i in 0 until 50) {
            val future = executor.submit {
                val timeUs = (i * 100_000L)
                val bm = generator.getBitmap(timeUs)
                assertNotNull(bm)
                assertEquals(1080, bm.width)
                assertEquals(1920, bm.height)
            }
            futures.add(future)
        }

        for (future in futures) {
            future.get()
        }
        executor.shutdown()

        assertEquals(1, generator.allocationCount)
        generator.release()
    }
}
