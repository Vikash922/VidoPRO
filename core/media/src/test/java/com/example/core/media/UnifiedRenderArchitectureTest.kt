package com.example.core.media

import com.example.core.media.render.CanvasConfig
import com.example.core.media.render.ImageRenderLayer
import com.example.core.media.render.RenderExportException
import com.example.core.media.render.RenderScene
import com.example.core.media.render.RenderSceneBuilder
import com.example.core.media.render.RenderTransform
import com.example.core.media.render.RenderTransformEvaluator
import com.example.core.media.render.TextRenderLayer
import com.example.core.media.render.TimeMapping
import com.example.core.media.render.VideoRenderLayer
import com.example.core.model.AspectRatio
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.MediaType
import com.example.core.model.Project
import com.example.core.model.TextClipData
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive verification of the Unified Rendering / Compositor Architecture.
 *
 * Verifies Requirements A through O, deterministic Z-ordering, time-mapping parity,
 * multi-resolution coordinate transformations, and regression test projects across
 * 9:16, 16:9, 1:1, and 4:5 canvases.
 */
class UnifiedRenderArchitectureTest {

    private fun createStandardRegressionProject(aspectRatio: AspectRatio = AspectRatio.RATIO_9_16): Pair<Project, Map<String, Asset>> {
        val (w, h) = when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> 1080 to 1920
            AspectRatio.RATIO_16_9 -> 1920 to 1080
            AspectRatio.RATIO_1_1 -> 1080 to 1080
            AspectRatio.RATIO_4_5 -> 1080 to 1350
        }

        val videoAsset = Asset(id = "asset_vid_1", uri = "file:///sample_main.mp4", mediaType = MediaType.VIDEO, width = w, height = h, durationMs = 15000L)
        val pipVideoAsset = Asset(id = "asset_pip_1", uri = "file:///sample_pip.mp4", mediaType = MediaType.VIDEO, width = 640, height = 360, durationMs = 8000L)
        val imageAsset = Asset(id = "asset_img_1", uri = "file:///sample_logo.png", mediaType = MediaType.IMAGE, width = 400, height = 400)
        val audioAssetA = Asset(id = "asset_aud_1", uri = "file:///sample_music.mp3", mediaType = MediaType.AUDIO, durationMs = 5000L)
        val audioAssetB = Asset(id = "asset_aud_2", uri = "file:///sample_sfx.mp3", mediaType = MediaType.AUDIO, durationMs = 4000L)

        val assets = mapOf(
            videoAsset.id to videoAsset,
            pipVideoAsset.id to pipVideoAsset,
            imageAsset.id to imageAsset,
            audioAssetA.id to audioAssetA,
            audioAssetB.id to audioAssetB
        )

