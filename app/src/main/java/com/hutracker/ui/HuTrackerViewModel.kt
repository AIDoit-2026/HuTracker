package com.hutracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hutracker.data.RoomGameStore
import com.hutracker.domain.GameRecord
import com.hutracker.domain.GameStatus
import com.hutracker.domain.PlayerDelta
import com.hutracker.domain.PlayerSummary
import com.hutracker.domain.ScoreCalculator
import com.hutracker.domain.ScoreEntry
import com.hutracker.domain.ScoringMode
import com.hutracker.domain.SettlementLine

enum class AppTab(val label: String) {
    GAMES("牌局"),
    CURRENT("记分"),
    SETTLEMENT("结算"),
}

data class NewGameDraft(
    val playerNames: List<String> = listOf("", "", "", ""),
    val scoringMode: ScoringMode = ScoringMode.EQUAL,
)

data class EntryDraft(
    val entryId: String? = null,
    val winnerPlayerId: String? = null,
    val baseScore: String = "",
    val manualScores: Map<String, String> = emptyMap(),
    val note: String = "",
)

data class HuTrackerUiState(
    val games: List<GameRecord> = emptyList(),
    val selectedGameId: String? = null,
    val activeGame: GameRecord? = null,
    val summaries: List<PlayerSummary> = emptyList(),
    val settlementLines: List<SettlementLine> = emptyList(),
    val activeTab: AppTab = AppTab.GAMES,
    val showNewGameDialog: Boolean = false,
    val newGameDraft: NewGameDraft = NewGameDraft(),
    val showEntryDialog: Boolean = false,
    val entryDraft: EntryDraft = EntryDraft(),
    val showConfirmSettlement: Boolean = false,
    val errorMessage: String? = null,
)

class HuTrackerViewModel(private val store: RoomGameStore) : ViewModel() {

    var uiState by mutableStateOf(HuTrackerUiState())
        private set

    init {
        sync()
    }

    fun setTab(tab: AppTab) {
        sync(uiState.copy(activeTab = tab))
    }

    fun selectGame(gameId: String) {
        sync(uiState.copy(selectedGameId = gameId, activeTab = AppTab.CURRENT))
    }

    fun openNewGameDialog() {
        sync(uiState.copy(showNewGameDialog = true, newGameDraft = NewGameDraft()))
    }

    fun closeNewGameDialog() {
        sync(uiState.copy(showNewGameDialog = false))
    }

    fun updateNewGameName(index: Int, name: String) {
        val names = uiState.newGameDraft.playerNames.toMutableList()
        names[index] = name
        sync(uiState.copy(newGameDraft = uiState.newGameDraft.copy(playerNames = names)))
    }

    fun updateNewGameMode(scoringMode: ScoringMode) {
        sync(uiState.copy(newGameDraft = uiState.newGameDraft.copy(scoringMode = scoringMode)))
    }

    fun createGame() {
        runCatching {
            val names = uiState.newGameDraft.playerNames.map { it.trim() }.filter { it.isNotBlank() }
            store.createGame(names, uiState.newGameDraft.scoringMode)
        }.onSuccess { record ->
            sync(
                uiState.copy(
                    selectedGameId = record.game.id,
                    activeTab = AppTab.CURRENT,
                    showNewGameDialog = false,
                    errorMessage = null,
                ),
            )
        }.onFailure { error ->
            sync(uiState.copy(errorMessage = error.message ?: "创建牌局失败"))
        }
    }

    fun openAddEntryDialog() {
        val record = uiState.activeGame ?: return
        if (record.game.status == GameStatus.FINISHED) {
            sync(uiState.copy(errorMessage = "已结算牌局不能继续记分"))
            return
        }
        val draft = EntryDraft(
            winnerPlayerId = record.players.firstOrNull()?.id,
            manualScores = record.players.associate { it.id to "0" },
        )
        sync(uiState.copy(showEntryDialog = true, entryDraft = draft))
    }

    fun openEditEntryDialog(entry: ScoreEntry) {
        val record = uiState.activeGame ?: return
        if (record.game.status == GameStatus.FINISHED) {
            sync(uiState.copy(errorMessage = "已结算牌局不能修改记录"))
            return
        }
        val draft = EntryDraft(
            entryId = entry.id,
            winnerPlayerId = entry.equalWinnerPlayerId ?: record.players.firstOrNull()?.id,
            baseScore = entry.equalBaseScore?.toString().orEmpty(),
            manualScores = record.players.associate { player ->
                val delta = entry.manualDeltas.firstOrNull { it.playerId == player.id }?.delta ?: 0
                player.id to delta.toString()
            },
            note = entry.note,
        )
        sync(uiState.copy(showEntryDialog = true, entryDraft = draft))
    }

