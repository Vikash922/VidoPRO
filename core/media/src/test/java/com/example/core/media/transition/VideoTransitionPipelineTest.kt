package com.example.core.media.transition

import com.example.core.media.render.RenderTransform
import com.example.core.media.render.RenderTransition
import com.example.core.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTransitionPipelineTest {

    private fun createTransition(
        type: TransitionType,
        cutTimeMs: Long = 3000L,
        durationMs: Long = 1000L,
        direction: String = "LEFT"
    ): RenderTransition {
        val start = cutTimeMs - (durationMs / 2)
        val end = cutTimeMs + (durationMs / 2)
        return RenderTransition(
            id = "t1",
            type = type,
            durationMs = durationMs,
            cutTimeMs = cutTimeMs,
            startTimeMs = start,
            endTimeMs = end,
            firstClipId = "clip1",
            secondClipId = "clip2",
            properties = mapOf("direction" to direction)
        )
    }

    @Test
    fun `RenderTransition centered window and deterministic progress`() {
        val transition = createTransition(TransitionType.FADE, cutTimeMs = 3000L, durationMs = 1000L)

        // Center window: 2500ms to 3500ms
        assertEquals(2500L, transition.startTimeMs)
        assertEquals(3500L, transition.endTimeMs)

        assertFalse(transition.isActiveAt(2499L))
        assertTrue(transition.isActiveAt(2500L))
        assertTrue(transition.isActiveAt(3000L))
        assertTrue(transition.isActiveAt(3500L))
        assertFalse(transition.isActiveAt(3501L))

        assertEquals(0.0f, transition.progressAt(2500L), 0.001f)
        assertEquals(0.5f, transition.progressAt(3000L), 0.001f)
        assertEquals(1.0f, transition.progressAt(3500L), 0.001f)
    }

    @Test
    fun `Fade transition modulates opacity correctly`() {
        val transition = createTransition(TransitionType.FADE)
        val base = RenderTransform(x = 0f, y = 0f, scaleX = 1f, scaleY = 1f, rotation = 0f, opacity = 1.0f)

        // At progress 0.0
        val out0 = TransitionEvaluator.evaluateOutgoingTransform(transition, 0f, base, 1080f, 1920f)
        val in0 = TransitionEvaluator.evaluateIncomingTransform(transition, 0f, base, 1080f, 1920f)
        assertEquals(1.0f, out0.opacity, 0.001f)
        assertEquals(0.0f, in0.opacity, 0.001f)

        // At progress 0.5 (cut point)
        val outHalf = TransitionEvaluator.evaluateOutgoingTransform(transition, 0.5f, base, 1080f, 1920f)
        val inHalf = TransitionEvaluator.evaluateIncomingTransform(transition, 0.5f, base, 1080f, 1920f)
        assertEquals(0.5f, outHalf.opacity, 0.001f)
        assertEquals(0.5f, inHalf.opacity, 0.001f)

        // At progress 1.0
        val out1 = TransitionEvaluator.evaluateOutgoingTransform(transition, 1.0f, base, 1080f, 1920f)
        val in1 = TransitionEvaluator.evaluateIncomingTransform(transition, 1.0f, base, 1080f, 1920f)
        assertEquals(0.0f, out1.opacity, 0.001f)
        assertEquals(1.0f, in1.opacity, 0.001f)
    }

    @Test
    fun `Slide transition transforms translation in all directions`() {
        val base = RenderTransform.IDENTITY
        val width = 1080f
        val height = 1920f

        // Slide LEFT
        val slideLeft = createTransition(TransitionType.SLIDE, direction = "LEFT")
        val outLeft = TransitionEvaluator.evaluateOutgoingTransform(slideLeft, 0.5f, base, width, height)
        val inLeft = TransitionEvaluator.evaluateIncomingTransform(slideLeft, 0.5f, base, width, height)
        assertEquals(-width * 0.5f, outLeft.x, 0.001f)
        assertEquals(width * 0.5f, inLeft.x, 0.001f)

        // Slide RIGHT
        val slideRight = createTransition(TransitionType.SLIDE, direction = "RIGHT")
        val outRight = TransitionEvaluator.evaluateOutgoingTransform(slideRight, 0.5f, base, width, height)
        val inRight = TransitionEvaluator.evaluateIncomingTransform(slideRight, 0.5f, base, width, height)
        assertEquals(width * 0.5f, outRight.x, 0.001f)
        assertEquals(-width * 0.5f, inRight.x, 0.001f)

        // Slide UP
        val slideUp = createTransition(TransitionType.SLIDE, direction = "UP")
        val outUp = TransitionEvaluator.evaluateOutgoingTransform(slideUp, 0.5f, base, width, height)
        val inUp = TransitionEvaluator.evaluateIncomingTransform(slideUp, 0.5f, base, width, height)
        assertEquals(-height * 0.5f, outUp.y, 0.001f)
        assertEquals(height * 0.5f, inUp.y, 0.001f)

        // Slide DOWN
        val slideDown = createTransition(TransitionType.SLIDE, direction = "DOWN")
        val outDown = TransitionEvaluator.evaluateOutgoingTransform(slideDown, 0.5f, base, width, height)
        val inDown = TransitionEvaluator.evaluateIncomingTransform(slideDown, 0.5f, base, width, height)
        assertEquals(height * 0.5f, outDown.y, 0.001f)
        assertEquals(-height * 0.5f, inDown.y, 0.001f)
    }

    @Test
    fun `Zoom transition scales outgoing up and incoming from zoom-in`() {
        val transition = createTransition(TransitionType.ZOOM)
        val base = RenderTransform(x = 10f, y = 20f, scaleX = 1f, scaleY = 1f, rotation = 0f, opacity = 1f)

        // At progress 0.0: outgoing scale is 1.0, incoming scale is 0.5
        val out0 = TransitionEvaluator.evaluateOutgoingTransform(transition, 0f, base, 1080f, 1920f)
        val in0 = TransitionEvaluator.evaluateIncomingTransform(transition, 0f, base, 1080f, 1920f)
        assertEquals(1.0f, out0.scaleX, 0.001f)
        assertEquals(0.5f, in0.scaleX, 0.001f)

        // At progress 1.0: outgoing scale is 1.5, incoming scale is 1.0
        val out1 = TransitionEvaluator.evaluateOutgoingTransform(transition, 1.0f, base, 1080f, 1920f)
        val in1 = TransitionEvaluator.evaluateIncomingTransform(transition, 1.0f, base, 1080f, 1920f)
        assertEquals(1.5f, out1.scaleX, 0.001f)
        assertEquals(1.0f, in1.scaleX, 0.001f)
    }

    @Test
    fun `Wipe transition translates incoming smoothly across canvas`() {
        val transition = createTransition(TransitionType.WIPE)
        val base = RenderTransform.IDENTITY
        val width = 1080f

        val inStart = TransitionEvaluator.evaluateIncomingTransform(transition, 0.0f, base, width, 1920f)
        val inHalf = TransitionEvaluator.evaluateIncomingTransform(transition, 0.5f, base, width, 1920f)
        val inEnd = TransitionEvaluator.evaluateIncomingTransform(transition, 1.0f, base, width, 1920f)

        assertEquals(width, inStart.x, 0.001f)
        assertEquals(width * 0.5f, inHalf.x, 0.001f)
        assertEquals(0f, inEnd.x, 0.001f)
    }

    @Test
    fun `Base transform is preserved and layered under transition transform`() {
        val transition = createTransition(TransitionType.SLIDE, direction = "LEFT")
        val base = RenderTransform(x = 100f, y = 50f, scaleX = 2.0f, scaleY = 2.0f, rotation = 45f, opacity = 0.8f)

        val out = TransitionEvaluator.evaluateOutgoingTransform(transition, 0.5f, base, 1000f, 1000f)

        // x had 100f base, slide left adds -0.5 * 1000f = -500f -> total -400f
        assertEquals(-400f, out.x, 0.001f)
        assertEquals(50f, out.y, 0.001f)
        assertEquals(2.0f, out.scaleX, 0.001f)
        assertEquals(2.0f, out.scaleY, 0.001f)
        assertEquals(45f, out.rotation, 0.001f)
        assertEquals(0.8f, out.opacity, 0.001f)
    }
}
