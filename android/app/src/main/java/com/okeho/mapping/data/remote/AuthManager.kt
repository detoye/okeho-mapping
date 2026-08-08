package com.okeho.mapping.data.remote

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthManager {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            SupabaseClient.getClient().auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            _isAuthenticated.value = true
            _currentUserEmail.value = email
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            SupabaseClient.getClient().auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = mapOf("full_name" to fullName)
            }
            _isAuthenticated.value = true
            _currentUserEmail.value = email
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        SupabaseClient.getClient().auth.signOut()
        _isAuthenticated.value = false
        _currentUserEmail.value = null
    }

    suspend fun getCurrentUser(): String? {
        return try {
            val session = SupabaseClient.getClient().auth.currentSessionOrNull()
            session?.user?.id
        } catch (e: Exception) {
            null
        }
    }
}
