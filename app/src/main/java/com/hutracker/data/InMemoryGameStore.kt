package com.hutracker.data

import com.hutracker.domain.Game
import com.hutracker.domain.GameRecord
import com.hutracker.domain.GameStatus
import com.hutracker.domain.Player
import com.hutracker.domain.PlayerDelta
import com.hutracker.domain.RoundTracker
import com.hutracker.domain.ScoreCalculator
import com.hutracker.domain.ScoreEntry
import com.hutracker.domain.ScoringMode

class InMemoryGameStore {
    private val records = linkedMapOf<String, GameRecord>()

    fun listGames(): List<GameRecord> = records.values.toList().sortedByDescending { it.game.updatedAtMillis }

    fun getGame(gameId: String): GameRecord? = records[gameId]

    fun createGame(playerNames: List<String>, scoringMode: ScoringMode): GameRecord {
        require(playerNames.size in 2..4) { "需要 2 到 4 名玩家" }
        val trimmedNames = playerNames.map { it.trim() }
        require(trimmedNames.all { it.isNotBlank() }) { "玩家名称不能为空" }

        val game = Game(scoringMode = scoringMode)
        val players = trimmedNames.mapIndexed { index, name ->
            Player(gameId = game.id, name = name, seatIndex = index)
        }
        val record = GameRecord(game = game, players = players)
        records[game.id] = record
        return record
    }

    fun addEqualEntry(gameId: String, winnerPlayerId: String, baseScore: Int, note: String = "") {
        val record = records.getValue(gameId)
        ScoreCalculator.validateEqualEntry(record, winnerPlayerId, baseScore).getOrThrow()
        val context = RoundTracker.currentContext(record.entries.size, record.players.size)
        val entry = ScoreEntry(
            gameId = gameId,
            roundIndex = context.roundIndex,
            dealerPlayerId = record.players[context.dealerIndex].id,
            equalWinnerPlayerId = winnerPlayerId,
            equalBaseScore = baseScore,
            note = note.trim(),
        )
        replaceRecord(record.copy(entries = record.entries + entry))
    }

    fun addManualEntry(gameId: String, deltas: List<PlayerDelta>, note: String = "") {
        val record = records.getValue(gameId)
        ScoreCalculator.validateManualEntry(record, deltas).getOrThrow()
        val context = RoundTracker.currentContext(record.entries.size, record.players.size)
        val entry = ScoreEntry(
            gameId = gameId,
            roundIndex = context.roundIndex,
            dealerPlayerId = record.players[context.dealerIndex].id,
            manualDeltas = deltas,
            note = note.trim(),
        )
        replaceRecord(record.copy(entries = record.entries + entry))
    }

    fun updateEqualEntry(gameId: String, entryId: String, winnerPlayerId: String, baseScore: Int, note: String = "") {
        val record = records.getValue(gameId)
        ScoreCalculator.validateEqualEntry(record, winnerPlayerId, baseScore).getOrThrow()
        val entries = record.entries.map { entry ->
            if (entry.id == entryId) {
                entry.copy(
                    equalWinnerPlayerId = winnerPlayerId,
                    equalBaseScore = baseScore,
                    manualDeltas = emptyList(),
                    note = note.trim(),
                )
            } else {
                entry
            }
        }
        replaceRecord(record.copy(entries = entries))
    }

    fun updateManualEntry(gameId: String, entryId: String, deltas: List<PlayerDelta>, note: String = "") {
        val record = records.getValue(gameId)
        ScoreCalculator.validateManualEntry(record, deltas).getOrThrow()
        val entries = record.entries.map { entry ->
            if (entry.id == entryId) {
                entry.copy(
                    equalWinnerPlayerId = null,
                    equalBaseScore = null,
                    manualDeltas = deltas,
                    note = note.trim(),
                )
            } else {
                entry
            }
        }
        replaceRecord(record.copy(entries = entries))
    }

    fun deleteEntry(gameId: String, entryId: String) {
        val record = records.getValue(gameId)
        ensureEditable(record)
        replaceRecord(record.copy(entries = record.entries.filterNot { it.id == entryId }))
    }

    fun confirmSettlement(gameId: String) {
        val record = records.getValue(gameId)
        val settledGame = record.game.copy(status = GameStatus.FINISHED, updatedAtMillis = System.currentTimeMillis())
        records[gameId] = record.copy(game = settledGame)
    }

    private fun replaceRecord(record: GameRecord) {
        records[record.game.id] = RoundTracker.rebuildEntryContexts(record)
    }

    private fun ensureEditable(record: GameRecord) {
        check(record.game.status == GameStatus.ACTIVE) { "已结算牌局不能修改记录" }
    }
}
