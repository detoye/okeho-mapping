package com.okeho.mapping.data.repository

import com.okeho.mapping.data.local.dao.StreetDao
import com.okeho.mapping.data.local.entity.StreetEntity
import com.okeho.mapping.domain.model.Street
import com.okeho.mapping.domain.repository.StreetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StreetRepositoryImpl @Inject constructor(
    private val streetDao: StreetDao
) : StreetRepository {

    override fun getAllStreets(): Flow<List<Street>> {
        return streetDao.getAllStreets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getStreetById(id: String): Street? {
        return streetDao.getStreetById(id)?.toDomain()
    }

    override suspend fun insertStreet(street: Street) {
        streetDao.insertStreet(StreetEntity.fromDomain(street))
    }

    override suspend fun updateStreet(street: Street) {
        streetDao.updateStreet(StreetEntity.fromDomain(street))
    }

    override suspend fun deleteStreet(street: Street) {
        streetDao.deleteStreet(StreetEntity.fromDomain(street))
    }

    override suspend fun getPendingStreets(): List<Street> {
        return streetDao.getPendingStreets().map { it.toDomain() }
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        streetDao.updateSyncStatus(id, status)
    }
}
