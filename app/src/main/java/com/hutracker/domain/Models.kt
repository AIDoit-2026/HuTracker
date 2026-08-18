package com.hutracker.domain

import java.util.UUID

enum class ScoringMode(val label: String) {
    EQUAL("等额"),
    MANUAL("非等额"),
}

enum class GameStatus(val label: String) {
    ACTIVE("记录中"),
    FINISHED("已结算"),
}

enum class SeatDirection(val label: String) {
    EAST("东"),
    SOUTH("南"),
    WEST("西"),
    NORTH("北"),
}

data class Game(
    val id: String = UUID.randomUUID().toString(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
    val scoringMode: ScoringMode,
    val status: GameStatus = GameStatus.ACTIVE,
    val currentRoundIndex: Int = 0,
    val currentDealerIndex: Int = 0,
)

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val gameId: String,
    val name: String,
    val seatIndex: Int,
) {
    val seat: SeatDirection
        get() = SeatDirection.entries[seatIndex]
}

data class GameRecord(
    val game: Game,
    val players: List<Player>,
    val entries: List<ScoreEntry> = emptyList(),
)

data class ScoreEntry(
    val id: String = UUID.randomUUID().toString(),
    val gameId: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val roundIndex: Int,
    val dealerPlayerId: String,
    val equalWinnerPlayerId: String? = null,
    val equalBaseScore: Int? = null,
    val manualDeltas: List<PlayerDelta> = emptyList(),
    val note: String = "",
)

data class PlayerDelta(
    val playerId: String,
    val delta: Int,
) {
    val isWin: Boolean
        get() = delta > 0
}

data class PlayerSummary(
    val player: Player,
    val netScore: Int,
    val recordedWinTotal: Int,
    val winCount: Int,
) {
    val averageWinScore: Double
        get() = if (winCount == 0) 0.0 else netScore.toDouble() / winCount
}

data class SettlementLine(
    val fromPlayer: Player,
    val toPlayer: Player,
    val amount: Int,
)

data class RoundContext(
    val roundIndex: Int,
    val dealerIndex: Int,
) {
    val roundLabel: String
        get() = "${SeatDirection.entries[roundIndex].label}风圈"
}