        // Main Video: 0s -> 10s (trimmed inPoint 1000ms, speed 1.0x)
        val mainClip = Clip(
            id = "clip_main",
            trackId = "track_video",
            type = ClipType.VIDEO,
            assetId = videoAsset.id,
            startTimeMs = 0L,
            durationMs = 10000L,
            inPointMs = 1000L,
            outPointMs = 11000L,
            transform = Transform(x = 10f, y = -20f, scaleX = 1.1f, scaleY = 1.1f, rotation = 5f, opacity = 0.95f),
            effects = listOf(Effect(id = "eff_1", clipId = "clip_main", type = EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 15f)))
        )

        // Video Overlay (PiP): 2s -> 7s (speed 1.5x, volume 0.8)
        val pipClip = Clip(
            id = "clip_pip",
            trackId = "track_overlay",
            type = ClipType.VIDEO,
            assetId = pipVideoAsset.id,
            startTimeMs = 2000L,
            durationMs = 5000L,
            inPointMs = 500L,
            outPointMs = 8000L,
            speed = 1.5f,
            volume = 0.8f,
            transform = Transform(x = 100f, y = 150f, scaleX = 0.8f, scaleY = 0.8f)
        )

        // Image Overlay: 4s -> 9s with animated opacity keyframes
        val imageClip = Clip(
            id = "clip_image",
            trackId = "track_overlay",
            type = ClipType.IMAGE,
            assetId = imageAsset.id,
            startTimeMs = 4000L,
            durationMs = 5000L,
            transform = Transform(x = -50f, y = 50f),
            keyframes = listOf(
                Keyframe(id = "kf_op_1", clipId = "clip_image", property = KeyframeProperty.OPACITY, timeMs = 4000L, value = 0.0f),
                Keyframe(id = "kf_op_2", clipId = "clip_image", property = KeyframeProperty.OPACITY, timeMs = 6000L, value = 1.0f)
            )
        )

        // Text Overlay: 1s -> 6s
        val textClip = Clip(
            id = "clip_text",
            trackId = "track_text",
            type = ClipType.TEXT,
            startTimeMs = 1000L,
            durationMs = 5000L,
            transform = Transform(x = 0f, y = 200f),
            textData = TextClipData(clipId = "clip_text", text = "VidoPRO Title", fontSize = 28f, textColor = "#FFCC00")
        )

        // Audio Clips with explicit 3-second GAP:
        // Audio A: 0s -> 4s
        // Gap: 4s -> 7s
        // Audio B: 7s -> 11s
        val audioClipA = Clip(
            id = "clip_audio_a",
            trackId = "track_audio",
            type = ClipType.AUDIO,
            assetId = audioAssetA.id,
            startTimeMs = 0L,
            durationMs = 4000L,
            volume = 1.0f
        )
        val audioClipB = Clip(
            id = "clip_audio_b",
            trackId = "track_audio",
            type = ClipType.AUDIO,
            assetId = audioAssetB.id,
            startTimeMs = 7000L,
            durationMs = 4000L,
            volume = 0.7f
        )

        val project = Project(
            id = "proj_regression",
            name = "Regression Project",
            width = w,
            height = h,
            aspectRatio = aspectRatio,
            durationMs = 11000L,
            tracks = listOf(
                Track(id = "track_video", projectId = "proj_regression", type = TrackType.VIDEO, order = 0, clips = listOf(mainClip)),
                Track(id = "track_overlay", projectId = "proj_regression", type = TrackType.OVERLAY, order = 1, clips = listOf(pipClip, imageClip)),
                Track(id = "track_text", projectId = "proj_regression", type = TrackType.TEXT, order = 2, clips = listOf(textClip)),
                Track(id = "track_audio", projectId = "proj_regression", type = TrackType.AUDIO, order = 3, clips = listOf(audioClipA, audioClipB))
            )
        )

        return Pair(project, assets)
    }

    // ── A & B: Main Video and Main Video Transform ──────────────────────────
    @Test
    fun testMainVideoAndTransformSceneMapping() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        assertEquals(1, scene.videoLayers.filter { it.isMainVideo }.size)
        val mainLayer = scene.mainVideoLayerAt(0L)
        assertNotNull(mainLayer)
        assertEquals("clip_main", mainLayer?.id)
        assertEquals("file:///sample_main.mp4", mainLayer?.sourceUri)
        assertEquals(0L, mainLayer?.timelineStartMs)
        assertEquals(10000L, mainLayer?.timelineEndMs)
        assertEquals(1000L, mainLayer?.sourceInPointMs)
        assertEquals(11000L, mainLayer?.sourceOutPointMs)

        // Evaluate transform
        val evaluatedTransform = mainLayer!!.evaluateTransformAt(5000L)
        assertEquals(10f, evaluatedTransform.x, 0.001f)
        assertEquals(-20f, evaluatedTransform.y, 0.001f)
        assertEquals(1.1f, evaluatedTransform.scaleX, 0.001f)
        assertEquals(1.1f, evaluatedTransform.scaleY, 0.001f)
        assertEquals(5f, evaluatedTransform.rotation, 0.001f)
        assertEquals(0.95f, evaluatedTransform.opacity, 0.001f)
    }

    // ── C & D: Image and Video Overlays ──────────────────────────────────────
    @Test
    fun testImageAndVideoOverlayLayers() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        // PiP Video Overlay
        val pipLayers = scene.videoLayers.filter { !it.isMainVideo }
        assertEquals(1, pipLayers.size)
        val pip = pipLayers[0]
        assertEquals("clip_pip", pip.id)
        assertEquals(2000L, pip.timelineStartMs)
        assertEquals(7000L, pip.timelineEndMs)
        assertEquals(1.5f, pip.speed, 0.001f)
        assertEquals(0.8f, pip.volume, 0.001f)

        // Image Overlay
        assertEquals(1, scene.imageLayers.size)
        val img = scene.imageLayers[0]
        assertEquals("clip_image", img.id)
        assertEquals(4000L, img.timelineStartMs)
        assertEquals(9000L, img.timelineEndMs)
        assertEquals("file:///sample_logo.png", img.sourceUri)
    }

    // ── E: Text Overlay Layer ────────────────────────────────────────────────
    @Test
    fun testTextOverlayLayer() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        assertEquals(1, scene.textLayers.size)
        val text = scene.textLayers[0]
        assertEquals("clip_text", text.id)
        assertEquals(1000L, text.timelineStartMs)
        assertEquals(6000L, text.timelineEndMs)
        assertEquals("VidoPRO Title", text.textData.text)
        assertEquals("#FFCC00", text.textData.textColor)
    }

    // ── F & G: Timeline Offset & Trimmed Media ──────────────────────────────
    @Test
    fun testTimelineOffsetAndTrimmedMedia() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        val main = scene.mainVideoLayerAt(0L)!!
        assertEquals(10000L, main.durationMs)
        assertEquals(1000L, main.sourceInPointMs)

        // Active checks at specific timestamps
        assertTrue(main.isActiveAt(0L))
        assertTrue(main.isActiveAt(9999L))
        assertFalse(main.isActiveAt(10000L))

        val pip = scene.videoLayers.first { !it.isMainVideo }
        assertFalse(pip.isActiveAt(1000L))
        assertTrue(pip.isActiveAt(2000L))
        assertTrue(pip.isActiveAt(5000L))
        assertFalse(pip.isActiveAt(7000L))
    }

    // ── H & I: Multiple Layers and Deterministic Z-Order ────────────────────
    @Test
    fun testLayerPrecedenceAndZOrder() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        val allVisuals = scene.allVisualLayers
        assertEquals(4, allVisuals.size)

        // Strict Z-Order Precedence:
        // Main Video (0..99) < Video Overlay (100..199) < Image Overlay (200..299) < Text Overlay (300..399)
        assertTrue(allVisuals[0] is VideoRenderLayer && (allVisuals[0] as VideoRenderLayer).isMainVideo)
        assertTrue(allVisuals[1] is VideoRenderLayer && !(allVisuals[1] as VideoRenderLayer).isMainVideo)
        assertTrue(allVisuals[2] is ImageRenderLayer)
        assertTrue(allVisuals[3] is TextRenderLayer)

        assertTrue(allVisuals[0].zIndex < allVisuals[1].zIndex)
        assertTrue(allVisuals[1].zIndex < allVisuals[2].zIndex)
        assertTrue(allVisuals[2].zIndex < allVisuals[3].zIndex)

        // At timestamp 5000ms: Main Video, PiP, Image, and Text are ALL active
        val activeAt5s = scene.activeVisualLayersAt(5000L)
        assertEquals(4, activeAt5s.size)
        assertEquals("clip_main", activeAt5s[0].id)
        assertEquals("clip_pip", activeAt5s[1].id)
        assertEquals("clip_image", activeAt5s[2].id)
        assertEquals("clip_text", activeAt5s[3].id)
    }

    // ── J: Canvas Aspect Ratios (9:16, 16:9, 1:1, 4:5) ──────────────────────
    @Test
    fun testCanvasAspectRatios() {
        val ratios = listOf(
            AspectRatio.RATIO_9_16 to (1080 to 1920),
            AspectRatio.RATIO_16_9 to (1920 to 1080),
            AspectRatio.RATIO_1_1 to (1080 to 1080),
            AspectRatio.RATIO_4_5 to (1080 to 1350)
        )

        for ((ratio, dims) in ratios) {
            val (project, assets) = createStandardRegressionProject(ratio)
            val scene = RenderSceneBuilder.buildScene(project, assets)

            assertEquals(dims.first, scene.canvasConfig.width)
            assertEquals(dims.second, scene.canvasConfig.height)
            assertEquals(ratio, scene.canvasConfig.aspectRatio)

            // Test coordinate mapping to preview and export
            val t = RenderTransform(x = 100f, y = -100f, scaleX = 1f, scaleY = 1f)
            val previewT = scene.canvasConfig.projectToPreview(t, 360f, 360f / ratio.floatRatio)
            assertTrue(previewT.x > 0f)

            val (ndcX, ndcY) = scene.canvasConfig.projectToExportNdc(t, dims.first, dims.second)
            assertEquals((200f / dims.first), ndcX, 0.001f)
            assertEquals((200f / dims.second), ndcY, 0.001f) // Y inverted in NDC
        }
    }

    // ── K: Project Time -> Source Time Mapping (Dev-05, Dev-06) ─────────────
    @Test
    fun testTimeMappingWithTrimAndSpeed() {
        // Normal 1.0x speed, trimmed inPoint = 1000ms, start = 2000ms
        val srcTime1 = TimeMapping.projectTimeToSourceTime(
            projectTimeMs = 5000L,
            layerStartMs = 2000L,
            sourceInPointMs = 1000L,
            speed = 1.0f
        )
        // Elapsed = 3000ms -> source = 1000 + 3000 = 4000ms
        assertEquals(4000L, srcTime1)

        // Fast-forward 2.0x speed, inPoint = 500ms, start = 2000ms
        val srcTime2 = TimeMapping.projectTimeToSourceTime(
            projectTimeMs = 5000L,
            layerStartMs = 2000L,
            sourceInPointMs = 500L,
            speed = 2.0f
        )
        // Elapsed = 3000ms * 2.0 = 6000ms -> source = 500 + 6000 = 6500ms
        assertEquals(6500L, srcTime2)

        // Slow-motion 0.5x speed
        val srcTime3 = TimeMapping.projectTimeToSourceTime(
            projectTimeMs = 4000L,
            layerStartMs = 2000L,
            sourceInPointMs = 0L,
            speed = 0.5f
        )
        // Elapsed = 2000ms * 0.5 = 1000ms -> source = 1000ms
        assertEquals(1000L, srcTime3)
    }

    // ── L: Transform Evaluation with Keyframes ───────────────────────────────
    @Test
    fun testKeyframeAnimatedTransformEvaluation() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        val imageLayer = scene.imageLayers.first { it.id == "clip_image" }

        // Keyframe opacity: 4000ms -> 0.0f, 6000ms -> 1.0f
        val tAtStart = imageLayer.evaluateTransformAt(4000L)
        assertEquals(0.0f, tAtStart.opacity, 0.001f)

        val tAtMid = imageLayer.evaluateTransformAt(5000L)
        assertEquals(0.5f, tAtMid.opacity, 0.001f)

        val tAtEnd = imageLayer.evaluateTransformAt(6000L)
        assertEquals(1.0f, tAtEnd.opacity, 0.001f)

        // After last keyframe: clamps to final value
        val tAfter = imageLayer.evaluateTransformAt(8000L)
        assertEquals(1.0f, tAfter.opacity, 0.001f)
    }

    // ── M & N & O: Parity between Preview and Export RenderScenes ────────────
    @Test
    fun testPreviewAndExportSceneParity() {
        val (project, assets) = createStandardRegressionProject()

        // Generate scene derived for Preview
        val previewScene = RenderSceneBuilder.buildScene(project, assets)

        // Generate scene derived for Export
        val exportScene = RenderSceneBuilder.buildScene(project, assets)

        // EXACT EQUALITY: Both backends consume identical layer count, durations, timing, and transforms
        assertEquals(previewScene.durationMs, exportScene.durationMs)
        assertEquals(previewScene.canvasConfig, exportScene.canvasConfig)
        assertEquals(previewScene.videoLayers.size, exportScene.videoLayers.size)
        assertEquals(previewScene.imageLayers.size, exportScene.imageLayers.size)
        assertEquals(previewScene.textLayers.size, exportScene.textLayers.size)
        assertEquals(previewScene.audioLayers.size, exportScene.audioLayers.size)

        for (t in 0L..10000L step 1000L) {
            val previewLayers = previewScene.activeVisualLayersAt(t)
            val exportLayers = exportScene.activeVisualLayersAt(t)

            assertEquals(previewLayers.size, exportLayers.size)
            for (i in previewLayers.indices) {
                val pLayer = previewLayers[i]
                val eLayer = exportLayers[i]

                assertEquals(pLayer.id, eLayer.id)
                assertEquals(pLayer.zIndex, eLayer.zIndex)
                assertEquals(pLayer.timelineStartMs, eLayer.timelineStartMs)
                assertEquals(pLayer.timelineEndMs, eLayer.timelineEndMs)

                val pT = pLayer.evaluateTransformAt(t)
                val eT = eLayer.evaluateTransformAt(t)
                assertEquals(pT.x, eT.x, 0.0001f)
                assertEquals(pT.y, eT.y, 0.0001f)
                assertEquals(pT.scaleX, eT.scaleX, 0.0001f)
                assertEquals(pT.scaleY, eT.scaleY, 0.0001f)
                assertEquals(pT.rotation, eT.rotation, 0.0001f)
                assertEquals(pT.opacity, eT.opacity, 0.0001f)
            }
        }
    }

    // ── Audio Gap Preservation in Scene ─────────────────────────────────────
    @Test
    fun testAudioGapsInScene() {
        val (project, assets) = createStandardRegressionProject()
        val scene = RenderSceneBuilder.buildScene(project, assets)

        // Audio A: 0..4s, Audio B: 7..11s, Audible PiP: 2..7s
        val audioLayers = scene.audioLayers
        assertTrue(audioLayers.size >= 2)

        val clipA = audioLayers.first { it.id == "clip_audio_a" }
        val clipB = audioLayers.first { it.id == "clip_audio_b" }

        val gapMs = clipB.timelineStartMs - clipA.timelineEndMs
        assertEquals(3000L, gapMs) // 3-second explicit silence gap preserved
    }

    // ── Exception Categorization ────────────────────────────────────────────
    @Test
    fun testExceptionTaxonomy() {
        val inv = RenderExportException.InvalidTimelineException("No clips")
        assertTrue(inv.message!!.contains("Invalid timeline"))

        val oom = RenderExportException.OutOfMemoryRenderException("Buffer exhausted")
        assertTrue(oom.message!!.contains("Out of memory"))

        val codec = RenderExportException.UnsupportedCodecException("AV1")
        assertEquals("AV1", codec.codecName)
    }
}
