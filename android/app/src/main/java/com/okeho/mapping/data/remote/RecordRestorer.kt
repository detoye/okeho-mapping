package com.okeho.mapping.data.remote

import android.util.Log
import com.okeho.mapping.data.local.dao.CaptureDao
import com.okeho.mapping.data.local.dao.StreetDao
import com.okeho.mapping.data.local.entity.CaptureEntity
import com.okeho.mapping.data.local.entity.StreetEntity
import com.okeho.mapping.domain.model.FeatureType
import com.okeho.mapping.domain.model.SurfaceType
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.model.TrafficDirection
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls the signed-in user's records down from Supabase into Room.
 *
 * This is what makes records survive a reinstall: Room is wiped on uninstall,
 * but the rows live on the server, and this restores them the next time the
 * account signs in. Without it, logging in after a reinstall would show an
 * empty Records screen and the account would look broken.
 *
 * Restored rows are written as SYNCED so they are not re-uploaded, and they
 * are written before the user's locally-captured-but-unsynced rows so any
 * id collision resolves in favor of the local copy. `photo_url` keeps the
 * object path from the server; the local URI on the capturing device is
 * already stored in its own local row.
 */
@Singleton
class RecordRestorer @Inject constructor(
    private val client: SupabaseClient,
    private val captureDao: CaptureDao,
    private val streetDao: StreetDao
) {
    /**
     * Watches the session and restores once per sign-in.
     *
     * This lives in the singleton rather than a ViewModel deliberately: the
     * login, signup, and gate composables each resolve their own ViewModel
     * instance, so an init-block observer there would run the restore up to
     * three times for one sign-in. A singleton collecting the flow once, with
     * [restoredFor] to ignore re-emissions of the same session, runs it once.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                val userId = (status as? SessionStatus.Authenticated)?.session?.user?.id
                when {
                    userId == null -> restoredFor = null
                    userId != restoredFor -> {
                        restoredFor = userId
                        restoreFor(userId)
                    }
                }
            }
        }
    }

    private var restoredFor: String? = null

    suspend fun restoreFor(userId: String) = withContext(Dispatchers.IO) {
        try {
            restoreCaptures(userId)
            restoreStreets(userId)
            Log.d(TAG, "Restore complete for $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
        }
    }

    private suspend fun restoreCaptures(userId: String) {
        val rows = client.from(CAPTURES).select(Columns.raw("id,name,feature_type,geometry,accuracy,photo_url,ocr_text,created_at")) {
            filter { eq("user_id", userId) }
        }.decodeList<CaptureRow>()

        // Inserts use REPLACE, so a server row would overwrite a local one with
        // the same id. Anything still PENDING or FAILED holds work that has not
        // reached the server, so those ids are skipped rather than clobbered.
        val unsynced = captureDao.getPendingCaptures().map { it.id }.toSet()

        val now = System.currentTimeMillis()
        val entities = rows.mapNotNull { row ->
            if (row.id in unsynced) return@mapNotNull null
            val (lat, lng) = row.geometry?.asLatLng() ?: return@mapNotNull null
            CaptureEntity(
                id = row.id,
                userId = userId,
                name = row.name,
                featureType = FeatureType.fromString(row.feature_type).name,
                latitude = lat,
                longitude = lng,
                accuracy = row.accuracy,
                photoUrl = row.photo_url,
                ocrText = row.ocr_text,
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = row.created_at?.let(::parseTimestamp) ?: now,
                updatedAt = now
            )
        }
        if (entities.isNotEmpty()) captureDao.insertCaptures(entities)
        Log.d(TAG, "Restored ${entities.size} captures (${unsynced.size} local pending kept)")
    }

    private suspend fun restoreStreets(userId: String) {
        val rows = client.from(STREETS).select(Columns.raw("id,name,geometry,surface_type,traffic_direction,created_at")) {
            filter { eq("user_id", userId) }
        }.decodeList<StreetRow>()

        val unsynced = streetDao.getPendingStreets().map { it.id }.toSet()

        val now = System.currentTimeMillis()
        val entities = rows.mapNotNull { row ->
            if (row.id in unsynced) return@mapNotNull null
            val points = row.geometry?.asLatLngList()?.takeIf { it.size >= 2 }
                ?: return@mapNotNull null
            StreetEntity(
                id = row.id,
                userId = userId,
                name = row.name,
                pointsJson = points.joinToString(";") { "${it.first},${it.second}" },
                surfaceType = surfaceOf(row.surface_type),
                trafficDirection = trafficOf(row.traffic_direction),
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = row.created_at?.let(::parseTimestamp) ?: now,
                updatedAt = now
            )
        }
        if (entities.isNotEmpty()) streetDao.insertStreets(entities)
        Log.d(TAG, "Restored ${entities.size} streets (${unsynced.size} local pending kept)")
    }

    private fun surfaceOf(value: String): String =
        (SurfaceType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: SurfaceType.PAVED).name

    private fun trafficOf(value: String): String =
        (TrafficDirection.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: TrafficDirection.TWO_WAY).name

    /**
     * Server timestamps look like `2026-08-10T21:31:02.283375+00:00`; the app
     * stores epoch millis. `java.time` is unavailable -- minSdk is 24 with no
     * core library desugaring -- so this uses SimpleDateFormat, which parses
     * the pattern and ignores the trailing fraction and offset. The zone must
     * be set explicitly: the default would read a UTC value as device-local
     * and shift every restored record's ordering by the device's offset.
     *
     * Built per call rather than shared, because SimpleDateFormat is mutable
     * and not thread-safe.
     */
    private fun parseTimestamp(value: String): Long =
        try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(value)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

    private companion object {
        const val CAPTURES = "captures"
        const val STREETS = "streets"
        const val TAG = "RecordRestorer"
    }
}
