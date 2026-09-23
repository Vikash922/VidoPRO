package com.example.feature.timeline.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineArchitectureTest {

    @Test
    fun testTimelineSelectionState() {
        val selection = TimelineSelectionState(
            selectedClipId = "clip_1",
            multiSelectedClipIds = setOf("clip_1", "clip_2"),
            activeGroupId = "group_abc"
        )

        assertTrue(selection.isClipSelected("clip_1"))
        assertFalse(selection.isClipSelected("clip_2"))

        assertTrue(selection.isClipMultiSelected("clip_1"))
        assertTrue(selection.isClipMultiSelected("clip_2"))
        assertFalse(selection.isClipMultiSelected("clip_3"))

        assertTrue(selection.isClipInSelection("clip_1"))
        assertTrue(selection.isClipInSelection("clip_2"))
        assertFalse(selection.isClipInSelection("clip_3"))

        assertTrue(selection.isMultiSelectActive)
        assertEquals("group_abc", selection.activeGroupId)
    }

    @Test
    fun testTimelineGestureHandlerConversions() {
        val pixelsPerMs = 0.05f

        val ms = TimelineGestureHandler.pixelsToMs(100f, pixelsPerMs)
        assertEquals(2000L, ms)

        val pixels = TimelineGestureHandler.msToPixels(2000L, pixelsPerMs)
        assertEquals(100f, pixels, 0.001f)

        // Zero pixelsPerMs edge case
        val zeroMs = TimelineGestureHandler.pixelsToMs(100f, 0f)
        assertEquals(0L, zeroMs)
    }
}
