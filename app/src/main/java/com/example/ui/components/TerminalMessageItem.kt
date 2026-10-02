package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CardType
import com.example.model.SystemLevel
import com.example.model.TerminalEntry
import com.example.model.WagerRank
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PaperColor
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.RankColorA
import com.example.ui.theme.RankColorB
import com.example.ui.theme.RankColorC
import com.example.ui.theme.RankColorS
import com.example.ui.theme.RockColor
import com.example.ui.theme.ScissorsColor
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBorderHighlight
import com.example.ui.theme.TerminalCardBg
import com.example.ui.theme.TerminalCrimson
import com.example.ui.theme.TerminalDarkSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary

@Composable
fun TerminalMessageItem(
    entry: TerminalEntry,
    modifier: Modifier = Modifier
) {
    when (entry) {
        is TerminalEntry.Chat -> {
            ChatLogItem(entry = entry, modifier = modifier)
        }
        is TerminalEntry.SystemMsg -> {
            SystemLogItem(entry = entry, modifier = modifier)
        }
        is TerminalEntry.CardBox -> {
            CardBoxRevealItem(entry = entry, modifier = modifier)
        }
        is TerminalEntry.RevealOutcome -> {
            RevealOutcomeItem(entry = entry, modifier = modifier)
        }
    }
}

/**
 * Chat message item:
 * - Local player (RIGHT): "> message"
 * - Opponent (LEFT): "< message"
 */
@Composable
private fun ChatLogItem(
    entry: TerminalEntry.Chat,
    modifier: Modifier = Modifier
) {
    val alignment = if (entry.isLocal) Alignment.End else Alignment.Start
    val prefix = if (entry.isLocal) ">" else "<"
    val accentColor = if (entry.isLocal) PhosphorGreen else CyberCyan

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "${entry.senderName.uppercase()} ",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (entry.isLocal) Color(0xFF0D2316) else Color(0xFF0A1F2C))
                .border(
                    width = 1.dp,
                    color = if (entry.isLocal) Color(0xFF144D2B) else Color(0xFF12435A),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$prefix ${entry.text}",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TerminalTextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * System and game event log item:
 * Centered or full-width with distinct bracket styling and color highlights.
 */
@Composable
private fun SystemLogItem(
    entry: TerminalEntry.SystemMsg,
    modifier: Modifier = Modifier
) {
    val tintColor = when (entry.level) {
        SystemLevel.INFO -> CyberCyan
        SystemLevel.WARNING -> TerminalAmber
        SystemLevel.SUCCESS -> PhosphorGreen
        SystemLevel.DANGER -> TerminalCrimson
        SystemLevel.COUNTDOWN -> TerminalAmber
        SystemLevel.HIGHLIGHT -> PhosphorGreen
        SystemLevel.CARDS -> PhosphorGreen
    }

    if (entry.level == SystemLevel.COUNTDOWN || entry.level == SystemLevel.HIGHLIGHT) {
        // Dramatic centered countdown / reveal
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tintColor,
                letterSpacing = 2.sp
            )
        }
    } else {
        // System message container with bracket coloring
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, Color(0xFF161F2E), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val annotated = buildTerminalText(entry.text, tintColor)
            Text(
                text = annotated,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TerminalTextSecondary
            )
        }
    }
}

/**
 * Parses brackets like [ROCK], [PAPER], [SCISSORS], [C], [B], [A], [S]
 * and applies authentic colors.
 */
