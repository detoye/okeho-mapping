package com.okeho.mapping

import android.app.Application
import com.okeho.mapping.data.remote.SupabaseClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OkehoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.initialize(Config.SUPABASE_URL, Config.SUPABASE_ANON_KEY)
    }
}
