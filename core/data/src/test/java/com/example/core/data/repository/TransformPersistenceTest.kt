package com.example.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.Transform
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransformPersistenceTest {

    @Test
    fun testTransformPersistenceSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "transform_persistence_test.db"
        context.deleteDatabase(dbName)

        val projectId = "project_transform_1"
        val trackId = "track_video_1"

        val customTransform = Transform(
            x = 120.5f,
            y = -45.0f,
            scaleX = 1.75f,
            scaleY = 1.75f,
            rotation = 90f,
            opacity = 0.85f,
            anchorX = 0.5f,
            anchorY = 0.5f
        )

        val clipWithTransform = Clip(
            id = "clip_transformed",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 4000L,
            inPointMs = 0L,
            outPointMs = 4000L,
            transform = customTransform
        )

        val defaultClip = Clip(
            id = "clip_default",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 4000L,
            durationMs = 3000L,
            inPointMs = 0L,
            outPointMs = 3000L,
            transform = Transform.DEFAULT
        )

        val track = Track(
            id = trackId,
            projectId = projectId,
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clipWithTransform, defaultClip)
        )

        val project = Project(
            id = projectId,
            name = "Transform Test Project",
            width = 1080,
            height = 1920,
            aspectRatio = AspectRatio.RATIO_9_16,
            tracks = listOf(track)
        )

        // 1. Open database and save project with transformed clips
        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo1 = ProjectRepositoryImpl(db1)

        repo1.updateProject(project)
        db1.close()

        // 2. Re-open database from disk simulating app restart
        val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo2 = ProjectRepositoryImpl(db2)

        // 3. Load project and verify transform properties preserved
        val loadedProject = repo2.getProjectById(projectId)
        assertNotNull("Project must load after database reopen", loadedProject)

        val loadedTrack = loadedProject!!.tracks.first { it.id == trackId }
        val loadedClipTransformed = loadedTrack.clips.first { it.id == "clip_transformed" }
        val loadedClipDefault = loadedTrack.clips.first { it.id == "clip_default" }

        val t = loadedClipTransformed.transform
        assertEquals("X offset must be preserved", 120.5f, t.x, 0.0001f)
        assertEquals("Y offset must be preserved", -45.0f, t.y, 0.0001f)
        assertEquals("scaleX must be preserved", 1.75f, t.scaleX, 0.0001f)
        assertEquals("scaleY must be preserved", 1.75f, t.scaleY, 0.0001f)
        assertEquals("rotation must be preserved", 90f, t.rotation, 0.0001f)
        assertEquals("opacity must be preserved", 0.85f, t.opacity, 0.0001f)
        assertEquals("anchorX must be preserved", 0.5f, t.anchorX, 0.0001f)
        assertEquals("anchorY must be preserved", 0.5f, t.anchorY, 0.0001f)

        assertEquals("Default clip must retain DEFAULT transform", Transform.DEFAULT, loadedClipDefault.transform)

        db2.close()
    }
}
