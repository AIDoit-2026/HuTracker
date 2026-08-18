package com.hutracker.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY updatedAtMillis DESC")
    fun listGames(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGame(gameId: String): GameEntity?

    @Query("SELECT * FROM players WHERE gameId = :gameId ORDER BY seatIndex")
    fun getPlayers(gameId: String): List<PlayerEntity>

    @Query("SELECT * FROM score_entries WHERE gameId = :gameId ORDER BY createdAtMillis")
    fun getEntries(gameId: String): List<ScoreEntryEntity>

    @Query("SELECT * FROM score_deltas WHERE entryId IN (:entryIds)")
    fun getDeltas(entryIds: List<String>): List<ScoreDeltaEntity>

    @Upsert fun upsertGame(game: GameEntity)
    @Upsert fun upsertPlayers(players: List<PlayerEntity>)
    @Upsert fun upsertEntry(entry: ScoreEntryEntity)
    @Upsert fun upsertDeltas(deltas: List<ScoreDeltaEntity>)

    @Query("DELETE FROM score_deltas WHERE entryId = :entryId")
    fun deleteDeltas(entryId: String)

    @Transaction
    fun deleteEntry(entryId: String) {
        deleteDeltas(entryId)
        deleteEntryRow(entryId)
    }

    @Query("DELETE FROM score_entries WHERE id = :entryId")
    fun deleteEntryRow(entryId: String)

    @Transaction
    fun replaceEntry(entry: ScoreEntryEntity, deltas: List<ScoreDeltaEntity>) {
        deleteDeltas(entry.id)
        upsertEntry(entry)
        if (deltas.isNotEmpty()) upsertDeltas(deltas)
    }
}
