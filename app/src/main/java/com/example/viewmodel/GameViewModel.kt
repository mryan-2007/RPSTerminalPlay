package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CardCounts
import com.example.model.CardType
import com.example.model.ConnectionStatus
import com.example.model.GameUiState
import com.example.model.PlayerRole
import com.example.model.SystemLevel
import com.example.model.TerminalEntry
import com.example.model.WagerRank
import com.example.network.LocalGameEngine
import com.example.network.NetworkMessageListener
import com.example.network.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class GameViewModel : ViewModel(), NetworkMessageListener {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val wsClient = WebSocketClient(this)
    private var localEngine: LocalGameEngine? = null

    init {
        // Start in pure, clean terminal state without auto-starting local game or score boxes
        showWelcomeTerminal()
    }

    fun showWelcomeTerminal() {
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                role = PlayerRole.UNASSIGNED,
                player1Name = "",
                player2Name = "",
                terminalEntries = listOf(
                    TerminalEntry.SystemMsg(
                        id = UUID.randomUUID().toString(),
                        text = "    ╔══════════════════════════════════╗\n" +
                               "    ║         RPS // TACTICAL TERMINAL       ║\n" +
                               "    ╚══════════════════════════════════╝\n\n" +
                               "Choose an option to play:\n" +
                               "  • Type /local [name]  - Start solo / practice battle\n" +
                               "  • Tap 🌐 (top right)  - Connect to online friend\n" +
                               "  • Type /help          - Show all commands & rules",
                        level = SystemLevel.INFO
                    )
                )
            )
        }
    }

    fun startLocalEngine(playerName: String = _uiState.value.localPlayerName.ifBlank { "Player" }) {
        wsClient.disconnect()
        val cleanName = playerName.trim().ifEmpty { "Player" }
        _uiState.update {
            it.copy(
                isHostMode = true,
                connectionStatus = ConnectionStatus.CONNECTED,
                role = PlayerRole.PLAYER1,
                localPlayerName = cleanName,
                player1Name = cleanName,
                player2Name = "Bot",
                player1Score = 50,
                player2Score = 50,
                terminalEntries = emptyList()
            )
        }
        localEngine = LocalGameEngine(
            localPlayerName = cleanName,
            opponentName = "Bot",
            scope = viewModelScope
        ) { jsonString ->
            processIncomingJson(jsonString)
        }
    }

    fun connectToTermuxServer(address: String, playerName: String = "Player") {
        localEngine = null
        val cleanAddr = address.trim()
        val name = playerName.trim().ifEmpty { "Player" }

        _uiState.update {
            it.copy(
                serverAddress = cleanAddr,
                localPlayerName = name,
                player1Name = "",
                player2Name = "",
                connectionStatus = ConnectionStatus.CONNECTING,
                isHostMode = false,
                terminalEntries = it.terminalEntries + TerminalEntry.SystemMsg(
                    id = UUID.randomUUID().toString(),
                    text = "[SYSTEM] Connecting to server at $cleanAddr as $name...",
                    level = SystemLevel.INFO
                )
            )
        }

        viewModelScope.launch {
            wsClient.connect(cleanAddr)
        }
    }

    fun disconnect() {
        wsClient.disconnect()
        localEngine = null
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                role = PlayerRole.UNASSIGNED,
                player1Name = "",
                player2Name = "",
                terminalEntries = it.terminalEntries + TerminalEntry.SystemMsg(
                    id = UUID.randomUUID().toString(),
                    text = "[SYSTEM] Disconnected from server.",
                    level = SystemLevel.WARNING
                )
            )
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun setReplyingTo(chat: TerminalEntry.Chat?) {
        _uiState.update { it.copy(replyingTo = chat) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun deleteMessage(id: String) {
        _uiState.update { current ->
            current.copy(
                terminalEntries = current.terminalEntries.filterNot { it.id == id },
                replyingTo = if (current.replyingTo?.id == id) null else current.replyingTo
            )
        }
    }

    fun submitCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val replying = _uiState.value.replyingTo

        // Clear input bar and active reply
        _uiState.update { it.copy(inputText = "", replyingTo = null) }

        // Local command handling
        val lower = text.lowercase()
        if (lower == "/clear") {
            clearTerminalLog()
            return
        }

        if (lower.startsWith("/local")) {
            val parts = text.split("\\s+".toRegex())
            val name = if (parts.size > 1) parts[1] else _uiState.value.localPlayerName
            startLocalEngine(name)
            return
        }

        if (lower.startsWith("/connect")) {
            val parts = text.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val addr = parts[1]
                val name = if (parts.size >= 3) parts[2] else _uiState.value.localPlayerName
                connectToTermuxServer(addr, name)
            } else {
                showConnectDialog(true)
            }
            return
        }

        if (lower == "/help") {
            showHelp()
            return
        }

        if (localEngine != null) {
            val formatted = if (replying != null) {
                "[Replying to ${replying.senderName}: \"${replying.text.take(20)}\"] $text"
            } else {
                text
            }
            localEngine?.handleInput(formatted)
        } else if (wsClient.isConnected) {
            val payload = JSONObject().apply {
                put("type", "input")
                put("text", text)
                if (replying != null) {
                    put("reply_to_sender", replying.senderName)
                    put("reply_to_text", replying.text.take(30))
                }
            }
            wsClient.send(payload.toString())
        } else {
            addSystemMessage("[SYSTEM] Not connected to any game. Type /local to play solo or tap 🌐 to connect online.", SystemLevel.WARNING)
        }
    }

    private fun showHelp() {
        val help = """
[SYSTEM] COMMANDS & GAMEPLAY:
  /local [name]     - Start local solo practice
  /connect <url>    - Connect to online / friend
  /start            - Begin active round
  r, p, s           - Choose Rock 🪨, Paper 📄, Scissors ✂️
  c, b, a, s        - Choose Wager Rank (C:3, B:7, A:15, S:25)
  r A, p B          - Shortcut: Card & Rank together
  /cards            - View your remaining cards
  /clear            - Clear terminal screen
""".trimIndent()
        addSystemMessage(help, SystemLevel.INFO)
    }

    fun clearTerminalLog() {
        _uiState.update {
            it.copy(
                terminalEntries = listOf(
                    TerminalEntry.SystemMsg(
                        id = UUID.randomUUID().toString(),
                        text = "[SYSTEM] Terminal cleared.",
                        level = SystemLevel.INFO
                    )
                )
            )
        }
    }

    fun showGuideDialog(show: Boolean) {
        _uiState.update { it.copy(showGuideDialog = show) }
    }

    fun showConnectDialog(show: Boolean) {
        _uiState.update { it.copy(showConnectDialog = show) }
    }

    // Network Callbacks
    override fun onConnected() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
            val handshake = JSONObject().apply {
                put("type", "handshake")
                put("name", _uiState.value.localPlayerName)
            }
            wsClient.send(handshake.toString())
        }
    }

    override fun onDisconnected(reason: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    terminalEntries = it.terminalEntries + TerminalEntry.SystemMsg(
                        id = UUID.randomUUID().toString(),
                        text = "[SYSTEM] Connection closed: $reason",
                        level = SystemLevel.WARNING
                    )
                )
            }
        }
    }

    override fun onMessageReceived(text: String) {
        viewModelScope.launch {
            processIncomingJson(text)
        }
    }

    override fun onError(error: Throwable) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    terminalEntries = it.terminalEntries + TerminalEntry.SystemMsg(
                        id = UUID.randomUUID().toString(),
                        text = "[ERROR] Network failure: ${error.message ?: "Connection error"}\nCheck if server is running at ${_uiState.value.serverAddress}",
                        level = SystemLevel.DANGER
                    )
                )
            }
        }
    }

    private fun processIncomingJson(raw: String) {
        try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "handshake_ack" -> {
                    val roleStr = json.optString("role")
                    val assignedRole = when (roleStr) {
                        "player1" -> PlayerRole.PLAYER1
                        "player2" -> PlayerRole.PLAYER2
                        else -> PlayerRole.UNASSIGNED
                    }
                    val ackName = json.optString("name", _uiState.value.localPlayerName)
                    _uiState.update {
                        it.copy(
                            role = assignedRole,
                            localPlayerName = ackName,
                            connectionStatus = ConnectionStatus.CONNECTED
                        )
                    }
                }
                "card_box" -> { }  

                "countdown" -> {
                    val text = json.optString("text")
                    val levelStr = json.optString("level")

                    val level = when (levelStr) {
                        "countdown_3" -> SystemLevel.COUNTDOWN
                        "countdown_2" -> SystemLevel.WARNING
                        "countdown_1" -> SystemLevel.DANGER
                        "countdown_reveal" -> SystemLevel.HIGHLIGHT
                        else -> SystemLevel.INFO
                    }

                    _uiState.update { state ->
                        val entries = state.terminalEntries.toMutableList()

                        val existingIndex = entries.indexOfLast {
                            it is TerminalEntry.Countdown
                        }

                        if (existingIndex >= 0) {
                            val existing = entries[existingIndex] as TerminalEntry.Countdown

                            entries[existingIndex] = existing.copy(
                                text = text,
                                level = level
                            )
                        } else {
                            entries.add(
                                TerminalEntry.Countdown(
                                    id = UUID.randomUUID().toString(),
                                    text = text,
                                    level = level
                                )
                            )
                        }

                        state.copy(terminalEntries = entries)
                    }
                }
                
                "system" -> {
                    val text = json.optString("text")
                    val levelStr = json.optString("level", "info")
                    val level = when (levelStr) {
                        "warning" -> SystemLevel.WARNING
                        "danger" -> SystemLevel.DANGER
                        "success" -> SystemLevel.SUCCESS
                        "countdown" -> SystemLevel.COUNTDOWN
                        "highlight" -> SystemLevel.HIGHLIGHT
                        "cards_display", "cards" -> SystemLevel.CARDS
                        else -> SystemLevel.INFO
                    }
                    val entry = TerminalEntry.SystemMsg(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        level = level
                    )
                    _uiState.update { it.copy(terminalEntries = it.terminalEntries + entry) }
                }
                "state_sync" -> {
                    val stateName = json.optString("state", _uiState.value.stateName)
                    val round = json.optInt("round", _uiState.value.roundNumber)
                    val p1Name = json.optString("p1_name", _uiState.value.player1Name)
                    val p1Score = json.optInt("p1_score", _uiState.value.player1Score)
                    val p1Ready = json.optBoolean("p1_ready", _uiState.value.player1Ready)
                    val p1Locked = json.optBoolean("p1_locked", _uiState.value.player1Locked)
                    val p1RankStr = json.optString("p1_rank")
                    val p1Rank = if (p1RankStr.isNotEmpty()) WagerRank.fromString(p1RankStr) else null

                    val p2Name = json.optString("p2_name", _uiState.value.player2Name)
                    val p2Score = json.optInt("p2_score", _uiState.value.player2Score)
                    val p2Ready = json.optBoolean("p2_ready", _uiState.value.player2Ready)
                    val p2Locked = json.optBoolean("p2_locked", _uiState.value.player2Locked)
                    val p2RankStr = json.optString("p2_rank")
                    val p2Rank = if (p2RankStr.isNotEmpty()) WagerRank.fromString(p2RankStr) else null

                    _uiState.update {
                        it.copy(
                            stateName = stateName,
                            roundNumber = round,
                            player1Name = p1Name,
                            player1Score = p1Score,
                            player1Ready = p1Ready,
                            player1Locked = p1Locked,
                            player1Rank = p1Rank,
                            player2Name = p2Name,
                            player2Score = p2Score,
                            player2Ready = p2Ready,
                            player2Locked = p2Locked,
                            player2Rank = p2Rank
                        )
                    }
                }
                "cards_sync" -> {
                    val countsObj = json.optJSONObject("counts")
                    if (countsObj != null) {
                        val rock = countsObj.optInt("rock", 2)
                        val paper = countsObj.optInt("paper", 2)
                        val scissors = countsObj.optInt("scissors", 2)
                        _uiState.update { it.copy(localCards = CardCounts(rock, paper, scissors)) }
                    }
                }
                "reveal" -> {
                    val winner = json.optInt("winner")
                    val p1Name = json.optString(
                         "p1_name",
                        _uiState.value.player1Name.ifEmpty { "Player" })
                    val p2Name = json.optString(
                        "p2_name",
                        _uiState.value.player2Name.ifEmpty { "Opponent" })
                    val reason = json.optString("reason")
                    val outcomeText = json.optString("outcome_text")
                    val p1Delta = json.optInt("p1_delta")
                    val p2Delta = json.optInt("p2_delta")
                    val p1Score = json.optInt("p1_score")
                    val p2Score = json.optInt("p2_score")

                    val p1Card = CardType.fromString(
                        json.optString("p1_card")
                    )
                    val p1Rank = WagerRank.fromString(
                        json.optString("p1_rank")
                    )
                    val p2Card = CardType.fromString(
                        json.optString("p2_card")
                    )
                    val p2Rank = WagerRank.fromString(
                        json.optString("p2_rank")
                    )

                    val cardBoxEntry = TerminalEntry.CardBox(
                        id = UUID.randomUUID().toString(),
                        p1Name = p1Name,
                        p1Card = p1Card,
                        p1Rank = p1Rank,
                        p2Name = p2Name,
                        p2Card = p2Card,
                        p2Rank = p2Rank,
                        isRevealed = true
                    )

                    val entry = TerminalEntry.RevealOutcome(
                        id = UUID.randomUUID().toString(),
                        winner = winner,
                        p1Name = p1Name,
                        p2Name = p2Name,
                        reason = reason,
                        outcomeText = outcomeText,
                        p1Delta = p1Delta,
                        p2Delta = p2Delta,
                        p1Score = p1Score,
                        p2Score = p2Score
                    )

                    // STEP 1: SHOW ONLY THE REVEAL BOX
                    _uiState.update {
                        it.copy(
                            terminalEntries = it.terminalEntries + cardBoxEntry,
                            player1Score = p1Score,
                            player2Score = p2Score
                        )
                    }

                    // STEP 2: WAIT FOR THE FULL CARD
                    viewModelScope.launch {
                        delay(3780)

                        // STEP 3: NOW SHOW THE RESULT BOX
                        _uiState.update {
                            it.copy(
                                terminalEntries = it.terminalEntries + entry
                            )
                        }
                    }
                }
                "clear" -> {
                    clearTerminalLog()
                }
            }
        } catch (e: Exception) {
            addSystemMessage("[DEBUG] Error parsing message: ${e.message}", SystemLevel.WARNING)
        }
    }

    private fun addSystemMessage(text: String, level: SystemLevel = SystemLevel.INFO) {
        val entry = TerminalEntry.SystemMsg(
            id = UUID.randomUUID().toString(),
            text = text,
            level = level
        )
        _uiState.update { it.copy(terminalEntries = it.terminalEntries + entry) }
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
        localEngine = null
    }
}
