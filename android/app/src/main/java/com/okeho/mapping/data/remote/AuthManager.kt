package com.okeho.mapping.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over supabase-kt auth.
 *
 * [sessionStatus] is the library's own flow, not a hand-maintained copy. An
 * earlier version tracked a separate `isAuthenticated` boolean that started
 * false on every process start and was only ever set by signIn/signUp, so a
 * restored session was invisible to the app and the user appeared logged out
 * after every cold start.
 */
@Singleton
class AuthManager @Inject constructor(
    private val client: SupabaseClient
) {
    /** Includes LoadingFromStorage, so callers can wait out session restore. */
    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

    val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    val currentUserEmail: String?
        get() = client.auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Returns true when the account is immediately usable. False means the
     * project still requires email confirmation, so no session was issued and
     * the caller should say so rather than wait for a navigation that will
     * never happen.
     */
    suspend fun signUp(email: String, password: String, fullName: String): Result<Boolean> =
        runCatching {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            client.auth.currentSessionOrNull() != null
        }

    suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
    }

    /**
     * Retries loading the stored session after a [SessionStatus.NetworkError].
     * Returns true if a session was restored.
     */
    suspend fun reloadSession(): Boolean = runCatching {
        client.auth.loadFromStorage()
    }.getOrDefault(false)

    /**
     * Sends a password reset email. Supabase always reports success here,
     * whether or not the address has an account, so the UI must not treat the
     * result as confirmation that an account exists.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email)
    }
}
