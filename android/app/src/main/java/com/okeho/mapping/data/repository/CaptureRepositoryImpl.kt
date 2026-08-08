package com.okeho.mapping.data.repository

import com.okeho.mapping.data.local.dao.CaptureDao
import com.okeho.mapping.data.local.entity.CaptureEntity
import com.okeho.mapping.domain.model.Capture
import com.okeho.mapping.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor(
    private val captureDao: CaptureDao
) : CaptureRepository {

    override fun getAllCaptures(): Flow<List<Capture>> {
        return captureDao.getAllCaptures().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCaptureById(id: String): Capture? {
        return captureDao.getCaptureById(id)?.toDomain()
    }

    override suspend fun insertCapture(capture: Capture) {
        captureDao.insertCapture(CaptureEntity.fromDomain(capture))
    }

    override suspend fun updateCapture(capture: Capture) {
        captureDao.updateCapture(CaptureEntity.fromDomain(capture))
    }

    override suspend fun deleteCapture(capture: Capture) {
        captureDao.deleteCapture(CaptureEntity.fromDomain(capture))
    }

    override suspend fun getPendingCaptures(): List<Capture> {
        return captureDao.getPendingCaptures().map { it.toDomain() }
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        captureDao.updateSyncStatus(id, status)
    }
}
