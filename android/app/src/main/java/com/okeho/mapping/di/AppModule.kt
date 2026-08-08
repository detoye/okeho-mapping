package com.okeho.mapping.di

import android.content.Context
import androidx.room.Room
import com.okeho.mapping.data.local.OkehoDatabase
import com.okeho.mapping.data.local.dao.CaptureDao
import com.okeho.mapping.data.local.dao.StreetDao
import com.okeho.mapping.data.repository.CaptureRepositoryImpl
import com.okeho.mapping.data.repository.StreetRepositoryImpl
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OkehoDatabase {
        return Room.databaseBuilder(
            context,
            OkehoDatabase::class.java,
            "okeho_database"
        ).build()
    }

    @Provides
    fun provideCaptureDao(database: OkehoDatabase): CaptureDao {
        return database.captureDao()
    }

    @Provides
    fun provideStreetDao(database: OkehoDatabase): StreetDao {
        return database.streetDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository

    @Binds
    @Singleton
    abstract fun bindStreetRepository(impl: StreetRepositoryImpl): StreetRepository
}
