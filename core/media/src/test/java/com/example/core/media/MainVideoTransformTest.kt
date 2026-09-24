package com.example.core.media

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainVideoTransformTest {

    private fun createVideoClip(
        id: String = "clip_main",
        transform: Transform = Transform.DEFAULT,
        keyframes: List<Keyframe> = emptyList()
    ): Clip {
        return Clip(
            id = id,
            trackId = "track_main",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L,
            transform = transform,
            keyframes = keyframes
        )
    }

    // A. Default transform
    @Test
    fun testDefaultTransformProducesNoEffect() {
        val clip = createVideoClip(transform = Transform.DEFAULT)
        val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920)
        assertNull("Default transform should produce null effect to avoid GPU overhead", effect)

        val (ndcX, ndcY) = CanvasCoordinateHelper.toNdcCoordinates(clip.transform, 1080, 1920)
        assertEquals(0f, ndcX, 0.0001f)
        assertEquals(0f, ndcY, 0.0001f)
    }

    // B. Position change
    @Test
    fun testPositionChangeNdcMapping() {
        // Move horizontally by +270px (1/4th of 1080) and vertically by +480px (1/4th of 1920)
        val transform = Transform(x = 270f, y = 480f)
        val clip = createVideoClip(transform = transform)

        val (ndcX, ndcY) = CanvasCoordinateHelper.toNdcCoordinates(transform, 1080, 1920)
        // 2 * 270 / 1080 = 0.5f
        assertEquals(0.5f, ndcX, 0.0001f)
        // -(2 * 480 / 1920) = -0.5f (inverted in NDC)
        assertEquals(-0.5f, ndcY, 0.0001f)

        val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920)
        assertNotNull(effect)
        assertTrue(effect is MatrixTransformation)

        val matrix = (effect as MatrixTransformation).getMatrix(0L)
        val values = FloatArray(9)
        matrix.getValues(values)
        assertEquals(0.5f, values[Matrix.MTRANS_X], 0.0001f)
        assertEquals(-0.5f, values[Matrix.MTRANS_Y], 0.0001f)
    }

    // C. Scale change (0.5x, 1.0x, 1.5x, 2.0x)
    @Test
    fun testScaleChangeValues() {
        val testScales = listOf(0.5f, 1.0f, 1.5f, 2.0f)
        for (scale in testScales) {
            val transform = Transform(scaleX = scale, scaleY = scale)
            val clip = createVideoClip(transform = transform)
            val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920)

            if (scale == 1.0f) {
                assertNull(effect)
            } else {
                assertNotNull(effect)
                val matrix = (effect as MatrixTransformation).getMatrix(0L)
                val values = FloatArray(9)
                matrix.getValues(values)
                assertEquals(scale, values[Matrix.MSCALE_X], 0.0001f)
                assertEquals(scale, values[Matrix.MSCALE_Y], 0.0001f)
            }
        }
    }

    // D. Rotation (0°, 90°, 180°, 270°, and arbitrary 45°)
    @Test
    fun testRotationAngles() {
        val angles = listOf(0f, 90f, 180f, 270f, 45f)
        for (angle in angles) {
            val transform = Transform(rotation = angle)
            val clip = createVideoClip(transform = transform)
            val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920)

            if (angle == 0f) {
                assertNull(effect)
            } else {
                assertNotNull(effect)
                val matrix = (effect as MatrixTransformation).getMatrix(0L)
                val values = FloatArray(9)
                matrix.getValues(values)

                val radians = Math.toRadians(-angle.toDouble())
                val expectedCos = Math.cos(radians).toFloat()
                val expectedSin = Math.sin(radians).toFloat()

                assertEquals(expectedCos, values[Matrix.MSCALE_X], 0.01f)
                assertEquals(-expectedSin, values[Matrix.MSKEW_X], 0.01f)
                assertEquals(expectedSin, values[Matrix.MSKEW_Y], 0.01f)
                assertEquals(expectedCos, values[Matrix.MSCALE_Y], 0.01f)
            }
        }
    }

    // E. Different canvas ratios (9:16, 16:9, 1:1, 4:5)
    @Test
    fun testDifferentCanvasAspectRatios() {
        val ratios = listOf(
            AspectRatio.RATIO_9_16 to (1080 to 1920),
            AspectRatio.RATIO_16_9 to (1920 to 1080),
            AspectRatio.RATIO_1_1 to (1080 to 1080),
            AspectRatio.RATIO_4_5 to (1080 to 1350)
        )

        for ((aspectRatio, dims) in ratios) {
            val (width, height) = dims
            val resolvedDims = CanvasCoordinateHelper.getDimensionsForAspectRatio(aspectRatio)
            assertEquals(width, resolvedDims.first)
            assertEquals(height, resolvedDims.second)

            // Shift horizontally by 20% of width
            val shiftX = width * 0.2f
            val shiftY = height * 0.2f
            val transform = Transform(x = shiftX, y = shiftY)

            val (ndcX, ndcY) = CanvasCoordinateHelper.toNdcCoordinates(transform, width, height)
            assertEquals("NDC X shift must be 20% for $aspectRatio", 0.4f, ndcX, 0.0001f)
            assertEquals("NDC Y shift must be -20% for $aspectRatio", -0.4f, ndcY, 0.0001f)
        }
    }

    // G. Preview transform and round-trip gesture conversion
    @Test
    fun testPreviewTransformAndPanRoundTrip() {
        val projectW = 1080
        val projectH = 1920
        val canvasW = 360f
        val canvasH = 640f

        val initialTransform = Transform(x = 180f, y = 320f, scaleX = 1.5f, scaleY = 1.5f, rotation = 45f)
        val previewTransform = CanvasCoordinateHelper.toPreviewCoordinates(
            initialTransform,
            canvasWidthPx = canvasW,
            canvasHeightPx = canvasH,
            projectWidth = projectW,
            projectHeight = projectH
        )

        assertEquals(60f, previewTransform.x, 0.0001f) // 180 * (360/1080)
        assertEquals(106.66667f, previewTransform.y, 0.001f) // 320 * (640/1920)
        assertEquals(1.5f, previewTransform.scaleX, 0.0001f)
        assertEquals(45f, previewTransform.rotation, 0.0001f)

        // Gesture drag by 30px horizontally on screen
        val (projDx, projDy) = CanvasCoordinateHelper.fromPreviewPan(
            panX = 30f,
            panY = 60f,
            canvasWidthPx = canvasW,
            canvasHeightPx = canvasH,
            projectWidth = projectW,
            projectHeight = projectH
        )
        assertEquals(90f, projDx, 0.0001f) // 30 * (1080/360)
        assertEquals(180f, projDy, 0.0001f) // 60 * (1920/640)
    }

    // H. Export transform matrix verification
    @Test
    fun testExportTransformMatrixWithScaleRotateTranslate() {
        val transform = Transform(x = 540f, y = 960f, scaleX = 2.0f, scaleY = 2.0f, rotation = 90f)
        val clip = createVideoClip(transform = transform)

        val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920)
        assertNotNull(effect)
        val matrix = (effect as MatrixTransformation).getMatrix(0L)

        val values = FloatArray(9)
        matrix.getValues(values)

        // Translation: (2 * 540 / 1080) = 1.0f, -(2 * 960 / 1920) = -1.0f
        assertEquals(1.0f, values[Matrix.MTRANS_X], 0.001f)
        assertEquals(-1.0f, values[Matrix.MTRANS_Y], 0.001f)

        // Rotate -90° (NDC) and scale 2.0x
        // cos(-90) = 0, sin(-90) = -1
        // matrix: scale 2.0 * rotate -90:
        // m00 = 0, m01 = 2, m10 = -2, m11 = 0
        assertEquals(0f, values[Matrix.MSCALE_X], 0.01f)
        assertEquals(2.0f, values[Matrix.MSKEW_X], 0.01f)
        assertEquals(-2.0f, values[Matrix.MSKEW_Y], 0.01f)
        assertEquals(0f, values[Matrix.MSCALE_Y], 0.01f)
    }

    // I. Multiple clips remain unaffected
    @Test
    fun testMultipleClipsRemainIsolated() {
        val clip1 = createVideoClip(id = "clip_1", transform = Transform(scaleX = 1.5f, scaleY = 1.5f, rotation = 90f))
        val clip2 = createVideoClip(id = "clip_2", transform = Transform.DEFAULT)

        val effect1 = Media3EffectHelper.createTransformEffect(clip1, 1080, 1920)
        val effect2 = Media3EffectHelper.createTransformEffect(clip2, 1080, 1920)

        assertNotNull("Clip 1 has custom transform so effect must be present", effect1)
        assertNull("Clip 2 has default transform so effect must be null", effect2)
    }

    // J. Reset transform
    @Test
    fun testResetTransform() {
        val modifiedTransform = Transform(x = 100f, y = -200f, scaleX = 2.5f, scaleY = 2.5f, rotation = 180f, opacity = 0.5f)
        val resetTransform = Transform.DEFAULT

        assertEquals(0f, resetTransform.x, 0.0001f)
        assertEquals(0f, resetTransform.y, 0.0001f)
        assertEquals(1f, resetTransform.scaleX, 0.0001f)
        assertEquals(1f, resetTransform.scaleY, 0.0001f)
        assertEquals(0f, resetTransform.rotation, 0.0001f)
        assertEquals(1f, resetTransform.opacity, 0.0001f)

        val effect = Media3EffectHelper.createTransformEffect(createVideoClip(transform = resetTransform), 1080, 1920)
        assertNull("Reset transform must produce no export effect", effect)
    }

    // K. Keyframed transform interpolation
    @Test
    fun testKeyframedTransformInterpolation() {
        val keyframes = listOf(
            Keyframe(
                id = "kf_1",
                clipId = "clip_main",
                property = KeyframeProperty.SCALE_X,
                timeMs = 0L,
                value = 1.0f,
                interpolation = InterpolationType.LINEAR
            ),
            Keyframe(
                id = "kf_2",
                clipId = "clip_main",
                property = KeyframeProperty.SCALE_X,
                timeMs = 2000L,
                value = 2.0f,
                interpolation = InterpolationType.LINEAR
            ),
            Keyframe(
                id = "kf_3",
                clipId = "clip_main",
                property = KeyframeProperty.ROTATION,
                timeMs = 0L,
                value = 0f,
                interpolation = InterpolationType.LINEAR
            ),
            Keyframe(
                id = "kf_4",
                clipId = "clip_main",
                property = KeyframeProperty.ROTATION,
                timeMs = 2000L,
                value = 180f,
                interpolation = InterpolationType.LINEAR
            )
        )

        val clip = createVideoClip(keyframes = keyframes)

        // At 0ms
        val t0 = KeyframeEvaluator.evaluateTransform(clip, 0L)
        assertEquals(1.0f, t0.scaleX, 0.0001f)
        assertEquals(0f, t0.rotation, 0.0001f)

        // At 1000ms (halfway)
        val t1 = KeyframeEvaluator.evaluateTransform(clip, 1000L)
        assertEquals(1.5f, t1.scaleX, 0.0001f)
        assertEquals(90f, t1.rotation, 0.0001f)

        // At 2000ms (end)
        val t2 = KeyframeEvaluator.evaluateTransform(clip, 2000L)
        assertEquals(2.0f, t2.scaleX, 0.0001f)
        assertEquals(180f, t2.rotation, 0.0001f)

        // Test dynamic export matrix across timestamps
        val effect = Media3EffectHelper.createTransformEffect(clip, 1080, 1920) as MatrixTransformation
        assertNotNull(effect)

        // 1000ms = 1,000,000 us
        val matrix1s = effect.getMatrix(1_000_000L)
        val vals1s = FloatArray(9)
        matrix1s.getValues(vals1s)

        // Scale 1.5x, rotation -90° (cos(-90)=0, sin(-90)=-1)
        assertEquals(0f, vals1s[Matrix.MSCALE_X], 0.01f)
        assertEquals(1.5f, vals1s[Matrix.MSKEW_X], 0.01f)
        assertEquals(-1.5f, vals1s[Matrix.MSKEW_Y], 0.01f)
        assertEquals(0f, vals1s[Matrix.MSCALE_Y], 0.01f)
    }
}
