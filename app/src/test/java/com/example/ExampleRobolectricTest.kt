package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CardCounts
import com.example.model.CardType
import com.example.model.TerminalEntry
import com.example.model.WagerRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RPS Terminal Battle", appName)
    }

    @Test
    fun `test card deck counts and deductions`() {
        var deck = CardCounts(rock = 2, paper = 2, scissors = 2)
        assertEquals(6, deck.total)
        assertTrue(deck.hasAny(CardType.ROCK))

        // Play one rock
        deck = deck.copy(rock = deck.rock - 1)
        assertEquals(1, deck.rock)
        assertEquals(5, deck.total)

        // Play second rock
        deck = deck.copy(rock = deck.rock - 1)
        assertEquals(0, deck.rock)
        assertFalse(deck.hasAny(CardType.ROCK))
    }

    @Test
    fun `test wager ranks points`() {
        assertEquals(3, WagerRank.C.points)
        assertEquals(7, WagerRank.B.points)
        assertEquals(15, WagerRank.A.points)
        assertEquals(25, WagerRank.S.points)
    }

    @Test
    fun `test shorthand card names r, p, s and small letters`() {
        assertEquals(CardType.ROCK, CardType.fromString("r"))
        assertEquals(CardType.ROCK, CardType.fromString("rock"))
        assertEquals(CardType.ROCK, CardType.fromString("ROCK"))

        assertEquals(CardType.PAPER, CardType.fromString("p"))
        assertEquals(CardType.PAPER, CardType.fromString("paper"))
        assertEquals(CardType.PAPER, CardType.fromString("PAPER"))

        assertEquals(CardType.SCISSORS, CardType.fromString("s"))
        assertEquals(CardType.SCISSORS, CardType.fromString("scissors"))
        assertEquals(CardType.SCISSORS, CardType.fromString("SCISSORS"))
    }

    @Test
    fun `test individual wager asymmetric scoring`() {
        // Player 1 wagers A (15 pts), Player 2 wagers B (7 pts)
        val p1Wager = WagerRank.A.points // 15
        val p2Wager = WagerRank.B.points // 7

        var p1Score = 50
        var p2Score = 50

        // If Player 1 wins: Player 1 gets +15, Player 2 loses -7
        val p1Delta = p1Wager
        val p2Delta = -p2Wager

        p1Score += p1Delta
        p2Score += p2Delta

        assertEquals(65, p1Score)
        assertEquals(43, p2Score)
    }

    @Test
    fun `test chat reply model fields`() {
        val chat = TerminalEntry.Chat(
            id = UUID.randomUUID().toString(),
            text = "Nice move!",
            senderRole = "player1",
            senderName = "Alex",
            isLocal = true,
            replyToSender = "Sam",
            replyToText = "Good luck"
        )

        assertEquals("Alex", chat.senderName)
        assertEquals("Sam", chat.replyToSender)
        assertEquals("Good luck", chat.replyToText)
    }

    @Test
    fun `test card box model with round number`() {
        val box = TerminalEntry.CardBox(
            id = UUID.randomUUID().toString(),
            roundNumber = 3,
            p1Name = "Alex",
            p1Card = CardType.ROCK,
            p1Rank = WagerRank.A,
            p2Name = "Sam",
            p2Card = CardType.SCISSORS,
            p2Rank = WagerRank.B,
            isRevealed = true
        )

        assertEquals(3, box.roundNumber)
        assertEquals("Alex", box.p1Name)
        assertEquals(CardType.ROCK, box.p1Card)
        assertEquals(WagerRank.A, box.p1Rank)
        assertTrue(box.isRevealed)
    }
}
