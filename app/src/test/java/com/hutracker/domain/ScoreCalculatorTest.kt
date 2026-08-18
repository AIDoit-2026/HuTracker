package com.hutracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun equalModeDerivesImplicitLossesAtSettlement() {
        val record = sampleRecord(ScoringMode.EQUAL)
        val alice = record.players[0]
        val bob = record.players[1]
        val carol = record.players[2]
        val entries = listOf(
            ScoreEntry(gameId = record.game.id, roundIndex = 0, dealerPlayerId = alice.id, equalWinnerPlayerId = alice.id, equalBaseScore = 10),
            ScoreEntry(gameId = record.game.id, roundIndex = 0, dealerPlayerId = bob.id, equalWinnerPlayerId = bob.id, equalBaseScore = 20),
        )

        val summaries = ScoreCalculator.summaries(record.copy(entries = entries)).associateBy { it.player.id }

        assertEquals(0, summaries.getValue(alice.id).netScore)
        assertEquals(30, summaries.getValue(bob.id).netScore)
        assertEquals(-30, summaries.getValue(carol.id).netScore)
    }

    @Test
    fun manualModeCountsEveryPositiveDeltaAsWin() {
        val record = sampleRecord(ScoringMode.MANUAL)
        val alice = record.players[0]
        val bob = record.players[1]
        val carol = record.players[2]
        val entries = listOf(
            ScoreEntry(
                gameId = record.game.id,
                roundIndex = 0,
                dealerPlayerId = alice.id,
                manualDeltas = listOf(
                    PlayerDelta(alice.id, 10),
                    PlayerDelta(bob.id, 5),
                    PlayerDelta(carol.id, -15),
                ),
            ),
        )

        val summaries = ScoreCalculator.summaries(record.copy(entries = entries)).associateBy { it.player.id }

        assertEquals(1, summaries.getValue(alice.id).winCount)
        assertEquals(1, summaries.getValue(bob.id).winCount)
        assertEquals(0, summaries.getValue(carol.id).winCount)
    }

    @Test
    fun roundTrackerUsesParticipatingPlayerCount() {
        assertEquals(RoundContext(roundIndex = 0, dealerIndex = 0), RoundTracker.currentContext(0, 3))
        assertEquals(RoundContext(roundIndex = 0, dealerIndex = 2), RoundTracker.currentContext(2, 3))
        assertEquals(RoundContext(roundIndex = 1, dealerIndex = 0), RoundTracker.currentContext(3, 3))
        assertEquals(RoundContext(roundIndex = 0, dealerIndex = 0), RoundTracker.currentContext(12, 3))
    }

    @Test
    fun equalEntryEditChangesTotalsWithoutRecordingLosses() {
        val record = sampleRecord(ScoringMode.EQUAL)
        val alice = record.players[0]
        val bob = record.players[1]
        val entry = ScoreEntry(gameId = record.game.id, roundIndex = 0, dealerPlayerId = alice.id, equalWinnerPlayerId = alice.id, equalBaseScore = 10)

        val before = ScoreCalculator.summaries(record.copy(entries = listOf(entry))).associateBy { it.player.id }
        val after = ScoreCalculator.summaries(record.copy(entries = listOf(entry.copy(equalWinnerPlayerId = bob.id, equalBaseScore = 25)))).associateBy { it.player.id }

        assertEquals(20, before.getValue(alice.id).netScore)
        assertEquals(50, after.getValue(bob.id).netScore)
        assertEquals(-50, after.getValue(alice.id).netScore)
    }

    @Test
    fun settlementLocksRecordMutation() {
        val record = sampleRecord(ScoringMode.EQUAL).copy(game = sampleRecord(ScoringMode.EQUAL).game.copy(status = GameStatus.FINISHED))
        val result = ScoreCalculator.validateEqualEntry(record, record.players.first().id, 10)
        assertEquals(false, result.isSuccess)
    }

    private fun sampleRecord(scoringMode: ScoringMode): GameRecord {
        val game = Game(scoringMode = scoringMode)
        val players = listOf("Alice", "Bob", "Carol").mapIndexed { index, name ->
            Player(gameId = game.id, name = name, seatIndex = index)
        }
        return GameRecord(game = game, players = players)
    }
}
