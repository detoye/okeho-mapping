package com.okeho.mapping.domain.model

import java.util.UUID

data class Street(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val points: List<Pair<Double, Double>> = emptyList(),
    val surfaceType: SurfaceType = SurfaceType.PAVED,
    val trafficDirection: TrafficDirection = TrafficDirection.TWO_WAY,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SurfaceType(val displayName: String) {
    PAVED("Paved"),
    UNPAVED("Unpaved")
}

enum class TrafficDirection(val displayName: String) {
    ONE_WAY("One-way"),
    TWO_WAY("Two-way")
}
