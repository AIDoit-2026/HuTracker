package com.hutracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val scoringMode: String,
    val status: String,
    val currentRoundIndex: Int,
    val currentDealerIndex: Int,
)

@Entity(tableName = "players", primaryKeys = ["id"])
data class PlayerEntity(
    val id: String,
    val gameId: String,
    val name: String,
    val seatIndex: Int,
)

@Entity(tableName = "score_entries", primaryKeys = ["id"])
data class ScoreEntryEntity(
    val id: String,
    val gameId: String,
    val createdAtMillis: Long,
    val roundIndex: Int,
    val dealerPlayerId: String,
    val equalWinnerPlayerId: String?,
    val equalBaseScore: Int?,
    val note: String,
)

@Entity(tableName = "score_deltas", primaryKeys = ["entryId", "playerId"])
data class ScoreDeltaEntity(
    val entryId: String,
    val playerId: String,
    val delta: Int,
)
