package com.okeho.mapping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.okeho.mapping.domain.model.Street
import com.okeho.mapping.domain.model.SurfaceType
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.model.TrafficDirection

@Entity(tableName = "streets")
data class StreetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val pointsJson: String,
    val surfaceType: String,
    val trafficDirection: String,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = Street(
        id = id,
        userId = userId,
        name = name,
        points = parsePoints(pointsJson),
        surfaceType = SurfaceType.valueOf(surfaceType),
        trafficDirection = TrafficDirection.valueOf(trafficDirection),
        syncStatus = SyncStatus.valueOf(syncStatus),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(street: Street) = StreetEntity(
            id = street.id,
            userId = street.userId,
            name = street.name,
            pointsJson = serializePoints(street.points),
            surfaceType = street.surfaceType.name,
            trafficDirection = street.trafficDirection.name,
            syncStatus = street.syncStatus.name,
            createdAt = street.createdAt,
            updatedAt = street.updatedAt
        )

        private fun parsePoints(json: String): List<Pair<Double, Double>> {
            if (json.isBlank()) return emptyList()
            return json.split(";").map { point ->
                val (lat, lng) = point.split(",").map { it.trim().toDouble() }
                Pair(lat, lng)
            }
        }

        private fun serializePoints(points: List<Pair<Double, Double>>): String {
            return points.joinToString(";") { "${it.first},${it.second}" }
        }
    }
}
