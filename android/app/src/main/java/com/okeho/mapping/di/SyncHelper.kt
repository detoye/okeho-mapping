package com.okeho.mapping.di

import com.okeho.mapping.data.local.OkehoDatabase
import com.okeho.mapping.data.repository.CaptureRepositoryImpl
import com.okeho.mapping.data.repository.StreetRepositoryImpl
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository

object SyncHelper {
    private var database: OkehoDatabase? = null

    val captureRepository: CaptureRepository by lazy {
        CaptureRepositoryImpl(database!!.captureDao())
    }

    val streetRepository: StreetRepository by lazy {
        StreetRepositoryImpl(database!!.streetDao())
    }

    fun init(db: OkehoDatabase) {
        database = db
    }
}
