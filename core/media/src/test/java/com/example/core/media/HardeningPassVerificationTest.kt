package com.example.core.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HardeningPassVerificationTest {

    @Test
    fun testKeyframeEvaluatorEmptyKeyframesFastPath() {
        val staticTransform = Transform(x = 100f, y = 200f, scaleX = 1.5f, scaleY = 1.5f, rotation = 45f, opacity = 0.8f)
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 5000L,
            transform = staticTransform,
            keyframes = emptyList()
        )

        // Evaluator should return the static transform without modifying values
        val evaluated = KeyframeEvaluator.evaluateTransform(clip, 2500L)
        assertEquals(staticTransform, evaluated)

        // Volume evaluator should return base volume
        val evaluatedVolume = KeyframeEvaluator.evaluateVolume(clip, 2500L)
        assertEquals(1.0f, evaluatedVolume, 0.001f)
    }

    @Test
    fun testKeyframeEvaluatorInterpolationAndRotation() {
        // Test interpolate directly
        val midLinear = KeyframeEvaluator.interpolate(10f, 20f, 0.5f, InterpolationType.LINEAR)
        assertEquals(15f, midLinear, 0.001f)

        val holdBefore = KeyframeEvaluator.interpolate(10f, 20f, 0.99f, InterpolationType.HOLD)
        assertEquals(10f, holdBefore, 0.001f)
        val holdAtOne = KeyframeEvaluator.interpolate(10f, 20f, 1.0f, InterpolationType.HOLD)
        assertEquals(20f, holdAtOne, 0.001f)

        // Test shortest-path rotation boundary crossing (350 deg -> 10 deg)
        val rotBoundary = KeyframeEvaluator.evaluateRotation(350f, 10f, 0.5f, InterpolationType.LINEAR)
        val normalized = (rotBoundary % 360f + 360f) % 360f
        assertEquals(0f, normalized, 0.001f)

        // Test continuous multi-turn spin (0 deg -> 720 deg)
        val rotSpin = KeyframeEvaluator.evaluateRotation(0f, 720f, 0.5f, InterpolationType.LINEAR)
        assertEquals(360f, rotSpin, 0.001f)
    }

    @Test
    fun testKeyframeEvaluatorVolumeKeyframes() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.AUDIO,
            startTimeMs = 0L,
            durationMs = 4000L,
            volume = 0.5f,
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.VOLUME, 0L, 0.0f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.VOLUME, 2000L, 1.0f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.VOLUME, 4000L, 0.2f, InterpolationType.LINEAR)
            )
        )

        assertEquals(0.0f, KeyframeEvaluator.evaluateVolume(clip, 0L), 0.001f)
        assertEquals(0.5f, KeyframeEvaluator.evaluateVolume(clip, 1000L), 0.001f)
        assertEquals(1.0f, KeyframeEvaluator.evaluateVolume(clip, 2000L), 0.001f)
        assertEquals(0.6f, KeyframeEvaluator.evaluateVolume(clip, 3000L), 0.001f)
        assertEquals(0.2f, KeyframeEvaluator.evaluateVolume(clip, 4000L), 0.001f)
    }

    @Test
    fun testImageOverlayGeneratorCanvasReusePreventsAllocationChurn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = Clip(
            id = "img1",
            trackId = "t_overlay",
            assetId = "asset_img",
            type = ClipType.IMAGE,
            startTimeMs = 0L,
            durationMs = 3000L,
            isVisible = true
        )
        val asset = Asset(
            id = "asset_img",
            uri = "file:///dummy/path.png",
            mediaType = com.example.core.model.MediaType.IMAGE
        )

        val generator = ImageOverlayGenerator(
            context = context,
            imageClips = listOf(clip),
            assets = mapOf(asset.id to asset),
            videoWidth = 640,
            videoHeight = 360
        )

        // When no active clips (e.g. at 5000ms), returns 1x1 emptyBitmap
        val emptyBmp1 = generator.getBitmap(5_000_000L) // 5000ms in us
        val emptyBmp2 = generator.getBitmap(6_000_000L) // 6000ms in us
        assertEquals(1, emptyBmp1.width)
        assertEquals(1, emptyBmp1.height)
        assertSame(emptyBmp1, emptyBmp2)

        // Calling close should clean up gracefully
        generator.close()
    }
}
