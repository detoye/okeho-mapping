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
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
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
        // KotlinXSerializer defaults to the strict `Json`, not supabase's own
        // lenient instance, so any key the DTOs don't declare is fatal. PostGIS
        // geometry arrives as GeoJSON carrying a `crs` object, which killed
        // every restore. Decoding must tolerate columns the app doesn't model.
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            }
        )
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
