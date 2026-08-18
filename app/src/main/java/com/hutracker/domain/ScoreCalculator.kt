package com.hutracker.domain

object ScoreCalculator {
    fun summaries(record: GameRecord): List<PlayerSummary> {
        return when (record.game.scoringMode) {
            ScoringMode.EQUAL -> equalSummaries(record)
            ScoringMode.MANUAL -> manualSummaries(record)
        }
    }

    fun settlementLines(record: GameRecord): List<SettlementLine> {
        val summariesByPlayer = summaries(record).associateBy { it.player.id }
        val creditors = record.players
            .mapNotNull { player ->
                val score = summariesByPlayer.getValue(player.id).netScore
                if (score > 0) Balance(player, score) else null
            }
            .sortedByDescending { it.remaining }
            .toMutableList()
        val debtors = record.players
            .mapNotNull { player ->
                val score = summariesByPlayer.getValue(player.id).netScore
                if (score < 0) Balance(player, -score) else null
            }
            .sortedByDescending { it.remaining }
            .toMutableList()

        val lines = mutableListOf<SettlementLine>()
        var debtorIndex = 0
        var creditorIndex = 0
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amount = minOf(debtor.remaining, creditor.remaining)
            if (amount > 0) {
                lines += SettlementLine(debtor.player, creditor.player, amount)
                debtors[debtorIndex] = debtor.copy(remaining = debtor.remaining - amount)
                creditors[creditorIndex] = creditor.copy(remaining = creditor.remaining - amount)
            }
            if (debtors[debtorIndex].remaining == 0) debtorIndex++
            if (creditors[creditorIndex].remaining == 0) creditorIndex++
        }
        return lines
    }

    fun validateEqualEntry(record: GameRecord, winnerPlayerId: String, baseScore: Int): Result<Unit> {
        if (record.game.status == GameStatus.FINISHED) {
            return Result.failure(IllegalStateException("已结算牌局不能修改记录"))
        }
        if (winnerPlayerId !in record.players.map { it.id }) {
            return Result.failure(IllegalArgumentException("请选择得分人"))
        }
        if (baseScore <= 0) {
            return Result.failure(IllegalArgumentException("分数必须大于 0"))
        }
        return Result.success(Unit)
    }

    fun validateManualEntry(record: GameRecord, deltas: List<PlayerDelta>): Result<Unit> {
        if (record.game.status == GameStatus.FINISHED) {
            return Result.failure(IllegalStateException("已结算牌局不能修改记录"))
        }
        if (deltas.map { it.playerId }.toSet() != record.players.map { it.id }.toSet()) {
            return Result.failure(IllegalArgumentException("需要为每位玩家输入分数"))
        }
        if (deltas.sumOf { it.delta } != 0) {
            return Result.failure(IllegalArgumentException("正分和负分合计必须为 0"))
        }
        if (deltas.none { it.delta > 0 }) {
            return Result.failure(IllegalArgumentException("至少需要一个正分"))
        }
        return Result.success(Unit)
    }

    private fun equalSummaries(record: GameRecord): List<PlayerSummary> {
        val playerCount = record.players.size
        val totalBaseScore = record.entries.sumOf { it.equalBaseScore ?: 0 }
        val winTotals = mutableMapOf<String, Int>().withDefault { 0 }
        val winCounts = mutableMapOf<String, Int>().withDefault { 0 }

        for (entry in record.entries) {
            val winnerId = entry.equalWinnerPlayerId ?: continue
            val baseScore = entry.equalBaseScore ?: 0
            winTotals[winnerId] = winTotals.getValue(winnerId) + baseScore
            winCounts[winnerId] = winCounts.getValue(winnerId) + 1
        }

        return record.players.map { player ->
            val recordedWinTotal = winTotals.getValue(player.id)
            PlayerSummary(
                player = player,
                netScore = recordedWinTotal * playerCount - totalBaseScore,
                recordedWinTotal = recordedWinTotal,
                winCount = winCounts.getValue(player.id),
            )
        }
    }

    private fun manualSummaries(record: GameRecord): List<PlayerSummary> {
        val netScores = mutableMapOf<String, Int>().withDefault { 0 }
        val recordedWinTotals = mutableMapOf<String, Int>().withDefault { 0 }
        val winCounts = mutableMapOf<String, Int>().withDefault { 0 }

        for (entry in record.entries) {
            for (delta in entry.manualDeltas) {
                netScores[delta.playerId] = netScores.getValue(delta.playerId) + delta.delta
                if (delta.isWin) {
                    recordedWinTotals[delta.playerId] =
                        recordedWinTotals.getValue(delta.playerId) + delta.delta
                    winCounts[delta.playerId] = winCounts.getValue(delta.playerId) + 1
                }
            }
        }

        return record.players.map { player ->
            PlayerSummary(
                player = player,
                netScore = netScores.getValue(player.id),
                recordedWinTotal = recordedWinTotals.getValue(player.id),
                winCount = winCounts.getValue(player.id),
            )
        }
    }

    private data class Balance(
        val player: Player,
        val remaining: Int,
    )
}
