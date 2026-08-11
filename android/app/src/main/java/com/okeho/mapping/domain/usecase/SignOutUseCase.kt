package com.okeho.mapping.domain.usecase

import com.okeho.mapping.data.local.OkehoDatabase
import com.okeho.mapping.data.remote.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signs out and wipes the local database.
 *
 * The order is the point of this class. The wipe runs first: if it ran after
 * sign-out and failed, the next user on a shared device would inherit the
 * previous user's records. Sign-out is now offered from two places, and that
 * invariant is not something to re-derive at each call site.
 *
 * The auth gate in MainActivity reacts to sessionStatus, so callers do not
 * navigate afterwards.
 */
@Singleton
class SignOutUseCase @Inject constructor(
    private val authManager: AuthManager,
    private val db: OkehoDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        db.clearAllTables()
        return authManager.signOut()
    }
}
