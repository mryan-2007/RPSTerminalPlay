package com.example.model

enum class SystemLevel {
    INFO,
    WARNING,
    SUCCESS,
    DANGER,
    COUNTDOWN,
    HIGHLIGHT,
    CARDS
}

sealed class TerminalEntry {
    abstract val id: String
    abstract val timestamp: Long

    data class Chat(
        override val id: String,
        val text: String,
        val senderRole: String, // "player1" | "player2"
        val senderName: String,
        val isLocal: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TerminalEntry()

    data class SystemMsg(
        override val id: String,
        val text: String,
        val level: SystemLevel = SystemLevel.INFO,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TerminalEntry()

    data class CardBox(
        override val id: String,
        val p1Name: String,
        val p1Card: CardType?,
        val p1Rank: WagerRank?,
        val p2Name: String,
        val p2Card: CardType?,
        val p2Rank: WagerRank?,
        val isRevealed: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TerminalEntry()

    data class RevealOutcome(
        override val id: String,
        val winner: Int, // 0: draw, 1: p1, 2: p2
        val reason: String,
        val outcomeText: String,
        val p1Delta: Int,
        val p2Delta: Int,
        val p1Score: Int,
        val p2Score: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TerminalEntry()
}
