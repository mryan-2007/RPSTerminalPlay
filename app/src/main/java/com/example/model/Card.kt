package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PaperColor
import com.example.ui.theme.RankColorA
import com.example.ui.theme.RankColorB
import com.example.ui.theme.RankColorC
import com.example.ui.theme.RankColorS
import com.example.ui.theme.RockColor
import com.example.ui.theme.ScissorsColor

enum class CardType(val code: String, val displayName: String) {
    ROCK("rock", "ROCK"),
    PAPER("paper", "PAPER"),
    SCISSORS("scissors", "SCISSORS");

    val color: Color
        get() = when (this) {
            ROCK -> RockColor
            PAPER -> PaperColor
            SCISSORS -> ScissorsColor
        }

    companion object {
        fun fromString(value: String): CardType? {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.code == clean || it.name.lowercase() == clean }
        }
    }
}

enum class WagerRank(val code: String, val points: Int, val description: String) {
    C("C", 3, "Minimal Risk (+3 / -3)"),
    B("B", 7, "Standard Wager (+7 / -7)"),
    A("A", 15, "High Stakes (+15 / -15)"),
    S("S", 25, "Supreme Gamble (+25 / -25)");

    val color: Color
        get() = when (this) {
            C -> RankColorC
            B -> RankColorB
            A -> RankColorA
            S -> RankColorS
        }

    companion object {
        fun fromString(value: String): WagerRank? {
            val clean = value.trim().uppercase()
            return entries.firstOrNull { it.code == clean }
        }
    }
}

data class CardItem(
    val id: String,
    val type: CardType,
    val rarity: String = "COMMON"
)

data class CardCounts(
    val rock: Int = 2,
    val paper: Int = 2,
    val scissors: Int = 2
) {
    val total: Int get() = rock + paper + scissors

    fun countFor(type: CardType): Int = when (type) {
        CardType.ROCK -> rock
        CardType.PAPER -> paper
        CardType.SCISSORS -> scissors
    }

    fun hasAny(type: CardType): Boolean = countFor(type) > 0
}
