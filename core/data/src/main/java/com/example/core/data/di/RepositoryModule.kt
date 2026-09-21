package com.example.core.data.di

import android.content.Context
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.data.repository.AssetRepository
import com.example.core.data.repository.AssetRepositoryImpl
import com.example.core.data.repository.ProjectRepository
import com.example.core.data.repository.ProjectRepositoryImpl
import com.example.core.database.AppDatabase
import com.example.core.database.di.DatabaseModule

/**
 * Repository Module providing singleton instances of repositories.
 */
object RepositoryModule {

    @Volatile
    private var projectRepositoryInstance: ProjectRepository? = null

    @Volatile
    private var assetRepositoryInstance: AssetRepository? = null

    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    fun provideProjectRepository(
        database: AppDatabase,
        dispatchers: DispatcherProvider = provideDispatcherProvider()
    ): ProjectRepository {
        return projectRepositoryInstance ?: synchronized(this) {
            projectRepositoryInstance ?: ProjectRepositoryImpl(database, dispatchers).also {
                projectRepositoryInstance = it
            }
        }
    }

    fun provideProjectRepository(context: Context): ProjectRepository {
        val database = DatabaseModule.provideAppDatabase(context)
        return provideProjectRepository(database)
    }

    fun provideAssetRepository(
        database: AppDatabase,
        dispatchers: DispatcherProvider = provideDispatcherProvider()
    ): AssetRepository {
        return assetRepositoryInstance ?: synchronized(this) {
            assetRepositoryInstance ?: AssetRepositoryImpl(database, dispatchers).also {
                assetRepositoryInstance = it
            }
        }
    }

    fun provideAssetRepository(context: Context): AssetRepository {
        val database = DatabaseModule.provideAppDatabase(context)
        return provideAssetRepository(database)
    }
}
