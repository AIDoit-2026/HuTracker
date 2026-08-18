package com.hutracker.data

import android.content.Context
import androidx.room.Room
import com.hutracker.data.db.GameDao
import com.hutracker.data.db.GameEntity
import com.hutracker.data.db.HuTrackerDatabase
import com.hutracker.data.db.PlayerEntity
import com.hutracker.data.db.ScoreDeltaEntity
import com.hutracker.data.db.ScoreEntryEntity
import com.hutracker.domain.Game
import com.hutracker.domain.GameRecord
import com.hutracker.domain.GameStatus
import com.hutracker.domain.Player
import com.hutracker.domain.PlayerDelta
import com.hutracker.domain.RoundTracker
import com.hutracker.domain.ScoreCalculator
import com.hutracker.domain.ScoreEntry
import com.hutracker.domain.ScoringMode

class RoomGameStore(context: Context) {
    private val db = Room.databaseBuilder(context, HuTrackerDatabase::class.java, "hutracker.db")
        .allowMainThreadQueries()
        .build()
    private val dao: GameDao = db.gameDao()

    fun listGames(): List<GameRecord> = dao.listGames().mapNotNull { load(it.id) }
    fun getGame(gameId: String): GameRecord? = dao.getGame(gameId)?.let { load(it.id) }

    fun createGame(playerNames: List<String>, scoringMode: ScoringMode): GameRecord {
        require(playerNames.size in 2..4) { "需要 2 到 4 名玩家" }
        val names = playerNames.map { it.trim() }
        require(names.all { it.isNotBlank() }) { "玩家名称不能为空" }
        val game = Game(scoringMode = scoringMode)
        val players = names.mapIndexed { index, name -> Player(gameId = game.id, name = name, seatIndex = index) }
        save(GameRecord(game, players))
        return GameRecord(game, players)
    }

    fun addEqualEntry(gameId: String, winnerPlayerId: String, baseScore: Int, note: String = "") = mutate(gameId) {
        ScoreCalculator.validateEqualEntry(it, winnerPlayerId, baseScore).getOrThrow()
        val context = com.hutracker.domain.RoundTracker.currentContext(it.entries.size, it.players.size)
        it.copy(entries = it.entries + ScoreEntry(gameId = gameId, roundIndex = context.roundIndex, dealerPlayerId = it.players[context.dealerIndex].id, equalWinnerPlayerId = winnerPlayerId, equalBaseScore = baseScore, note = note.trim()))
    }

    fun addManualEntry(gameId: String, deltas: List<PlayerDelta>, note: String = "") = mutate(gameId) {
        ScoreCalculator.validateManualEntry(it, deltas).getOrThrow()
        val context = RoundTracker.currentContext(it.entries.size, it.players.size)
        it.copy(entries = it.entries + ScoreEntry(gameId = gameId, roundIndex = context.roundIndex, dealerPlayerId = it.players[context.dealerIndex].id, manualDeltas = deltas, note = note.trim()))
    }

    fun updateEqualEntry(gameId: String, entryId: String, winnerPlayerId: String, baseScore: Int, note: String = "") = mutate(gameId) {
        ScoreCalculator.validateEqualEntry(it, winnerPlayerId, baseScore).getOrThrow()
        it.copy(entries = it.entries.map { entry -> if (entry.id == entryId) entry.copy(equalWinnerPlayerId = winnerPlayerId, equalBaseScore = baseScore, manualDeltas = emptyList(), note = note.trim()) else entry })
    }

    fun updateManualEntry(gameId: String, entryId: String, deltas: List<PlayerDelta>, note: String = "") = mutate(gameId) {
        ScoreCalculator.validateManualEntry(it, deltas).getOrThrow()
        it.copy(entries = it.entries.map { entry -> if (entry.id == entryId) entry.copy(equalWinnerPlayerId = null, equalBaseScore = null, manualDeltas = deltas, note = note.trim()) else entry })
    }

    fun deleteEntry(gameId: String, entryId: String) = mutate(gameId) { it.copy(entries = it.entries.filterNot { entry -> entry.id == entryId }) }

    fun confirmSettlement(gameId: String) {
        val record = getGame(gameId) ?: error("牌局不存在")
        check(record.game.status == GameStatus.ACTIVE) { "牌局已经结算" }
        save(record.copy(game = record.game.copy(status = GameStatus.FINISHED, updatedAtMillis = System.currentTimeMillis())))
    }

    private fun mutate(gameId: String, transform: (GameRecord) -> GameRecord) {
        val record = getGame(gameId) ?: error("牌局不存在")
        check(record.game.status == GameStatus.ACTIVE) { "已结算牌局不能修改记录" }
        save(RoundTracker.rebuildEntryContexts(transform(record)))
    }

    private fun save(record: GameRecord) {
        dao.upsertGame(record.game.toEntity())
        dao.upsertPlayers(record.players.map { it.toEntity() })
        val existing = dao.getEntries(record.game.id).map { it.id }.toSet()
        record.entries.forEach { entry -> dao.replaceEntry(entry.toEntity(), entry.manualDeltas.map { ScoreDeltaEntity(entry.id, it.playerId, it.delta) }) }
        existing.filter { id -> record.entries.none { it.id == id } }.forEach { dao.deleteEntry(it) }
    }

    private fun load(gameId: String): GameRecord? {
        val game = dao.getGame(gameId) ?: return null
        val players = dao.getPlayers(gameId).map { Player(it.id, it.gameId, it.name, it.seatIndex) }
        val entries = dao.getEntries(gameId)
        val deltas = if (entries.isEmpty()) emptyList() else dao.getDeltas(entries.map { it.id })
        return GameRecord(game.toDomain(), players, entries.map { entity -> entity.toDomain(deltas.filter { it.entryId == entity.id }.map { PlayerDelta(it.playerId, it.delta) }) })
    }
}

private fun Game.toEntity() = GameEntity(id, createdAtMillis, updatedAtMillis, scoringMode.name, status.name, currentRoundIndex, currentDealerIndex)
private fun GameEntity.toDomain() = Game(id, createdAtMillis, updatedAtMillis, ScoringMode.valueOf(scoringMode), GameStatus.valueOf(status), currentRoundIndex, currentDealerIndex)
private fun Player.toEntity() = PlayerEntity(id, gameId, name, seatIndex)
private fun ScoreEntry.toEntity() = ScoreEntryEntity(id, gameId, createdAtMillis, roundIndex, dealerPlayerId, equalWinnerPlayerId, equalBaseScore, note)
private fun ScoreEntryEntity.toDomain(deltas: List<PlayerDelta>) = ScoreEntry(id, gameId, createdAtMillis, roundIndex, dealerPlayerId, equalWinnerPlayerId, equalBaseScore, deltas, note)
