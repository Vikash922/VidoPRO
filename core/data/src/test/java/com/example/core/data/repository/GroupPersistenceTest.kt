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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupPersistenceTest {

    @Test
    fun testGroupPersistenceSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "group_persistence_test.db"
        context.deleteDatabase(dbName)

        val testGroupId = "group_${UUID.randomUUID()}"
        val projectId = "project_grp_1"
        val trackId = "track_grp_1"

        val clip1 = Clip(
            id = "clip_grp_1",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 3000L,
            inPointMs = 0L,
            outPointMs = 3000L,
            groupId = testGroupId
        )

        val clip2 = Clip(
            id = "clip_grp_2",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 3000L,
            durationMs = 4000L,
            inPointMs = 0L,
            outPointMs = 4000L,
            groupId = testGroupId
        )

        val clipUngrouped = Clip(
            id = "clip_solo",
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = 7000L,
            durationMs = 2000L,
            inPointMs = 0L,
            outPointMs = 2000L,
            groupId = null
        )

        val track = Track(
            id = trackId,
            projectId = projectId,
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clip1, clip2, clipUngrouped)
        )

        val initialProject = Project(
            id = projectId,
            name = "Group Test Project",
            aspectRatio = AspectRatio.RATIO_9_16,
            tracks = listOf(track)
        )

        // 1. Open database, save project with grouped clips
        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo1 = ProjectRepositoryImpl(db1)

        repo1.updateProject(initialProject)

        // Close db1
        db1.close()

        // 2. Re-open database from disk simulating app restart
        val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val repo2 = ProjectRepositoryImpl(db2)

        // 3. Load project and verify groupId persistence
        val loadedProject = repo2.getProjectById(projectId)
        assertNotNull("Project must load after database reopen", loadedProject)

        val loadedTrack = loadedProject!!.tracks.first { it.id == trackId }
        val loadedClip1 = loadedTrack.clips.first { it.id == "clip_grp_1" }
        val loadedClip2 = loadedTrack.clips.first { it.id == "clip_grp_2" }
        val loadedClipSolo = loadedTrack.clips.first { it.id == "clip_solo" }

        // Both grouped clips must retain the exact same groupId across database reopen
        assertEquals("Clip 1 must retain its groupId", testGroupId, loadedClip1.groupId)
        assertEquals("Clip 2 must retain its groupId", testGroupId, loadedClip2.groupId)
        assertEquals("Ungrouped clip must remain null", null, loadedClipSolo.groupId)

        // Verification: query directly via DAO
        val groupedEntities = db2.clipDao().getByGroupId(testGroupId)
        assertEquals("Should find exactly 2 grouped clips via DAO", 2, groupedEntities.size)

        db2.close()
    }
}
