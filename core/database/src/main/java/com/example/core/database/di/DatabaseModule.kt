package com.example.core.database.di

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.core.database.dao.AssetDao
import com.example.core.database.dao.ClipDao
import com.example.core.database.dao.EffectDao
import com.example.core.database.dao.KeyframeDao
import com.example.core.database.dao.ProjectDao
import com.example.core.database.dao.TextClipDao
import com.example.core.database.dao.TrackDao
import com.example.core.database.dao.TransformDao
import com.example.core.database.dao.TransitionDao

/**
 * Database Module providing AppDatabase and DAO singletons.
 */
object DatabaseModule {

    @Volatile
    private var databaseInstance: AppDatabase? = null

    fun provideAppDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: AppDatabase.getInstance(context).also { databaseInstance = it }
        }
    }

    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()
    fun provideTrackDao(database: AppDatabase): TrackDao = database.trackDao()
    fun provideClipDao(database: AppDatabase): ClipDao = database.clipDao()
    fun provideAssetDao(database: AppDatabase): AssetDao = database.assetDao()
    fun provideTransformDao(database: AppDatabase): TransformDao = database.transformDao()
    fun provideEffectDao(database: AppDatabase): EffectDao = database.effectDao()
    fun provideKeyframeDao(database: AppDatabase): KeyframeDao = database.keyframeDao()
    fun provideTextClipDao(database: AppDatabase): TextClipDao = database.textClipDao()
    fun provideTransitionDao(database: AppDatabase): TransitionDao = database.transitionDao()
}
