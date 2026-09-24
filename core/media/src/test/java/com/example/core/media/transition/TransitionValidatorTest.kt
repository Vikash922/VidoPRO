package com.example.core.media.transition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionValidatorTest {

    @Test
    fun `maxTransitionDuration respects shorter clip`() {
        val clipADuration = 3000L
        val clipBDuration = 1000L
        val maxDuration = TransitionValidator.maxTransitionDuration(clipADuration, clipBDuration)
        assertEquals(1000L, maxDuration)
    }

    @Test
    fun `validateAndClamp clamps duration to min and max`() {
        // Shorter than min 100ms
        assertEquals(100L, TransitionValidator.validateAndClamp(50L, 2000L, 2000L))

        // Normal valid duration
        assertEquals(500L, TransitionValidator.validateAndClamp(500L, 2000L, 2000L))

        // Exceeds min clip duration of 800ms
        assertEquals(800L, TransitionValidator.validateAndClamp(1500L, 800L, 2000L))

        // Clip shorter than min transition
        assertEquals(0L, TransitionValidator.validateAndClamp(500L, 50L, 2000L))
    }

    @Test
    fun `isValidTransitionDuration verifies valid boundaries`() {
        assertTrue(TransitionValidator.isValidTransitionDuration(500L, 1000L, 1000L))
        assertTrue(TransitionValidator.isValidTransitionDuration(100L, 500L, 500L))
        assertFalse(TransitionValidator.isValidTransitionDuration(50L, 1000L, 1000L))
        assertFalse(TransitionValidator.isValidTransitionDuration(1500L, 1000L, 1000L))
    }
}
