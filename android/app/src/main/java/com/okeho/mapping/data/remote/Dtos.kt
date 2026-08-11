package com.okeho.mapping.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

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

/**
 * Read-side DTOs, separate from the write DTOs above because the column is
 * asymmetric: PostgREST accepts EWKT *text* for a PostGIS `geometry` column
 * but returns it as a GeoJSON *object*. One shared type cannot describe both.
 *
 * [coordinates] stays a raw [JsonElement] because its shape depends on
 * [type]: a Point is `[lng, lat]`, a LineString is `[[lng, lat], ...]`.
 * Deserializing straight into a typed field would fail for one or the other.
 */
@Serializable
data class GeoJson(
    val type: String,
    val coordinates: JsonElement
)

/** GeoJSON is lng-first. The rest of the app is lat-first, so these swap. */
fun GeoJson.asLatLng(): Pair<Double, Double>? {
    val pair = (coordinates as? JsonArray)?.takeIf { it.size >= 2 } ?: return null
    val lng = pair[0].jsonPrimitive.doubleOrNull ?: return null
    val lat = pair[1].jsonPrimitive.doubleOrNull ?: return null
    return lat to lng
}

fun GeoJson.asLatLngList(): List<Pair<Double, Double>> =
    (coordinates as? JsonArray).orEmpty().mapNotNull { point ->
        val pair = (point as? JsonArray)?.takeIf { it.size >= 2 } ?: return@mapNotNull null
        val lng = pair[0].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
        val lat = pair[1].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
        lat to lng
    }

@Serializable
data class CaptureRow(
    val id: String,
    val name: String = "",
    val feature_type: String = "other",
    val geometry: GeoJson? = null,
    val accuracy: Float = 0f,
    val photo_url: String? = null,
    val ocr_text: String? = null,
    val created_at: String? = null
)

@Serializable
data class StreetRow(
    val id: String,
    val name: String = "",
    val geometry: GeoJson? = null,
    val surface_type: String = "paved",
    val traffic_direction: String = "two_way",
    val created_at: String? = null
)
