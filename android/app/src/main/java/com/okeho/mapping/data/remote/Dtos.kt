package com.okeho.mapping.data.remote

import kotlinx.serialization.Serializable

/**
 * Postgres stores position in a PostGIS `geometry` column, not lat/lng columns,
 * so [geometry] is sent as EWKT: "SRID=4326;POINT(lng lat)". Note the WKT axis
 * order is X Y — longitude first.
 *
 * [sync_status] deliberately has no default: supabase-kt serializes with
 * encodeDefaults = false, so a defaulted property is omitted from the request
 * and the column's own 'pending' default wins — leaving rows that did sync
 * labelled as though they hadn't.
 */
@Serializable
data class CaptureDto(
    val id: String,
    val user_id: String? = null,
    val name: String,
    val feature_type: String,
    val geometry: String,
    val accuracy: Float,
    val photo_url: String? = null,
    val ocr_text: String? = null,
    val sync_status: String
)

@Serializable
data class StreetDto(
    val id: String,
    val user_id: String? = null,
    val name: String,
    val geometry: String,
    val surface_type: String,
    val traffic_direction: String,
    val points_captured: Int,
    val sync_status: String
)
