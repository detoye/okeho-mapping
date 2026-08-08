package com.okeho.mapping.domain.model

import java.util.UUID

data class Capture(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val featureType: FeatureType = FeatureType.OTHER,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val photoUrl: String? = null,
    val ocrText: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
