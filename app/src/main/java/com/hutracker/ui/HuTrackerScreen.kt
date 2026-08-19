package com.hutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hutracker.domain.GameRecord
import com.hutracker.domain.GameStatus
import com.hutracker.domain.Player
import com.hutracker.domain.PlayerSummary
import com.hutracker.domain.ScoringMode
import com.hutracker.domain.SeatDirection
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuTrackerScreen(viewModel: HuTrackerViewModel) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var showRules by remember { mutableStateOf(false) }
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("帮助", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = {
                        showRules = true
                        drawerScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) { Text("国标麻将番种") }
            }
        },
    ) {
        if (showRules) {
            RulesPage(onBack = { showRules = false })
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("胡牌追踪器") },
                        navigationIcon = {
                            IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Outlined.Menu,
                                    contentDescription = "打开菜单",
                                )
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    TabRow(selectedTabIndex = state.activeTab.ordinal) {
                        AppTab.entries.forEach { tab ->
                            Tab(selected = state.activeTab == tab, onClick = { viewModel.setTab(tab) }, text = { Text(tab.label) })
                        }
                    }
                    when (state.activeTab) {
                        AppTab.GAMES -> GameListPane(state = state, onSelect = viewModel::selectGame, onNew = viewModel::openNewGameDialog)
                        AppTab.CURRENT -> CurrentGamePane(state, viewModel::openAddEntryDialog, viewModel::openEditEntryDialog, viewModel::deleteEntry, viewModel::selectEntry, viewModel::openConfirmSettlement)
                        AppTab.SETTLEMENT -> SettlementPane(state, viewModel::confirmSettlement) { viewModel.setTab(AppTab.CURRENT) }
                    }
                }
            }
        }
    }

    if (state.showNewGameDialog) {
        NewGameDialog(
            draft = state.newGameDraft,
            onDismiss = viewModel::closeNewGameDialog,
            onNameChange = viewModel::updateNewGameName,
            onModeChange = viewModel::updateNewGameMode,
            onConfirm = viewModel::createGame,
        )
    }

    if (state.showEntryDialog) {
        EntryDialog(
            game = state.activeGame,
            draft = state.entryDraft,
            onDismiss = viewModel::closeEntryDialog,
            onWinnerChange = viewModel::updateEntryWinner,
            onBaseScoreChange = viewModel::updateEntryBaseScore,
            onManualScoreChange = viewModel::updateManualScore,
            onNoteChange = viewModel::updateEntryNote,
            onConfirm = viewModel::saveEntry,
        )
    }

    if (state.showConfirmSettlement) {
        AlertDialog(
            onDismissRequest = viewModel::closeConfirmSettlement,
            title = { Text("确认结算") },
            text = { Text("确认后当前牌局将锁定，记录不可再修改。") },
            confirmButton = {
                Button(onClick = viewModel::confirmSettlement) {
                    Text("确认")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::closeConfirmSettlement) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RulesPage(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("国标麻将番种") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "UTF-8"
                    loadUrl("file:///android_asset/guobiao_fan.html")
                }
            },
        )
    }
}

@Composable
private fun GameListPane(
    state: HuTrackerUiState,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
            Text("新建牌局")
        }

        state.games.forEach { record ->
            GameCard(record = record, onClick = { onSelect(record.game.id) })
        }
    }
}

@Composable
private fun GameCard(record: GameRecord, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = record.players.joinToString(" · ") { it.name },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "${record.game.scoringMode.label} · ${record.game.status.label}")
            Text(text = "当前：${record.currentSeatLabel()}")
            Text(text = "记录数：${record.entries.size}")
        }
    }
}

