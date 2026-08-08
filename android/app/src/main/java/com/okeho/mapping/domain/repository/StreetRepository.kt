package com.okeho.mapping.domain.repository

import com.okeho.mapping.domain.model.Street
import kotlinx.coroutines.flow.Flow

interface StreetRepository {
    fun getAllStreets(): Flow<List<Street>>
    suspend fun getStreetById(id: String): Street?
    suspend fun insertStreet(street: Street)
    suspend fun updateStreet(street: Street)
    suspend fun deleteStreet(street: Street)
    suspend fun getPendingStreets(): List<Street>
    suspend fun updateSyncStatus(id: String, status: String)
}
