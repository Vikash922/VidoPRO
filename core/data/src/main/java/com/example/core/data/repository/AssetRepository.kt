package com.example.core.data.repository

import com.example.core.model.Asset
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    suspend fun insertAsset(asset: Asset)
    suspend fun getAssetById(assetId: String): Asset?
    fun observeAssets(): Flow<List<Asset>>
    suspend fun deleteAsset(assetId: String)
}
