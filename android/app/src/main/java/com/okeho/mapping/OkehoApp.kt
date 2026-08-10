package com.okeho.mapping

import android.app.Application
import com.okeho.mapping.data.local.OkehoDatabase
import com.okeho.mapping.data.remote.SupabaseClient
import com.okeho.mapping.di.SyncHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OkehoApp : Application() {

    @Inject
    lateinit var database: OkehoDatabase

    override fun onCreate() {
        super.onCreate()
        SupabaseClient.initialize(Config.SUPABASE_URL, Config.SUPABASE_ANON_KEY)
        SyncHelper.init(database)
    }
}
