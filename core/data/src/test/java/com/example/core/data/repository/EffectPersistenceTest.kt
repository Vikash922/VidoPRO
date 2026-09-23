package com.example.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EffectPersistenceTest {

    @Test
    fun testClipEffectsPersistenceSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "effect_persistence_test.db"
        context.deleteDatabase(dbName)

        val projectId = "project_eff_1"
        val trackId = "track_eff_1"
        val clipId = "clip_eff_1"

        val effBrightness = Effect(
            id = "eff_1",
            clipId = clipId,
            type = EffectType.BRIGHTNESS,
            order = 0,
            isEnabled = true,
            parameters = mapOf("value" to 20f, "brightness" to 20f)
        )

        val effContrast = Effect(
            id = "eff_2",
            clipId = clipId,
            type = EffectType.CONTRAST,
            order = 1,
            isEnabled = true,
            parameters = mapOf("value" to 1.35f, "contrast" to 1.35f)
        )

        val effSaturation = Effect(
            id = "eff_3",
            clipId = clipId,
            type = EffectType.SATURATION,
            order = 2,
            isEnabled = true,
            parameters = mapOf("value" to 0.85f, "saturation" to 0.85f)
        )

        val effExposure = Effect(
            id = "eff_4",
            clipId = clipId,
            type = EffectType.EXPOSURE,
            order = 3,
            isEnabled = true,
            parameters = mapOf("value" to -10f, "exposure" to -10f)
        )

        val clip = Clip(
            id = clipId,
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L,
            effects = listOf(effBrightness, effContrast, effSaturation, effExposure)
        )

        val track = Track(
            id = trackId,
            projectId = projectId,
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clip)
        )

        val project = Project(
            id = projectId,
            name = "Effect Test Project",
            aspectRatio = AspectRatio.RATIO_9_16,
            durationMs = 5000L,
            tracks = listOf(track)
        )

        // 1. Save project with effects in first database instance
        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        val repo1 = ProjectRepositoryImpl(db1)
        repo1.saveProject(project)
        db1.close()

        // 2. Reopen fresh database instance and reload project
        val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        val repo2 = ProjectRepositoryImpl(db2)
        val loadedProject = repo2.getProjectById(projectId)
        db2.close()

        assertNotNull(loadedProject)
        val loadedTrack = loadedProject!!.tracks.first { it.id == trackId }
        val loadedClip = loadedTrack.clips.first { it.id == clipId }

        assertEquals("All 4 effects must be persisted", 4, loadedClip.effects.size)

        val loadedBrightness = loadedClip.effects.find { it.type == EffectType.BRIGHTNESS }
        assertNotNull(loadedBrightness)
        assertEquals(20f, loadedBrightness!!.parameters["value"]!!, 0.001f)

        val loadedContrast = loadedClip.effects.find { it.type == EffectType.CONTRAST }
        assertNotNull(loadedContrast)
        assertEquals(1.35f, loadedContrast!!.parameters["value"]!!, 0.001f)

        val loadedSaturation = loadedClip.effects.find { it.type == EffectType.SATURATION }
        assertNotNull(loadedSaturation)
        assertEquals(0.85f, loadedSaturation!!.parameters["value"]!!, 0.001f)

        val loadedExposure = loadedClip.effects.find { it.type == EffectType.EXPOSURE }
        assertNotNull(loadedExposure)
        assertEquals(-10f, loadedExposure!!.parameters["value"]!!, 0.001f)
    }
}
