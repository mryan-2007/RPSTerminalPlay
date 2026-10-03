package com.example.network

import com.example.model.CardCounts
import com.example.model.CardType
import com.example.model.WagerRank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

/**
 * Local battle engine running entirely on-device.
 * Simulates an authentic game server with bot opponent, individual wagers,
 * and aesthetic compact reveals.
 */
class LocalGameEngine(
    val localPlayerName: String = "Player",
    val opponentName: String = "Bot",
    private val scope: CoroutineScope,
    private val onMessageToClient: (String) -> Unit
) {
    private var state = "WAITING_FOR_START"
    private var roundNumber = 1
    private var p1Score = 50
    private var p2Score = 50

    private var p1Cards = CardCounts(2, 2, 2)
    private var p2Cards = CardCounts(2, 2, 2)

    private var p1ChosenCard: CardType? = null
    private var p1ChosenRank: WagerRank? = null
    private var p1PendingCard: CardType? = null

    private var p2ChosenCard: CardType? = null
    private var p2ChosenRank: WagerRank? = null

    private var p1Ready = false
    private var p2Ready = false

    private var activeSequenceJob: Job? = null

    init {
        sendInitialState()
    }

    private fun sendInitialState() {
        val handshakeAck = JSONObject().apply {
            put("type", "handshake_ack")
            put("player_id", "p_host")
            put("role", "player1")
            put("name", localPlayerName)
            put("status", "connected")
        }
        onMessageToClient(handshakeAck.toString())

        val welcomeMsg = JSONObject().apply {
            put("type", "system")
            put("text", "[SYSTEM] Connected to Local Battle Engine.\n[SYSTEM] $opponentName is ready.\n[SYSTEM] Type /start to begin Round 1.")
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
                    put("sender_name", localPlayerName)
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
                            "Choose your rank wisely.",
                            "May the best RPS strategist win.",
                            "Locked and loaded."
                        )
                        val opponentChat = JSONObject().apply {
                            put("type", "chat")
                            put("sender_id", "p_opp")
                            put("sender_role", "player2")
                            put("sender_name", opponentName)
                            put("text", replies.random())
                        }
                        onMessageToClient(opponentChat.toString())
                    }
                }
            }
        }
    }

    private fun handleConversationalSelection(text: String): Boolean {
        if (state != "WAITING_FOR_CHOICES") return false
        if (p1ChosenCard != null) return false

        val parts = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Combined: e.g. "rock A", "r A", "p S", "s C"
        if (parts.size == 2) {
            val card = CardType.fromString(parts[0])
            val rank = WagerRank.fromString(parts[1])
            if (card != null && rank != null) {
                lockLocalChoice(card, rank)
                return true
            }
        }

        // Just card: e.g. "rock", "r", "paper", "p", "scissors", "s"
        if (parts.size == 1) {
            val card = CardType.fromString(parts[0])
            if (card != null) {
                if (!p1Cards.hasAny(card)) {
                    sendSystem("[SYSTEM] No remaining ${card.displayName} cards! Choose from available cards.", "danger")
                    return true
                }
                p1PendingCard = card
                sendSystem(
                    "[CARD SELECTED: ${card.displayName}]\n" +
                    "Now choose your wager rank:\n" +
                    "  [C]  3 pts   - Minimal risk\n" +
                    "  [B]  7 pts   - Normal wager\n" +
                    "  [A]  15 pts  - High stakes\n" +
                    "  [S]  25 pts  - Supreme gamble\n" +
                    "Type: C, B, A, or S (Your score: $p1Score)",
                    "info"
                )
                return true
            }
        }

        // Just rank after card is selected
        if (parts.size == 1 && p1PendingCard != null) {
            val rank = WagerRank.fromString(parts[0])
            if (rank != null) {
                val card = p1PendingCard!!
                p1PendingCard = null
                lockLocalChoice(card, rank)
                return true
            }
        }

        return false
    }

    private fun handleCommand(cmd: String) {
        val parts = cmd.trim().split("\\s+".toRegex())
        val action = parts[0].lowercase()

        when (action) {
            "/start" -> handleStart()
            "/cards" -> handleCards()
            "/choose" -> {
                if (parts.size >= 3) {
                    val card = CardType.fromString(parts[1])
                    val rank = WagerRank.fromString(parts[2])
                    if (card != null && rank != null) {
                        lockLocalChoice(card, rank)
                    } else {
                        sendSystem("[SYSTEM] Usage: /choose <rock|paper|scissors|r|p|s> <C|B|A|S>", "danger")
                    }
                } else {
                    sendSystem("[SYSTEM] Usage: /choose <rock|paper|scissors|r|p|s> <C|B|A|S>", "danger")
                }
            }
            "/status" -> handleStatus()
            "/help" -> handleHelp()
            else -> sendSystem("[SYSTEM] Unknown command '$action'. Type /help for assistance.", "warning")
        }
    }

    private fun handleStart() {
        if (state !in listOf("WAITING_FOR_START", "NEXT_ROUND", "CONNECTED")) {
            sendSystem("[SYSTEM] Round already active (State: $state).", "warning")
            return
        }

        p1Ready = true
        p2Ready = true
        sendSystem("[SYSTEM] Both players ready. Starting Round $roundNumber!", "info")

        scope.launch {
            delay(500)
            startRound()
        }
    }

    private fun startRound() {
        state = "WAITING_FOR_CHOICES"
        p1ChosenCard = null
        p1ChosenRank = null
        p1PendingCard = null
        p2ChosenCard = null
        p2ChosenRank = null

        val banner = "\n════════════════════════════════════════\n" +
                     "           ROUND $roundNumber STARTING\n" +
                     "════════════════════════════════════════"
        sendSystem(banner, "success")

        val promptMsg = "YOUR AVAILABLE CARDS:\n" +
                        "${formatCardsBracket()}\n\n" +
                        "Type card: r (rock), p (paper), s (scissors)\n" +
                        "Or combined: e.g. 'r A' or 'rock B'"
        sendSystem(promptMsg, "cards")

        syncState()

        // Simulate opponent choice with smart delay
        simulateOpponentChoice()
    }

    private fun formatCardsBracket(): String {
        val parts = mutableListOf<String>()
        repeat(p1Cards.rock) { parts.add("[ROCK]") }
        repeat(p1Cards.paper) { parts.add("[PAPER]") }
        repeat(p1Cards.scissors) { parts.add("[SCISSORS]") }
        return if (parts.isEmpty()) "[NO CARDS]" else parts.joinToString(" ")
    }

    private fun simulateOpponentChoice() {
        scope.launch {
            delay(Random.nextLong(1500, 3000))
            if (state != "WAITING_FOR_CHOICES") return@launch

            val availableTypes = mutableListOf<CardType>()
            if (p2Cards.rock > 0) availableTypes.add(CardType.ROCK)
            if (p2Cards.paper > 0) availableTypes.add(CardType.PAPER)
            if (p2Cards.scissors > 0) availableTypes.add(CardType.SCISSORS)

            val chosenType = if (availableTypes.isNotEmpty()) availableTypes.random() else CardType.ROCK

            val affordableRanks = WagerRank.entries.filter { it.points <= p2Score }
            val chosenRank = if (affordableRanks.isNotEmpty()) affordableRanks.random() else WagerRank.C

            p2ChosenCard = chosenType
            p2ChosenRank = chosenRank

            when (chosenType) {
                CardType.ROCK -> p2Cards = p2Cards.copy(rock = (p2Cards.rock - 1).coerceAtLeast(0))
                CardType.PAPER -> p2Cards = p2Cards.copy(paper = (p2Cards.paper - 1).coerceAtLeast(0))
                CardType.SCISSORS -> p2Cards = p2Cards.copy(scissors = (p2Cards.scissors - 1).coerceAtLeast(0))
            }

            sendSystem("[SYSTEM] $opponentName locked card: [ ??????  ${chosenRank.code} ]", "info")
            syncState()

            checkBothLocked()
        }
    }

    private fun lockLocalChoice(cardType: CardType, rank: WagerRank) {
        if (state != "WAITING_FOR_CHOICES") {
            sendSystem("[SYSTEM] No active round. Type /start when ready.", "warning")
            return
        }

        if (p1ChosenCard != null) {
            sendSystem("[SYSTEM] You already locked your card for this round.", "warning")
            return
        }

        if (!p1Cards.hasAny(cardType)) {
            sendSystem("[SYSTEM] You have no remaining ${cardType.displayName} cards!", "danger")
            return
        }

        if (rank.points > p1Score) {
            sendSystem("[SYSTEM] Cannot afford wager ${rank.code} (${rank.points} pts)! Your score is $p1Score.", "danger")
            return
        }

        p1ChosenCard = cardType
        p1ChosenRank = rank

        when (cardType) {
            CardType.ROCK -> p1Cards = p1Cards.copy(rock = p1Cards.rock - 1)
            CardType.PAPER -> p1Cards = p1Cards.copy(paper = p1Cards.paper - 1)
            CardType.SCISSORS -> p1Cards = p1Cards.copy(scissors = p1Cards.scissors - 1)
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
            sendSystem("\n[SYSTEM] BOTH CARDS LOCKED.", "info")

            // Terminal countdown: 3 → 2 → 1 → REVEAL
        delay(400)
        sendCountdown("3", "countdown_3")
        delay(1000)
        sendCountdown("2", "countdown_2")
        delay(1000)
        sendCountdown("1", "countdown_1")
        delay(1000)
        sendCountdown("REVEAL", "countdown_reveal")
        delay(400)

            val p1Card = p1ChosenCard ?: CardType.ROCK
            val p1Rk = p1ChosenRank ?: WagerRank.C
            val p2Card = p2ChosenCard ?: CardType.ROCK
            val p2Rk = p2ChosenRank ?: WagerRank.C

            // Structured card-box payload.
            // The Android UI will render this responsively instead of using ASCII.
            val cardBoxPayload = JSONObject().apply {
                put("type", "card_box")
                put("p1_name", localPlayerName)
                put("p1_card", p1Card.code)
                put("p1_rank", p1Rk.code)
                put("p2_name", opponentName)
                put("p2_card", p2Card.code)
                put("p2_rank", p2Rk.code)
                put("is_revealed", true)
            }

onMessageToClient(cardBoxPayload.toString())

            // Determine winner
            val winner = determineWinner(p1Card, p2Card)
            val p1Delta: Int
            val p2Delta: Int
            val outcomeText: String
            val reason: String

            if (winner == 0) {
                p1Delta = 0
                p2Delta = 0
                reason = "BOTH CHOSE ${p1Card.displayName}. IT'S A TIE!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] DRAW! Wagers returned (0 pts)."
            } else if (winner == 1) {
                // Each player's score changes strictly by their OWN chosen wager
                p1Delta = p1Rk.points
                p2Delta = -p2Rk.points
                p1Score += p1Delta
                p2Score += p2Delta
                reason = "${p1Card.displayName} CRUSHES ${p2Card.displayName}!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] $localPlayerName WINS!\n[SYSTEM] $localPlayerName +$p1Delta | $opponentName $p2Delta"
            } else {
                p1Delta = -p1Rk.points
                p2Delta = p2Rk.points
                p1Score += p1Delta
                p2Score += p2Delta
                reason = "${p2Card.displayName} CRUSHES ${p1Card.displayName}!"
                outcomeText = "[SYSTEM] $reason\n[SYSTEM] $opponentName WINS!\n[SYSTEM] $opponentName +$p2Delta | $localPlayerName $p1Delta"
            }

            // Structured reveal payload
            val revealPayload = JSONObject().apply {
                put("type", "reveal")
                put("p1_name", localPlayerName)
                put("p2_name", opponentName)
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
        val msg = "YOUR AVAILABLE CARDS:\n${formatCardsBracket()}\n" +
                  "Cards remaining: ${p1Cards.rock} Rock, ${p1Cards.paper} Paper, ${p1Cards.scissors} Scissors."
        sendSystem(msg, "cards")
    }

    private fun handleHelp() {
        val help = """
[SYSTEM] COMMANDS & SHORTCUTS:
  r / rock     - Play Rock (🪨 beats ✂️)
  p / paper    - Play Paper (📄 beats 🪨)
  s / scissors - Play Scissors (✂️ beats 📄)
  c, b, a, s   - Choose Wager Rank
  r A, p B     - Play card & rank combined
  /start       - Begin the round
  /cards       - View remaining deck
  /status      - Game state & scores
""".trimIndent()
        sendSystem(help, "info")
    }

    private fun handleStatus() {
        val status = """
[SYSTEM] STATUS:
  Player : $localPlayerName
  Opponent: $opponentName (Local)
  State   : $state
  Round   : $roundNumber
  Score   : $localPlayerName=$p1Score | $opponentName=$p2Score
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

    private fun sendCountdown(text: String, level: String) {
        val json = JSONObject().apply {
            put("type", "countdown")
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
            put("p1_name", localPlayerName)
            put("p1_score", p1Score)
            put("p1_ready", p1Ready)
            put("p1_locked", p1ChosenCard != null)
            put("p1_rank", p1ChosenRank?.code)
            put("p2_name", opponentName)
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
