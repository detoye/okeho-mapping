package com.okeho.mapping.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.okeho.mapping.data.local.dao.CaptureDao
import com.okeho.mapping.data.local.dao.StreetDao
import com.okeho.mapping.data.local.entity.CaptureEntity
import com.okeho.mapping.data.local.entity.StreetEntity

@Database(
    entities = [CaptureEntity::class, StreetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OkehoDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun streetDao(): StreetDao
}
