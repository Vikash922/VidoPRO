package com.example.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
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
class Migration1To2Test {

    private fun createV1Database(context: Context, dbName: String): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v1 tables matching AppDatabase v1 schema
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `projects` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `width` INTEGER NOT NULL,
                            `height` INTEGER NOT NULL,
                            `fps` INTEGER NOT NULL,
                            `durationMs` INTEGER NOT NULL,
                            `aspectRatio` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `thumbnailPath` TEXT,
                            `stateVersion` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tracks` (
                            `id` TEXT NOT NULL,
                            `projectId` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `trackOrder` INTEGER NOT NULL,
                            `isVisible` INTEGER NOT NULL,
                            `isLocked` INTEGER NOT NULL,
                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `assets` (
                            `id` TEXT NOT NULL,
                            `uri` TEXT NOT NULL,
                            `mimeType` TEXT,
                            `mediaType` TEXT NOT NULL,
                            `durationMs` INTEGER,
                            `width` INTEGER,
                            `height` INTEGER,
                            `sizeBytes` INTEGER,
                            `displayName` TEXT,
                            `thumbnailPath` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `clips` (
                            `id` TEXT NOT NULL,
                            `trackId` TEXT NOT NULL,
                            `assetId` TEXT,
                            `type` TEXT NOT NULL,
                            `startTimeMs` INTEGER NOT NULL,
                            `durationMs` INTEGER NOT NULL,
                            `inPointMs` INTEGER NOT NULL,
                            `outPointMs` INTEGER NOT NULL,
                            `speed` REAL NOT NULL,
                            `volume` REAL,
                            `isVisible` INTEGER NOT NULL,
                            `zIndex` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`assetId`) REFERENCES `assets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun testMigration1To2PreservesDataAndAddsGroupId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test.db"

        // 1. Create database with version 1 schema and insert test records
        val db = createV1Database(context, dbName)

        db.execSQL("""
            INSERT INTO `projects` VALUES (
                'proj_1', 'My Project', 1080, 1920, 30, 10000, 'RATIO_9_16', 1000, 1000, null, 1
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `tracks` VALUES (
                'track_1', 'proj_1', 'VIDEO', 0, 1, 0
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `assets` VALUES (
                'asset_1', 'content://media/1', 'video/mp4', 'VIDEO', 10000, 1920, 1080, 50000, 'clip.mp4', null, 1000
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `clips` VALUES (
                'clip_1', 'track_1', 'asset_1', 'VIDEO', 0, 5000, 0, 5000, 1.0, 1.0, 1, 0, 1000, 1000
            )
        """.trimIndent())

        // 2. Execute Migration from version 1 to 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // 3. Verify existing project, track, asset, and clip are untouched
        val projectCursor = db.query("SELECT id, name FROM projects WHERE id = 'proj_1'")
        assertTrue("Project must exist after migration", projectCursor.moveToFirst())
        assertEquals("My Project", projectCursor.getString(1))
        projectCursor.close()

        val trackCursor = db.query("SELECT id, type FROM tracks WHERE id = 'track_1'")
        assertTrue("Track must exist after migration", trackCursor.moveToFirst())
        assertEquals("VIDEO", trackCursor.getString(1))
        trackCursor.close()

        val assetCursor = db.query("SELECT id, displayName FROM assets WHERE id = 'asset_1'")
        assertTrue("Asset must exist after migration", assetCursor.moveToFirst())
        assertEquals("clip.mp4", assetCursor.getString(1))
        assetCursor.close()

        // 4. Verify existing clip still exists and groupId is null
        val clipCursor = db.query("SELECT id, durationMs, groupId FROM clips WHERE id = 'clip_1'")
        assertTrue("Clip must exist after migration", clipCursor.moveToFirst())
        assertEquals("clip_1", clipCursor.getString(0))
        assertEquals(5000L, clipCursor.getLong(1))
        assertTrue("groupId should be NULL for pre-existing clip", clipCursor.isNull(2))
        clipCursor.close()

        // 5. Verify groupId column can be written and read
        db.execSQL("UPDATE clips SET groupId = 'group_alpha' WHERE id = 'clip_1'")

        val updatedClipCursor = db.query("SELECT id, groupId FROM clips WHERE id = 'clip_1'")
        assertTrue(updatedClipCursor.moveToFirst())
        assertEquals("group_alpha", updatedClipCursor.getString(1))
        updatedClipCursor.close()

        db.close()
    }
}
