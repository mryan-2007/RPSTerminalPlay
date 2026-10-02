package com.example.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class PlayerRole {
    UNASSIGNED,
    PLAYER1, // Host
    PLAYER2  // Client
}

data class GameUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val serverAddress: String = "ws://127.0.0.1:8765",
    val localPlayerName: String = "Player",
    val role: PlayerRole = PlayerRole.UNASSIGNED,
    val isHostMode: Boolean = true,
    val showGuideDialog: Boolean = false,
    val showConnectDialog: Boolean = false,

    // Match Info
    val stateName: String = "WAITING_FOR_PLAYERS",
    val roundNumber: Int = 1,
    val player1Name: String = "",
    val player2Name: String = "",
    val player1Score: Int = 50,
    val player2Score: Int = 50,
    val player1Ready: Boolean = false,
    val player2Ready: Boolean = false,
    val player1Locked: Boolean = false,
    val player2Locked: Boolean = false,
    val player1Rank: WagerRank? = null,
    val player2Rank: WagerRank? = null,

    // Local Deck
    val localCards: CardCounts = CardCounts(2, 2, 2),

    // Terminal Log
    val terminalEntries: List<TerminalEntry> = emptyList(),

    // Input prompt state
    val inputText: String = "",
    val replyingTo: TerminalEntry.Chat? = null,
    val errorMessage: String? = null
) {
    val isLocalReady: Boolean
        get() = when (role) {
            PlayerRole.PLAYER1 -> player1Ready
            PlayerRole.PLAYER2 -> player2Ready
            PlayerRole.UNASSIGNED -> false
        }

    val isLocalLocked: Boolean
        get() = when (role) {
            PlayerRole.PLAYER1 -> player1Locked
            PlayerRole.PLAYER2 -> player2Locked
            PlayerRole.UNASSIGNED -> false
        }

    val localScore: Int
        get() = when (role) {
            PlayerRole.PLAYER1 -> player1Score
            PlayerRole.PLAYER2 -> player2Score
            PlayerRole.UNASSIGNED -> 50
        }

    val opponentScore: Int
        get() = when (role) {
            PlayerRole.PLAYER1 -> player2Score
            PlayerRole.PLAYER2 -> player1Score
            PlayerRole.UNASSIGNED -> 50
        }

    val opponentName: String
        get() = when (role) {
            PlayerRole.PLAYER1 -> player2Name
            PlayerRole.PLAYER2 -> player1Name
            PlayerRole.UNASSIGNED -> "Opponent"
        }
}