@Composable
private fun CurrentGamePane(
    state: HuTrackerUiState,
    onAdd: () -> Unit,
    onEdit: (com.hutracker.domain.ScoreEntry) -> Unit,
    onDelete: (String) -> Unit,
    onSelectEntry: (String) -> Unit,
    onSettle: () -> Unit,
) {
    val record = state.activeGame
    if (record == null) {
        EmptyPane("暂无牌局", "到“牌局”页新建一局。")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(record = record)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.game.status == GameStatus.ACTIVE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedButton(onClick = onAdd) { Text("记一笔") }
                        OutlinedButton(onClick = onSettle) { Text("结算") }
                    }
                } else {
                    Text("已结算，记录只读")
                }
            }
        }

        Text("记录", style = MaterialTheme.typography.titleMedium)
        if (record.entries.isEmpty()) {
            EmptyPane("暂无记录", "先记一笔。")
        } else {
            record.entries.forEachIndexed { index, entry ->
                EntryCard(
                    entry = entry,
                    players = record.players,
                    onEdit = if (record.game.status == GameStatus.ACTIVE) { { onEdit(entry) } } else null,
                    onDelete = if (record.game.status == GameStatus.ACTIVE) { { onDelete(entry.id) } } else null,
                    onSelect = { onSelectEntry(entry.id) },
                    selected = state.selectedEntryId == entry.id,
                    index = index + 1,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(record: GameRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("当前牌局", style = MaterialTheme.typography.titleMedium)
            Text(record.players.joinToString(" · ") { it.name })
            Text("模式：${record.game.scoringMode.label} · 状态：${record.game.status.label}")
            Text("当前：${record.currentSeatLabel()}")
        }
    }
}

@Composable
private fun EntryCard(
    entry: com.hutracker.domain.ScoreEntry,
    players: List<Player>,
    index: Int,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSelect: () -> Unit,
    selected: Boolean,
) {
    val content = if (entry.equalWinnerPlayerId != null) {
        val winner = players.first { it.id == entry.equalWinnerPlayerId }
        "等额 · ${winner.name} +${entry.equalBaseScore}"
    } else {
        val positive = entry.manualDeltas.filter { it.delta > 0 }
        val positiveText = positive.joinToString(" / ") { delta ->
            val player = players.first { it.id == delta.playerId }
            "${player.name} +${delta.delta}"
        }
        "非等额 · $positiveText"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.White),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$index.", style = MaterialTheme.typography.labelLarge)
            Text(content, modifier = Modifier.weight(1f), maxLines = 1)
            if (entry.note.isNotBlank()) Text(entry.note, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            if (selected) {
                if (onEdit != null) OutlinedButton(onClick = onEdit) { Text("修改") }
                if (onDelete != null) OutlinedButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

@Composable
private fun SettlementPane(
    state: HuTrackerUiState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val record = state.activeGame
    if (record == null) {
        EmptyPane("暂无结算", "先创建或选择一局牌局。")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(record = record)
        Text("结算预览", style = MaterialTheme.typography.titleMedium)
        state.summaries.forEach { summary ->
            SummaryRow(summary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("付款明细", style = MaterialTheme.typography.titleMedium)
        if (state.settlementLines.isEmpty()) {
            Text("暂无需要结算的差额。")
        } else {
            state.settlementLines.forEach { line ->
                Text("${line.fromPlayer.name} 给 ${line.toPlayer.name} ${line.amount}")
            }
        }
        if (record.game.status == GameStatus.ACTIVE) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Button(onClick = onConfirm) { Text("确认结算") }
            }
        } else {
            Text("已结算，记录已锁定。")
        }
    }
}

@Composable
private fun SummaryRow(summary: PlayerSummary) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = playerColor(summary.player.seatIndex))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(summary.player.name, style = MaterialTheme.typography.titleMedium)
            Text("净分：${summary.netScore}")
            Text("得分：${summary.recordedWinTotal}")
            Text("胡牌次数：${summary.winCount}")
            Text("平均得分：${String.format(Locale.CHINA, "%.2f", summary.averageWinScore)}")
        }
    }
}

@Composable
private fun NewGameDialog(
    draft: NewGameDraft,
    onDismiss: () -> Unit,
    onNameChange: (Int, String) -> Unit,
    onModeChange: (ScoringMode) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建牌局") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.playerNames.forEachIndexed { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { onNameChange(index, it) },
                        label = { Text("玩家 ${index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.scoringMode == ScoringMode.EQUAL,
                        onClick = { onModeChange(ScoringMode.EQUAL) },
                        label = { Text("等额") },
                    )
                    FilterChip(
                        selected = draft.scoringMode == ScoringMode.MANUAL,
                        onClick = { onModeChange(ScoringMode.MANUAL) },
                        label = { Text("非等额") },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("创建") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EntryDialog(
    game: GameRecord?,
    draft: EntryDraft,
    onDismiss: () -> Unit,
    onWinnerChange: (String) -> Unit,
    onBaseScoreChange: (String) -> Unit,
    onManualScoreChange: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    if (game == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.entryId == null) "记一笔" else "修改记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (game.game.scoringMode == ScoringMode.EQUAL) {
                    Text("选择得分人")
                    game.players.forEach { player ->
                        FilterChip(
                            selected = draft.winnerPlayerId == player.id,
                            onClick = { onWinnerChange(player.id) },
                            label = { Text(player.name) },
                        )
                    }
                    OutlinedTextField(
                        value = draft.baseScore,
                        onValueChange = onBaseScoreChange,
                        label = { Text("分数") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    game.players.forEach { player ->
                        OutlinedTextField(
                            value = draft.manualScores[player.id].orEmpty(),
                            onValueChange = { onManualScoreChange(player.id, it) },
                            label = { Text("${player.name} 分数") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = onNoteChange,
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EmptyPane(title: String, message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message)
        }
    }
}

private fun Int.toDirectionLabel(): String = SeatDirection.entries[this % SeatDirection.entries.size].label + "风"

private fun GameRecord.currentSeatLabel(): String =
    "${game.currentRoundIndex.toDirectionLabel()}${players[game.currentDealerIndex].seat.label}"

private fun playerColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFE0F2FE), Color(0xFFDCFCE7), Color(0xFFFEF3C7),
        Color(0xFFFCE7F3), Color(0xFFEDE9FE), Color(0xFFFFEDD5),
    )
    return colors[index]
}
