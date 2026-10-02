package com.example.network

import com.example.model.CardCounts
import com.example.model.CardType
import com.example.model.WagerRank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

/**
 * Embedded authoritative Game Engine running locally on device.
 * Enforces the exact same state machine, rules, wagers, and reveal sequence
 * as the Termux Python server.
 */
class LocalGameEngine(
    private val scope: CoroutineScope,
    private val onMessageToClient: (String) -> Unit
) {
    private var p1Score = 50
    private var p2Score = 50
    private var p1Cards = CardCounts(2, 2, 2)
    private var p2Cards = CardCounts(2, 2, 2)

    private var p1Ready = false
    private var p2Ready = false

    private var p1PendingCard: CardType? = null
    private var p1ChosenCard: CardType? = null
    private var p1ChosenRank: WagerRank? = null

    private var p2ChosenCard: CardType? = null
    private var p2ChosenRank: WagerRank? = null

    private var roundNumber = 1
    private var state = "WAITING_FOR_START"
    private var activeSequenceJob: Job? = null

    init {
        sendInitialState()
    }

    private fun sendInitialState() {
        val handshakeAck = JSONObject().apply {
            put("type", "handshake_ack")
            put("player_id", "p_host")
            put("role", "player1")
            put("name", "Ryan")
            put("status", "connected")
        }
        onMessageToClient(handshakeAck.toString())

        val welcomeMsg = JSONObject().apply {
            put("type", "system")
            put("text", "[SYSTEM] Connected to Local Battle Engine.\n[SYSTEM] Player 2 (Friend) connected.\n[SYSTEM] Type /start when ready.")
            put("level", "success")
        }
        onMessageToClient(welcomeMsg.toString())

        syncState()
    }

    fun handleInput(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        if (clean.startsWith("/")) {
            handleCommand(clean)
        } else {
            // Check if conversational card or rank selection during active round
            val handledSelection = handleConversationalSelection(clean)
            if (!handledSelection) {
                // Regular chat from local player
                val chatMsg = JSONObject().apply {
                    put("type", "chat")
                    put("sender_id", "p_host")
                    put("sender_role", "player1")
                    put("sender_name", "Ryan")
                    put("text", clean)
                }
                onMessageToClient(chatMsg.toString())

                // Friendly banter reply
                if (Random.nextFloat() > 0.45f) {
                    scope.launch {
                        delay(Random.nextLong(600, 1400))
                        val replies = listOf(
                            "Let's see what you've got!",
                            "I'm ready when you are.",
                            "Gamble high if you dare.",
                            "May the best RPS strategist win.",
                            "Locked and loaded."
                        )
                        val friendChat = JSONObject().apply {
                            put("type", "chat")
                            put("sender_id", "p_friend")
                            put("sender_role", "player2")
                            put("sender_name", "Friend")
                            put("text", replies.random())
                        }
                        onMessageToClient(friendChat.toString())
                    }
                }
            }
        }
    }

    private fun handleConversationalSelection(text: String): Boolean {
        if (state != "WAITING_FOR_CHOICES" || p1ChosenCard != null) {
            return false
        }

        val parts = text.split("\\s+".toRegex())

        // Case 1: Both card + rank entered at once (e.g. "rock A")
        if (parts.size == 2) {
            val c = CardType.fromString(parts[0])
            val r = WagerRank.fromString(parts[1])
            if (c != null && r != null) {
                handleChoose(parts[0], parts[1])
                return true
            }
        }

        // Case 2: Just card entered (e.g. "rock", "paper", "scissors")
        if (parts.size == 1) {
            val card = CardType.fromString(parts[0])
            if (card != null) {
                if (!p1Cards.hasAny(card)) {
                    sendSystem("[SYSTEM] You have no remaining ${card.displayName} cards! Choose from available cards.", "danger")
                    return true
                }
                p1PendingCard = card
                sendSystem(
                    "[CARD SELECTED: ${card.displayName}]\n" +
                    "Now choose a wager rank:\n" +
                    "  C  (3 points)   - Minimal risk\n" +
                    "  B  (7 points)   - Standard wager\n" +
                    "  A  (15 points)  - High stakes\n" +
                    "  S  (25 points)  - Supreme gamble\n" +
                    "Type: C, B, A, or S (Your score: $p1Score)",
                    "info"
                )
                return true
            }
        }

        // Case 3: Card was previously chosen, now entering rank (e.g. "A", "B", "C", "S")
        if (parts.size == 1 && p1PendingCard != null) {
            val rank = WagerRank.fromString(parts[0])
            if (rank != null) {
                handleChoose(p1PendingCard!!.code, parts[0])
                p1PendingCard = null
                return true
            }
        }

        return false
    }

    private fun handleCommand(cmdText: String) {
        val parts = cmdText.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()

        when (cmd) {
            "/start" -> handleStart()
            "/cards" -> handleCards()
            "/choose", "/play" -> {
                if (parts.size < 3) {
                    sendSystem("[SYSTEM] Usage: /choose <card> <rank>\nExample: /choose rock A\nRanks: C (3), B (7), A (15), S (25)", "warning")
                    return
                }
                handleChoose(parts[1], parts[2])
            }
            "/clear" -> {
                val clearMsg = JSONObject().apply {
                    put("type", "clear")
                    put("text", "[SYSTEM] Terminal cleared.")
                }
                onMessageToClient(clearMsg.toString())
            }
            "/help" -> handleHelp()
            "/status" -> handleStatus()
            else -> {
                sendSystem("[SYSTEM] Unknown command '$cmd'. Type /help for available commands.", "danger")
            }
        }
    }

    private fun handleStart() {
        if (state !in listOf("WAITING_FOR_START", "NEXT_ROUND", "CONNECTED")) {
            sendSystem("[SYSTEM] Round already in progress.", "warning")
            return
        }

        p1Ready = true
        sendSystem("[SYSTEM] PLAYER 1 (You) is ready.", "info")

        syncState()

        // Friend also readies up after short delay
        scope.launch {
            delay(800)
            p2Ready = true
            sendSystem("[SYSTEM] PLAYER 2 (Friend) is ready.", "info")
            delay(500)
            startRound()
        }
    }

    private fun startRound() {
        state = "WAITING_FOR_CHOICES"
        p1PendingCard = null
        p1ChosenCard = null
        p1ChosenRank = null
        p2ChosenCard = null
        p2ChosenRank = null

        val banner = """
========================================
   ROUND $roundNumber STARTING
   BOTH PLAYERS READY
========================================
""".trimIndent()
        sendSystem(banner, "success")

        val promptCards = """
YOUR AVAILABLE CARDS:
${formatCardsBracket(p1Cards)}

Type the card you want to play: rock, paper, or scissors
(Or enter both: e.g. rock A)
""".trimIndent()
        sendSystem(promptCards, "cards_display")

        syncState()

        // Friend decides their card and rank after short delay
        scope.launch {
            delay(Random.nextLong(1500, 3000))
            pickFriendChoice()
        }
    }

    private fun formatCardsBracket(deck: CardCounts): String {
        val lines = mutableListOf<String>()
        lines.add(if (deck.rock > 0) List(deck.rock) { "[ROCK]" }.joinToString(" ") else "[ROCK] (EMPTY)")
        lines.add(if (deck.paper > 0) List(deck.paper) { "[PAPER]" }.joinToString(" ") else "[PAPER] (EMPTY)")
        lines.add(if (deck.scissors > 0) List(deck.scissors) { "[SCISSORS]" }.joinToString(" ") else "[SCISSORS] (EMPTY)")
        return lines.joinToString("\n")
    }

    private fun pickFriendChoice() {
        val available = mutableListOf<CardType>()
        if (p2Cards.rock > 0) available.add(CardType.ROCK)
        if (p2Cards.paper > 0) available.add(CardType.PAPER)
        if (p2Cards.scissors > 0) available.add(CardType.SCISSORS)

        if (available.isEmpty()) {
            p2Cards = CardCounts(2, 2, 2)
            available.addAll(listOf(CardType.ROCK, CardType.PAPER, CardType.SCISSORS))
        }

        val chosenType = available.random()
        // Choose affordable rank
        val affordableRanks = WagerRank.entries.filter { it.points <= p2Score }
        val chosenRank = if (affordableRanks.isNotEmpty()) affordableRanks.random() else WagerRank.C

        p2ChosenCard = chosenType
        p2ChosenRank = chosenRank

        // Remove card from friend inventory
        p2Cards = when (chosenType) {
            CardType.ROCK -> p2Cards.copy(rock = p2Cards.rock - 1)
            CardType.PAPER -> p2Cards.copy(paper = p2Cards.paper - 1)
            CardType.SCISSORS -> p2Cards.copy(scissors = p2Cards.scissors - 1)
        }

        // Send masked indicator: opponent sees rank only, card hidden!
        sendSystem("[SYSTEM] PLAYER 2 locked card: [ ??????  ${chosenRank.code} ]", "info")
        syncState()

        checkBothLocked()
    }

    private fun handleChoose(cardStr: String, rankStr: String) {
        if (state != "WAITING_FOR_CHOICES") {
            sendSystem("[SYSTEM] No active round for choosing.\nUse /start when ready.", "warning")
            return
        }

        if (p1ChosenCard != null) {
            sendSystem("[SYSTEM] You already locked in your card for this round.", "warning")
            return
        }

        val cardType = CardType.fromString(cardStr)
        if (cardType == null) {
            sendSystem("[SYSTEM] Invalid card '$cardStr'. Choose from: rock, paper, scissors.", "danger")
            return
        }

        val rank = WagerRank.fromString(rankStr)
        if (rank == null) {
            sendSystem("[SYSTEM] Invalid rank '$rankStr'. Available: C (3pts), B (7pts), A (15pts), S (25pts).", "danger")
            return
        }

        if (!p1Cards.hasAny(cardType)) {
            sendSystem("[SYSTEM] You have no remaining ${cardType.displayName} cards! Check /cards.", "danger")
            return
        }

        if (p1Score < rank.points) {
            sendSystem("[SYSTEM] Cannot afford wager ${rank.code} (${rank.points} pts)! Your score is $p1Score.", "danger")
            return
        }

        p1ChosenCard = cardType
        p1ChosenRank = rank

        // Deduct card
        p1Cards = when (cardType) {
            CardType.ROCK -> p1Cards.copy(rock = p1Cards.rock - 1)
            CardType.PAPER -> p1Cards.copy(paper = p1Cards.paper - 1)
            CardType.SCISSORS -> p1Cards.copy(scissors = p1Cards.scissors - 1)
        }

        sendSystem("[SYSTEM] Card locked: [${cardType.displayName}    ${rank.code}] (Wager: ${rank.points} pts)", "success")
        syncState()

        checkBothLocked()
    }

    private fun checkBothLocked() {
        if (p1ChosenCard != null && p2ChosenCard != null) {
            state = "BOTH_CHOICES_LOCKED"
            syncState()
            triggerRevealSequence()
        }
    }

    private fun triggerRevealSequence() {
        activeSequenceJob?.cancel()
        activeSequenceJob = scope.launch {
            state = "REVEAL"
            sendSystem("\n[SYSTEM] BOTH PLAYERS LOCKED.", "info")

            delay(900)
            sendSystem("        3...", "countdown")
            delay(900)
            sendSystem("        2...", "countdown")
            delay(900)
            sendSystem("        1...", "countdown")
            delay(900)
            sendSystem("        REVEAL!", "highlight")
            delay(500)

            val p1Card = p1ChosenCard ?: CardType.ROCK
            val p1Rk = p1ChosenRank ?: WagerRank.C
            val p2Card = p2ChosenCard ?: CardType.ROCK
            val p2Rk = p2ChosenRank ?: WagerRank.C

            // Visual ASCII cards
            val ascii = """
   PLAYER 1 (Ryan)
   ┌───────────┐
   │ ${p1Card.displayName.padEnd(7)} ${p1Rk.code} │
   └───────────┘
        VS
   PLAYER 2 (Friend)
   ┌───────────┐
   │ ${p2Card.displayName.padEnd(7)} ${p2Rk.code} │
   └───────────┘
""".trimIndent()

            sendSystem(ascii, "card_box")

            // Determine winner
            val winner = determineWinner(p1Card, p2Card)
            var p1Delta = 0
            var p2Delta = 0
            val outcomeText: String
            val reason: String

            if (winner == 0) {
                reason = "BOTH PLAYED ${p1Card.displayName}. IT'S A DRAW!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] DRAW! Wagers returned to both players."
            } else if (winner == 1) {
                p1Delta = p2Rk.points
                p2Delta = -p2Rk.points
                p1Score += p1Delta
                p2Score += p2Delta
                reason = "${p1Card.displayName} BEATS ${p2Card.displayName}!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] PLAYER 1 WINS!\n[SYSTEM] Ryan +$p1Delta | Friend $p2Delta"
            } else {
                p1Delta = -p1Rk.points
                p2Delta = p1Rk.points
                p1Score += p1Delta
                p2Score += p2Delta
                reason = "${p2Card.displayName} BEATS ${p1Card.displayName}!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] PLAYER 2 WINS!\n[SYSTEM] Friend +$p2Delta | Ryan $p1Delta"
            }

            // Structured reveal payload
            val revealPayload = JSONObject().apply {
                put("type", "reveal")
                put("p1_card", p1Card.name)
                put("p1_rank", p1Rk.code)
                put("p2_card", p2Card.name)
                put("p2_rank", p2Rk.code)
                put("winner", winner)
                put("reason", reason)
                put("outcome_text", outcomeText)
                put("p1_delta", p1Delta)
                put("p2_delta", p2Delta)
                put("p1_score", p1Score)
                put("p2_score", p2Score)
            }
            onMessageToClient(revealPayload.toString())

            sendSystem(outcomeText, if (winner > 0) "success" else "info")

            // Check if deck needs replenishment
            if (p1Cards.total == 0) {
                p1Cards = CardCounts(2, 2, 2)
                sendSystem("[SYSTEM] All cards used! Deck replenished with 6 new cards (2 Rock, 2 Paper, 2 Scissors).", "info")
            }
            if (p2Cards.total == 0) {
                p2Cards = CardCounts(2, 2, 2)
            }

            roundNumber++
            state = "WAITING_FOR_START"
            p1Ready = false
            p2Ready = false

            sendSystem("\n[SYSTEM] Type /start to begin Round $roundNumber.", "info")
            syncState()
        }
    }

    private fun determineWinner(c1: CardType, c2: CardType): Int {
        if (c1 == c2) return 0
        return if ((c1 == CardType.ROCK && c2 == CardType.SCISSORS) ||
            (c1 == CardType.SCISSORS && c2 == CardType.PAPER) ||
            (c1 == CardType.PAPER && c2 == CardType.ROCK)) 1 else 2
    }

    private fun handleCards() {
        val lines = mutableListOf<String>()
        lines.add(if (p1Cards.rock > 0) List(p1Cards.rock) { "[ROCK]" }.joinToString(" ") else "[ROCK] (EMPTY)")
        lines.add(if (p1Cards.paper > 0) List(p1Cards.paper) { "[PAPER]" }.joinToString(" ") else "[PAPER] (EMPTY)")
        lines.add(if (p1Cards.scissors > 0) List(p1Cards.scissors) { "[SCISSORS]" }.joinToString(" ") else "[SCISSORS] (EMPTY)")

        sendSystem("YOUR INVENTORY:\n${lines.joinToString("\n")}", "cards_display")
    }

    private fun handleHelp() {
        val help = """
========================================
       AVAILABLE TERMINAL COMMANDS
========================================

/start
    Ready yourself for the next round.
    Both players must type /start.

/cards
    Display your remaining cards:
    [ROCK], [PAPER], [SCISSORS]

/choose <card> <rank>
    Select your card and wager rank.
    Cards: rock, paper, scissors
    Ranks:
      C  (3 points)   - Minimal risk
      B  (7 points)   - Standard wager
      A  (15 points)  - High stakes
      S  (25 points)  - Supreme gamble
    Aliases: /play <card> <rank>
    Example: /choose rock A

/clear
    Clear your visible terminal chat log.
    (Preserves game state and score)

/help
    Show this command guide.

/status
    Display current connection & round info.
========================================
""".trimIndent()
        sendSystem(help, "info")
    }

    private fun handleStatus() {
        val status = """
[SYSTEM] STATUS:
  Role: PLAYER 1 (HOST)
  Opponent: Friend (Online)
  State: $state
  Round: $roundNumber
  Score: Ryan=$p1Score | Friend=$p2Score
""".trimIndent()
        sendSystem(status, "info")
    }

    private fun sendSystem(text: String, level: String) {
        val json = JSONObject().apply {
            put("type", "system")
            put("text", text)
            put("level", level)
        }
        onMessageToClient(json.toString())
    }

    private fun syncState() {
        val json = JSONObject().apply {
            put("type", "state_sync")
            put("state", state)
            put("round", roundNumber)
            put("p1_name", "Ryan")
            put("p1_score", p1Score)
            put("p1_ready", p1Ready)
            put("p1_locked", p1ChosenCard != null)
            put("p1_rank", p1ChosenRank?.code)
            put("p2_name", "Friend")
            put("p2_score", p2Score)
            put("p2_ready", p2Ready)
            put("p2_locked", p2ChosenCard != null)
            put("p2_rank", p2ChosenRank?.code)
        }
        onMessageToClient(json.toString())

        val cardsJson = JSONObject().apply {
            put("type", "cards_sync")
            val countsObj = JSONObject().apply {
                put("rock", p1Cards.rock)
                put("paper", p1Cards.paper)
                put("scissors", p1Cards.scissors)
            }
            put("counts", countsObj)
        }
        onMessageToClient(cardsJson.toString())
    }
}
