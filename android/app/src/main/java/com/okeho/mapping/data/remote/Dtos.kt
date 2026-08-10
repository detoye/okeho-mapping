package com.okeho.mapping.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CaptureDto(
    val id: String,
    val user_id: String,
    val name: String,
    val feature_type: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val photo_url: String? = null,
    val ocr_text: String? = null,
    val sync_status: String = "synced"
)

@Serializable
data class StreetDto(
    val id: String,
    val user_id: String,
    val name: String,
    val surface_type: String,
    val traffic_direction: String,
    val points_captured: Int,
    val sync_status: String = "synced"
)
