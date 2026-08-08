package com.okeho.mapping.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    private var client: SupabaseClient? = null

    fun initialize(url: String, anonKey: String) {
        client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException("SupabaseClient not initialized. Call initialize() first.")
    }
}
