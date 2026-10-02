package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CardCounts
import com.example.model.CardType
import com.example.model.WagerRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
