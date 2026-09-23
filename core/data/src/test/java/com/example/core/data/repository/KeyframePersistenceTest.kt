package com.example.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
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
class KeyframePersistenceTest {

    @Test
    fun testKeyframePersistenceSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "keyframe_persistence_test.db"
        context.deleteDatabase(dbName)

        val projectId = "project_kf_1"
        val trackId = "track_kf_1"
        val clipId = "clip_kf_1"

        val kf1 = Keyframe(
            id = "kf_1",
            clipId = clipId,
            property = KeyframeProperty.POSITION_X,
            timeMs = 1500L,
            value = 120.5f,
            interpolation = InterpolationType.LINEAR
        )

        val kf2 = Keyframe(
            id = "kf_2",
            clipId = clipId,
            property = KeyframeProperty.SCALE_X,
            timeMs = 2500L,
            value = 1.8f,
            interpolation = InterpolationType.EASE_IN_OUT
        )

        val kf3 = Keyframe(
            id = "kf_3",
            clipId = clipId,
            property = KeyframeProperty.ROTATION,
            timeMs = 3500L,
            value = 45f,
            interpolation = InterpolationType.BEZIER
        )

        val clip = Clip(
            id = clipId,
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 1000L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L,
            keyframes = listOf(kf1, kf2, kf3)
        )

        val clipSolo = Clip(
            id = "clip_solo",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 6000L,
            durationMs = 2000L,
            inPointMs = 0L,
            outPointMs = 2000L,
            keyframes = emptyList()
        )

        val track = Track(
            id = trackId,
            projectId = projectId,
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clip, clipSolo)
        )

        val initialProject = Project(
            id = projectId,
            name = "Keyframe Test Project",
            aspectRatio = AspectRatio.RATIO_9_16,
            tracks = listOf(track)
        )

        // 1. Open database and save project with keyframes
        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo1 = ProjectRepositoryImpl(db1)
        repo1.updateProject(initialProject)

        // Close db1
        db1.close()

        // 2. Reopen database simulating application restart
        val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo2 = ProjectRepositoryImpl(db2)

        // 3. Load project and verify keyframes
        val loadedProject = repo2.getProjectById(projectId)
        assertNotNull("Project must load after db reopen", loadedProject)

        val loadedTrack = loadedProject!!.tracks.first { it.id == trackId }
        val loadedClip = loadedTrack.clips.first { it.id == clipId }
        val loadedSolo = loadedTrack.clips.first { it.id == "clip_solo" }

        assertEquals("Clip should contain exactly 3 persisted keyframes", 3, loadedClip.keyframes.size)
        assertTrue("Solo clip should have no keyframes", loadedSolo.keyframes.isEmpty())

        val loadedKf1 = loadedClip.keyframes.first { it.id == "kf_1" }
        assertEquals(clipId, loadedKf1.clipId)
        assertEquals(KeyframeProperty.POSITION_X, loadedKf1.property)
        assertEquals(1500L, loadedKf1.timeMs)
        assertEquals(120.5f, loadedKf1.value, 0.001f)
        assertEquals(InterpolationType.LINEAR, loadedKf1.interpolation)

        val loadedKf2 = loadedClip.keyframes.first { it.id == "kf_2" }
        assertEquals(KeyframeProperty.SCALE_X, loadedKf2.property)
        assertEquals(2500L, loadedKf2.timeMs)
        assertEquals(1.8f, loadedKf2.value, 0.001f)
        assertEquals(InterpolationType.EASE_IN_OUT, loadedKf2.interpolation)

        val loadedKf3 = loadedClip.keyframes.first { it.id == "kf_3" }
        assertEquals(KeyframeProperty.ROTATION, loadedKf3.property)
        assertEquals(3500L, loadedKf3.timeMs)
        assertEquals(45f, loadedKf3.value, 0.001f)
        assertEquals(InterpolationType.BEZIER, loadedKf3.interpolation)

        // 4. Verify via DAO directly
        val daoKeyframes = db2.keyframeDao().getByClipId(clipId)
        assertEquals(3, daoKeyframes.size)

        db2.close()
    }
}
