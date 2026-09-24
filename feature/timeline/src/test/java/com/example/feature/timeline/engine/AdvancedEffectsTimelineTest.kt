package com.example.feature.timeline.engine

import com.example.core.model.BlendMode
import com.example.core.model.Clip
import com.example.core.model.ClipMask
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.MaskShape
import com.example.core.model.Track
import com.example.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdvancedEffectsTimelineTest {

    private fun createInitialState(): Pair<TimelineEngineState, Clip> {
        val clip = Clip(
            id = "clip_1",
            trackId = "track_v1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 5000L
        )
        val track = Track(
            id = "track_v1",
            type = TrackType.VIDEO,
            clips = listOf(clip)
        )
        val state = TimelineEngineState(
            tracks = listOf(track),
            playheadPositionMs = 0L,
            durationMs = 5000L,
            selectedClipId = "clip_1"
        )
        return state to clip
    }

    @Test
    fun testAddUpdateRemoveReorderResetClipEffects() {
        val (initialState, clip) = createInitialState()

        val eff1 = Effect("eff_1", clip.id, EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 15f))
        val eff2 = Effect("eff_2", clip.id, EffectType.CONTRAST, parameters = mapOf("contrast" to 1.3f))

        // 1. Add eff1
        val s1 = TimelineReducer.reduce(initialState, TimelineAction.AddClipEffect(clip.id, eff1))
        val c1 = s1.tracks[0].clips[0]
        assertEquals(1, c1.effects.size)
        assertEquals("eff_1", c1.effects[0].id)

        // 2. Add eff2
        val s2 = TimelineReducer.reduce(s1, TimelineAction.AddClipEffect(clip.id, eff2))
        val c2 = s2.tracks[0].clips[0]
        assertEquals(2, c2.effects.size)

        // 3. Update eff1
        val updatedEff1 = eff1.copy(parameters = mapOf("brightness" to 30f))
        val s3 = TimelineReducer.reduce(s2, TimelineAction.UpdateClipEffect(clip.id, updatedEff1))
        val c3 = s3.tracks[0].clips[0]
        assertEquals(30f, c3.effects.find { it.id == "eff_1" }?.parameters?.get("brightness"))

        // 4. Reorder
        val s4 = TimelineReducer.reduce(s3, TimelineAction.ReorderClipEffects(clip.id, 1, 0))
        val c4 = s4.tracks[0].clips[0]
        assertEquals("eff_2", c4.effects[0].id)
        assertEquals("eff_1", c4.effects[1].id)

        // 5. Remove eff1
        val s5 = TimelineReducer.reduce(s4, TimelineAction.RemoveClipEffect(clip.id, "eff_1"))
        val c5 = s5.tracks[0].clips[0]
        assertEquals(1, c5.effects.size)
        assertEquals("eff_2", c5.effects[0].id)

        // 6. Reset
        val s6 = TimelineReducer.reduce(s5, TimelineAction.ResetClipEffects(clip.id))
        val c6 = s6.tracks[0].clips[0]
        assertEquals(0, c6.effects.size)
    }

    @Test
    fun testSetClipMask() {
        val (initialState, clip) = createInitialState()
        assertNull(initialState.tracks[0].clips[0].mask)

        val mask = ClipMask(
            shape = MaskShape.CIRCLE,
            x = 0.5f,
            y = 0.5f,
            width = 0.4f,
            height = 0.4f,
            feather = 0.1f,
            isInverted = true
        )

        val s1 = TimelineReducer.reduce(initialState, TimelineAction.SetClipMask(clip.id, mask))
        val c1 = s1.tracks[0].clips[0]
        assertNotNull(c1.mask)
        assertEquals(MaskShape.CIRCLE, c1.mask?.shape)
        assertEquals(0.4f, c1.mask?.width ?: 0f, 0.01f)
        assertEquals(true, c1.mask?.isInverted)

        // Remove mask
        val s2 = TimelineReducer.reduce(s1, TimelineAction.SetClipMask(clip.id, null))
        assertNull(s2.tracks[0].clips[0].mask)
    }

    @Test
    fun testSetClipBlendMode() {
        val (initialState, clip) = createInitialState()
        assertEquals(BlendMode.NORMAL, initialState.tracks[0].clips[0].blendMode)

        val s1 = TimelineReducer.reduce(initialState, TimelineAction.SetClipBlendMode(clip.id, BlendMode.SCREEN))
        assertEquals(BlendMode.SCREEN, s1.tracks[0].clips[0].blendMode)

        val s2 = TimelineReducer.reduce(s1, TimelineAction.SetClipBlendMode(clip.id, BlendMode.MULTIPLY))
        assertEquals(BlendMode.MULTIPLY, s2.tracks[0].clips[0].blendMode)
    }

    @Test
    fun testSetClipOpacity() {
        val (initialState, clip) = createInitialState()
        assertEquals(1.0f, initialState.tracks[0].clips[0].transform.opacity, 0.01f)

        val s1 = TimelineReducer.reduce(initialState, TimelineAction.SetClipOpacity(clip.id, 0.65f))
        assertEquals(0.65f, s1.tracks[0].clips[0].transform.opacity, 0.01f)
    }
}
