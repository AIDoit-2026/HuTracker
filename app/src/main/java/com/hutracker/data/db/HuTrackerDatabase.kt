package com.hutracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, PlayerEntity::class, ScoreEntryEntity::class, ScoreDeltaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HuTrackerDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
