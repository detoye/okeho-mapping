package com.okeho.mapping.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads capture photos to the private `photos` bucket.
 *
 * Before this existed, sync sent the device-local `content://` URI straight to
 * the `photo_url` column, so every synced photo was a path only the capturing
 * device could resolve -- and only until it forgot the URI permission grant.
 *
 * What lands in `photo_url` is the storage object path, not a URL. The bucket
 * is private, so the only readable link is a signed URL, and those expire --
 * persisting one would write a value that is broken by the time anyone reads
 * it. Consumers create a signed URL from this path on demand.
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    /**
     * Returns the object path on success, or null if the photo could not be
     * read or uploaded. Callers treat null as "no photo on the server".
     *
     * The path is deterministic -- `<userId>/<captureId>.jpg` -- so re-syncing
     * a FAILED capture overwrites in place instead of leaving an orphaned
     * duplicate behind. That upsert is what migration 005's UPDATE policy
     * exists to permit. The leading `<userId>/` segment is also what the
     * storage RLS policies match on, so it is required, not cosmetic.
     */
    suspend fun upload(localUri: String, captureId: String, userId: String): String? {
        val path = "$userId/$captureId.jpg"
        return try {
            val bytes = context.contentResolver.openInputStream(Uri.parse(localUri))
                ?.use { it.readBytes() }
                ?: run {
                    Log.w(TAG, "No stream for $localUri (permission grant likely expired)")
                    return null
                }
            client.storage.from(BUCKET).upload(path, bytes, upsert = true)
            Log.d(TAG, "Uploaded $path (${bytes.size} bytes)")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for $path", e)
            null
        }
    }

    private companion object {
        const val BUCKET = "photos"
        const val TAG = "PhotoUploader"
    }
}
