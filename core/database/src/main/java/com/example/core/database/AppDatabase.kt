package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.core.database.converters.Converters
import com.example.core.database.dao.AssetDao
import com.example.core.database.dao.ClipDao
import com.example.core.database.dao.EffectDao
import com.example.core.database.dao.KeyframeDao
import com.example.core.database.dao.ProjectDao
import com.example.core.database.dao.TextClipDao
import com.example.core.database.dao.TrackDao
import com.example.core.database.dao.TransformDao
import com.example.core.database.dao.TransitionDao
import com.example.core.database.entities.AssetEntity
import com.example.core.database.entities.ClipEntity
import com.example.core.database.entities.EffectEntity
import com.example.core.database.entities.KeyframeEntity
import com.example.core.database.entities.ProjectEntity
import com.example.core.database.entities.TextClipEntity
import com.example.core.database.entities.TrackEntity
import com.example.core.database.entities.TransformEntity
import com.example.core.database.entities.TransitionEntity

@Database(
    entities = [
        ProjectEntity::class,
        TrackEntity::class,
        ClipEntity::class,
        AssetEntity::class,
        TransformEntity::class,
        EffectEntity::class,
        KeyframeEntity::class,
        TextClipEntity::class,
        TransitionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun clipDao(): ClipDao
    abstract fun assetDao(): AssetDao
    abstract fun transformDao(): TransformDao
    abstract fun effectDao(): EffectDao
    abstract fun keyframeDao(): KeyframeDao
    abstract fun textClipDao(): TextClipDao
    abstract fun transitionDao(): TransitionDao

    companion object {
        const val DATABASE_NAME = "video_editor.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