fun buildTerminalText(text: String, defaultTint: Color) = buildAnnotatedString {
    var cursor = 0
    val bracketRegex = "\\[([^\\]]+)\\]".toRegex()
    val matches = bracketRegex.findAll(text)

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1
        val inside = match.groupValues[1].trim()

        if (start > cursor) {
            val normalText = text.substring(cursor, start)
            if (normalText.contains("[SYSTEM]") || normalText.contains("===")) {
                withStyle(SpanStyle(color = defaultTint, fontWeight = FontWeight.Bold)) {
                    append(normalText)
                }
            } else {
                withStyle(SpanStyle(color = TerminalTextSecondary)) {
                    append(normalText)
                }
            }
        }

        // Color specific tokens
        val tokenColor = when {
            inside.startsWith("ROCK") -> RockColor
            inside.startsWith("PAPER") -> PaperColor
            inside.startsWith("SCISSORS") -> ScissorsColor
            inside == "C" || inside.endsWith(" C") -> RankColorC
            inside == "B" || inside.endsWith(" B") -> RankColorB
            inside == "A" || inside.endsWith(" A") -> RankColorA
            inside == "S" || inside.endsWith(" S") -> RankColorS
            inside == "SYSTEM" -> defaultTint
            inside == "READY" -> PhosphorGreen
            inside == "LOCKED" -> CyberCyan
            inside.contains("??????") -> TerminalAmber
            else -> defaultTint
        }

        withStyle(SpanStyle(color = tokenColor, fontWeight = FontWeight.Bold)) {
            append("[")
            append(inside)
            append("]")
        }

        cursor = end
    }

    if (cursor < text.length) {
        val remaining = text.substring(cursor)
        withStyle(SpanStyle(color = if (remaining.contains("[SYSTEM]") || remaining.contains("===")) defaultTint else TerminalTextSecondary)) {
            append(remaining)
        }
    }
}

/**
 * ASCII Card Box Reveal UI component:
 * Shows the two played cards facing each other.
 */
@Composable
private fun CardBoxRevealItem(
    entry: TerminalEntry.CardBox,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalDarkSurface)
            .border(1.dp, PhosphorGreen, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "══ CARD REVEAL ══",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PhosphorGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Card
                AsciiCardView(
                    playerName = entry.p1Name,
                    cardType = entry.p1Card,
                    rank = entry.p1Rank,
                    isRevealed = entry.isRevealed
                )

                Text(
                    text = "VS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TerminalAmber
                )

                // Player 2 Card
                AsciiCardView(
                    playerName = entry.p2Name,
                    cardType = entry.p2Card,
                    rank = entry.p2Rank,
                    isRevealed = entry.isRevealed
                )
            }
        }
    }
}

@Composable
fun AsciiCardView(
    playerName: String,
    cardType: CardType?,
    rank: WagerRank?,
    isRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    val rankCode = rank?.code ?: "C"
    val rankColor = rank?.color ?: RankColorC
    val cardName = if (isRevealed) (cardType?.displayName ?: "ROCK") else "??????"
    val cardColor = if (isRevealed) (cardType?.color ?: RockColor) else TerminalAmber

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = playerName.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(115.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalCardBg)
                .border(1.dp, rankColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top border line
                Text(
                    text = "┌─────────┐",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = rankColor
                )

                // Content line: "│ ROCK  A │"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "│",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = rankColor
                    )
                    Text(
                        text = cardName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                    Text(
                        text = rankCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankColor
                    )
                    Text(
                        text = "│",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = rankColor
                    )
                }

                // Bottom border line
                Text(
                    text = "└─────────┘",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = rankColor
                )
            }
        }
    }
}

/**
 * Result of round resolution
 */
@Composable
private fun RevealOutcomeItem(
    entry: TerminalEntry.RevealOutcome,
    modifier: Modifier = Modifier
) {
    val winTitle = when (entry.winner) {
        0 -> "IT'S A DRAW!"
        1 -> "PLAYER 1 WINS!"
        2 -> "PLAYER 2 WINS!"
        else -> "ROUND RESOLVED"
    }

    val bannerColor = when (entry.winner) {
        0 -> TerminalAmber
        1 -> PhosphorGreen
        2 -> CyberCyan
        else -> TerminalTextPrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A121E))
            .border(1.dp, bannerColor, RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "═══ $winTitle ═══",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = bannerColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.reason,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TerminalTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "P1: ${entry.p1Score} (${if (entry.p1Delta >= 0) "+${entry.p1Delta}" else "${entry.p1Delta}"})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.p1Delta >= 0) PhosphorGreen else TerminalCrimson
                )

                Text(
                    text = "P2: ${entry.p2Score} (${if (entry.p2Delta >= 0) "+${entry.p2Delta}" else "${entry.p2Delta}"})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.p2Delta >= 0) CyberCyan else TerminalCrimson
                )
            }
        }
    }
}