    fun closeEntryDialog() {
        sync(uiState.copy(showEntryDialog = false, entryDraft = EntryDraft()))
    }

    fun updateEntryWinner(playerId: String) {
        sync(uiState.copy(entryDraft = uiState.entryDraft.copy(winnerPlayerId = playerId)))
    }

    fun updateEntryBaseScore(score: String) {
        sync(uiState.copy(entryDraft = uiState.entryDraft.copy(baseScore = score.filter { it.isDigit() })))
    }

    fun updateManualScore(playerId: String, score: String) {
        val normalized = score.filterIndexed { index, char -> char.isDigit() || (char == '-' && index == 0) }
        sync(uiState.copy(entryDraft = uiState.entryDraft.copy(manualScores = uiState.entryDraft.manualScores + (playerId to normalized))))
    }

    fun updateEntryNote(note: String) {
        sync(uiState.copy(entryDraft = uiState.entryDraft.copy(note = note)))
    }

    fun saveEntry() {
        val record = uiState.activeGame ?: return
        val draft = uiState.entryDraft
        runCatching {
            when (record.game.scoringMode) {
                ScoringMode.EQUAL -> {
                    val winnerId = requireNotNull(draft.winnerPlayerId) { "请选择得分人" }
                    val baseScore = draft.baseScore.toIntOrNull() ?: 0
                    if (draft.entryId == null) {
                        store.addEqualEntry(record.game.id, winnerId, baseScore, draft.note)
                    } else {
                        store.updateEqualEntry(record.game.id, draft.entryId, winnerId, baseScore, draft.note)
                    }
                }
                ScoringMode.MANUAL -> {
                    val deltas = record.players.map { player ->
                        PlayerDelta(player.id, draft.manualScores[player.id]?.toIntOrNull() ?: 0)
                    }
                    if (draft.entryId == null) {
                        store.addManualEntry(record.game.id, deltas, draft.note)
                    } else {
                        store.updateManualEntry(record.game.id, draft.entryId, deltas, draft.note)
                    }
                }
            }
        }.onSuccess {
            sync(uiState.copy(showEntryDialog = false, entryDraft = EntryDraft(), errorMessage = null))
        }.onFailure { error ->
            sync(uiState.copy(errorMessage = error.message ?: "保存记录失败"))
        }
    }

    fun deleteEntry(entryId: String) {
        val record = uiState.activeGame ?: return
        runCatching {
            store.deleteEntry(record.game.id, entryId)
        }.onSuccess {
            sync(uiState.copy(errorMessage = null))
        }.onFailure { error ->
            sync(uiState.copy(errorMessage = error.message ?: "删除记录失败"))
        }
    }

    fun openConfirmSettlement() {
        val record = uiState.activeGame ?: return
        if (record.entries.isEmpty()) {
            sync(uiState.copy(errorMessage = "至少需要一条记录才能结算"))
            return
        }
        sync(uiState.copy(showConfirmSettlement = true, activeTab = AppTab.SETTLEMENT))
    }

    fun closeConfirmSettlement() {
        sync(uiState.copy(showConfirmSettlement = false))
    }

    fun confirmSettlement() {
        val record = uiState.activeGame ?: return
        store.confirmSettlement(record.game.id)
        sync(uiState.copy(showConfirmSettlement = false, activeTab = AppTab.SETTLEMENT))
    }

    fun clearError() {
        sync(uiState.copy(errorMessage = null))
    }

    private fun sync(base: HuTrackerUiState = uiState) {
        val games = store.listGames()
        val selectedGameId = base.selectedGameId ?: games.firstOrNull()?.game?.id
        val activeGame = selectedGameId?.let { store.getGame(it) }
        uiState = base.copy(
            games = games,
            selectedGameId = selectedGameId,
            activeGame = activeGame,
            summaries = activeGame?.let(ScoreCalculator::summaries).orEmpty(),
            settlementLines = activeGame?.let(ScoreCalculator::settlementLines).orEmpty(),
        )
    }
}
