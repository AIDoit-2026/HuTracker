package com.hutracker.domain

object RoundTracker {
    fun currentContext(entryCount: Int, playerCount: Int): RoundContext {
        require(playerCount in 2..4) { "playerCount must be 2..4" }
        val roundIndex = (entryCount / playerCount) % SeatDirection.entries.size
        val dealerIndex = entryCount % playerCount
        return RoundContext(roundIndex = roundIndex, dealerIndex = dealerIndex)
    }

    fun rebuildEntryContexts(record: GameRecord): GameRecord {
        val rebuiltEntries = record.entries.mapIndexed { index, entry ->
            val context = currentContext(index, record.players.size)
            entry.copy(
                roundIndex = context.roundIndex,
                dealerPlayerId = record.players[context.dealerIndex].id,
            )
        }
        val current = currentContext(rebuiltEntries.size, record.players.size)
        return record.copy(
            game = record.game.copy(
                currentRoundIndex = current.roundIndex,
                currentDealerIndex = current.dealerIndex,
                updatedAtMillis = System.currentTimeMillis(),
            ),
            entries = rebuiltEntries,
        )
    }
}
