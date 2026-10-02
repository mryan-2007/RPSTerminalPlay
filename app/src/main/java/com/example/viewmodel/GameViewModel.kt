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
        // Start in Local Engine mode initially so user can test and play immediately!
        startLocalEngine()
    }

    fun startLocalEngine() {
        wsClient.disconnect()
        _uiState.update {
            it.copy(
                isHostMode = true,
                connectionStatus = ConnectionStatus.CONNECTED,
                role = PlayerRole.PLAYER1,
                localPlayerName = "Ryan",
                player1Name = "Ryan",
                player2Name = "Friend",
                terminalEntries = emptyList()
            )
        }
        localEngine = LocalGameEngine(viewModelScope) { jsonString ->
            processIncomingJson(jsonString)
        }
    }

    fun connectToTermuxServer(address: String, playerName: String = "Ryan") {
        localEngine = null
        val cleanAddr = address.trim()
        val name = playerName.trim().ifEmpty { "Ryan" }

        _uiState.update {
            it.copy(
                serverAddress = cleanAddr,
                localPlayerName = name,
                connectionStatus = ConnectionStatus.CONNECTING,
                isHostMode = false,
                terminalEntries = it.terminalEntries + TerminalEntry.SystemMsg(
                    id = UUID.randomUUID().toString(),
                    text = "[SYSTEM] Connecting to Termux server at $cleanAddr...",
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

    fun submitCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        // Clear input bar
        _uiState.update { it.copy(inputText = "") }

        // Local command handling for /clear: always clears visible log immediately
        if (text.equals("/clear", ignoreCase = true)) {
            clearTerminalLog()
            // Still dispatch to server/engine to inform if needed
        }

        if (localEngine != null) {
            localEngine?.handleInput(text)
        } else if (wsClient.isConnected) {
            val payload = JSONObject().apply {
                put("type", "input")
                put("text", text)
            }
            wsClient.send(payload.toString())
        } else {
            addSystemMessage("[SYSTEM] Not connected to any server. Check Termux connection or switch to Local mode.", SystemLevel.DANGER)
        }
    }

    fun quickCommand(command: String) {
        _uiState.update { it.copy(inputText = command) }
        submitCurrentInput()
    }

    fun appendToInput(token: String) {
        _uiState.update {
            val current = it.inputText
            val updated = if (current.isEmpty()) token else "$current $token"
            it.copy(inputText = updated)
        }
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
            // Send handshake
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
                        text = "[ERROR] Network failure: ${error.message ?: "Connection error"}\nCheck if Termux server is running at ${_uiState.value.serverAddress}",
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
                    _uiState.update {
                        it.copy(
                            role = assignedRole,
                            connectionStatus = ConnectionStatus.CONNECTED
                        )
                    }
                }
                "chat" -> {
                    val text = json.optString("text")
                    val senderRole = json.optString("sender_role")
                    val senderName = json.optString("sender_name", "Player")
                    val isLocal = when (_uiState.value.role) {
                        PlayerRole.PLAYER1 -> senderRole == "player1"
                        PlayerRole.PLAYER2 -> senderRole == "player2"
                        else -> false
                    }
                    val entry = TerminalEntry.Chat(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        senderRole = senderRole,
                        senderName = senderName,
                        isLocal = isLocal
                    )
                    _uiState.update { it.copy(terminalEntries = it.terminalEntries + entry) }
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
                        "cards_display" -> SystemLevel.CARDS
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
                    val reason = json.optString("reason")
                    val outcomeText = json.optString("outcome_text")
                    val p1Delta = json.optInt("p1_delta")
                    val p2Delta = json.optInt("p2_delta")
                    val p1Score = json.optInt("p1_score")
                    val p2Score = json.optInt("p2_score")
                    val entry = TerminalEntry.RevealOutcome(
                        id = UUID.randomUUID().toString(),
                        winner = winner,
                        reason = reason,
                        outcomeText = outcomeText,
                        p1Delta = p1Delta,
                        p2Delta = p2Delta,
                        p1Score = p1Score,
                        p2Score = p2Score
                    )
                    _uiState.update {
                        it.copy(
                            terminalEntries = it.terminalEntries + entry,
                            player1Score = p1Score,
                            player2Score = p2Score
                        )
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
