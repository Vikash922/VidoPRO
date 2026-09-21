package com.example.core.data.repository

import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.data.mappers.toDomain
import com.example.core.data.mappers.toEntity
import com.example.core.database.AppDatabase
import com.example.core.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AssetRepositoryImpl(
    database: AppDatabase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : AssetRepository {

    private val assetDao = database.assetDao()

    override suspend fun insertAsset(asset: Asset) = withContext(dispatchers.io) {
        assetDao.insert(asset.toEntity())
    }

    override suspend fun getAssetById(assetId: String): Asset? = withContext(dispatchers.io) {
        assetDao.getById(assetId)?.toDomain()
    }

    override fun observeAssets(): Flow<List<Asset>> {
        return assetDao.observeAssets()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun deleteAsset(assetId: String) = withContext(dispatchers.io) {
        assetDao.deleteById(assetId)
    }
}
