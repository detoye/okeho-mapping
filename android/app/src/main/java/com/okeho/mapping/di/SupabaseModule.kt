package com.okeho.mapping.di

import com.okeho.mapping.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_ANON_KEY
    ) {
        // Session survives process death via the default SettingsSessionManager
        // (SharedPreferences). The app's "am I logged in" state is derived from
        // auth.sessionStatus, so no separate flag needs persisting.
        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
        install(Storage)
        // Realtime is deliberately not installed: nothing subscribes to it, and
        // holding the websocket open is wasted state.
    }
}
