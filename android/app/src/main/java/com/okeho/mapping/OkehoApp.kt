package com.okeho.mapping

import android.app.Application
import com.okeho.mapping.data.remote.RecordRestorer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class OkehoApp : Application() {

    @Inject lateinit var recordRestorer: RecordRestorer

    override fun onCreate() {
        super.onCreate()
        // The Supabase client is a Hilt singleton (di/SupabaseModule.kt), and
        // session restore happens inside it, observable via
        // AuthManager.sessionStatus.
        //
        // Record restore is started here rather than from a ViewModel because
        // the login, signup, and gate composables each resolve their own
        // ViewModel instance -- an observer there fires up to three times per
        // sign-in. This scope outlives every screen, so it collects once.
        recordRestorer.start(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}
