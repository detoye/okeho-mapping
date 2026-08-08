package com.okeho.mapping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.okeho.mapping.domain.model.Capture
import com.okeho.mapping.domain.model.FeatureType
import com.okeho.mapping.domain.model.SyncStatus

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val featureType: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val photoUrl: String?,
    val ocrText: String?,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = Capture(
        id = id,
        userId = userId,
        name = name,
        featureType = FeatureType.fromString(featureType),
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        photoUrl = photoUrl,
        ocrText = ocrText,
        syncStatus = SyncStatus.valueOf(syncStatus),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(capture: Capture) = CaptureEntity(
            id = capture.id,
            userId = capture.userId,
            name = capture.name,
            featureType = capture.featureType.name,
            latitude = capture.latitude,
            longitude = capture.longitude,
            accuracy = capture.accuracy,
            photoUrl = capture.photoUrl,
            ocrText = capture.ocrText,
            syncStatus = capture.syncStatus.name,
            createdAt = capture.createdAt,
            updatedAt = capture.updatedAt
        )
    }
}
