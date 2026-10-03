package com.example.ui.components

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.TerminalCardBg
import com.example.ui.theme.TerminalCrimson
import com.example.ui.theme.TerminalDarkSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary

@Composable
fun TerminalMessageItem(
    entry: TerminalEntry,
    showSenderHeader: Boolean = true,
    onReply: (TerminalEntry.Chat) -> Unit = {},
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (entry) {
        is TerminalEntry.Chat -> {
            ChatLogItem(
                entry = entry,
                showSenderHeader = showSenderHeader,
                onReply = { onReply(entry) },
                onDelete = { onDelete(entry.id) },
                modifier = modifier
            )
        }
        is TerminalEntry.SystemMsg -> {
            SystemLogItem(
                entry = entry,
                onDelete = { onDelete(entry.id) },
                modifier = modifier
            )
        }
        is TerminalEntry.Countdown -> {
            CountdownLogItem(
                entry = entry,
                modifier = modifier
            )
        }
        is TerminalEntry.CardBox -> {
            CardBoxRevealItem(
                entry = entry,
                onDelete = { onDelete(entry.id) },
                modifier = modifier
            )
        }
        is TerminalEntry.RevealOutcome -> {
            RevealOutcomeItem(
                entry = entry,
                onDelete = { onDelete(entry.id) },
                modifier = modifier
            )
        }
    }
}

/**
 * Chat message item with:
 * - Grouped sender headers (like Messenger/WhatsApp)
 * - Quoted reply preview if replying to another message
 * - Long press to Reply, Copy, or Delete
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatLogItem(
    entry: TerminalEntry.Chat,
    showSenderHeader: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alignment = if (entry.isLocal) Alignment.End else Alignment.Start
    val prefix = if (entry.isLocal) ">" else "<"
    val accentColor = if (entry.isLocal) PhosphorGreen else CyberCyan
    var showActionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (showSenderHeader) 4.dp else 1.dp, bottom = 1.dp),
        horizontalAlignment = alignment
    ) {
        // Show sender name header ONLY if this is the start of a consecutive message block
        if (showSenderHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = entry.senderName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (entry.isLocal) Color(0xFF0D2517) else Color(0xFF0C2232))
                .border(
                    width = 1.dp,
                    color = if (entry.isLocal) Color(0xFF14542E) else Color(0xFF134A66),
                    shape = RoundedCornerShape(6.dp)
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showActionMenu = true }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                // If this message was a reply to another message, show sleek quote preview
                if (!entry.replyToSender.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF061018))
                            .border(1.dp, Color(0xFF1A3344), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "↩ ${entry.replyToSender}: \"${entry.replyToText ?: ""}\"",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TerminalTextMuted,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = "$prefix ${entry.text}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalTextPrimary,
                    lineHeight = 17.sp
                )
            }
        }
    }

    if (showActionMenu) {
        MessageActionDialog(
            senderName = entry.senderName,
            messageText = entry.text,
            onReply = {
                showActionMenu = false
                onReply()
            },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", entry.text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                showActionMenu = false
            },
            onDelete = {
                showActionMenu = false
                onDelete()
            },
            onDismiss = { showActionMenu = false }
        )
    }
}

/**
 * System and game event log item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemLogItem(
    entry: TerminalEntry.SystemMsg,
    onDelete: () -> Unit,
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

    var showActionMenu by remember { mutableStateOf(false) }

    if (entry.level == SystemLevel.COUNTDOWN || entry.level == SystemLevel.HIGHLIGHT) {
        // Compact, aesthetic centered countdown / reveal
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor,
                letterSpacing = 1.sp
            )
        }
    } else {
        // System message container with bracket coloring
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, Color(0xFF161F2E), RoundedCornerShape(4.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showActionMenu = true }
                )
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            val annotated = buildTerminalText(entry.text, tintColor)
            Text(
                text = annotated,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TerminalTextSecondary
            )
        }
    }

    if (showActionMenu) {
        MessageActionDialog(
            senderName = "SYSTEM",
            messageText = entry.text,
            showReplyOption = false,
            onReply = {},
            onCopy = {
                showActionMenu = false
            },
            onDelete = {
                showActionMenu = false
                onDelete()
            },
            onDismiss = { showActionMenu = false }
        )
    }
}

@Composable
private fun CountdownLogItem(
    entry: TerminalEntry.Countdown,
    modifier: Modifier = Modifier
) {
    val tintColor = when (entry.level) {
        SystemLevel.COUNTDOWN -> PhosphorGreen
        SystemLevel.WARNING -> TerminalAmber
        SystemLevel.DANGER -> TerminalCrimson
        SystemLevel.HIGHLIGHT -> Color(0xFF9B7BFF)
        else -> TerminalTextPrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = entry.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tintColor,
            letterSpacing = 1.sp
        )
    }
}


/**
 * Parses tokens like [ROCK], [PAPER], [SCISSORS], [C], [B], [A], [S]
 * and applies vibrant, aesthetic colors.
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
            if (normalText.contains("[SYSTEM]") || normalText.contains("===") || normalText.contains("═══")) {
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
            inside == "C" || inside.endsWith(" C") || inside.startsWith("C ") -> RankColorC
            inside == "B" || inside.endsWith(" B") || inside.startsWith("B ") -> RankColorB
            inside == "A" || inside.endsWith(" A") || inside.startsWith("A ") -> RankColorA
            inside == "S" || inside.endsWith(" S") || inside.startsWith("S ") -> RankColorS
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
        withStyle(SpanStyle(color = if (remaining.contains("[SYSTEM]") || remaining.contains("===") || remaining.contains("═══")) defaultTint else TerminalTextSecondary)) {
            append(remaining)
        }
    }
}

/**
 * Compact terminal-style card duel reveal
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardBoxRevealItem(
    entry: TerminalEntry.CardBox,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showActionMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalDarkSurface)
            .border(1.dp, PhosphorGreen, RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { showActionMenu = true }
            )
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Player 1
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CompactDuelCard(
                    playerName = entry.p1Name,
                    cardType = entry.p1Card,
                    rank = entry.p1Rank,
                    isRevealed = entry.isRevealed
                )
            }

            // Single sword in the exact center
            Text(
                text = "⚔",
                fontSize = 15.sp,
                color = TerminalTextPrimary,
                modifier = Modifier.padding(horizontal = 5.dp)
            )

            // Player 2
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CompactDuelCard(
                    playerName = entry.p2Name,
                    cardType = entry.p2Card,
                    rank = entry.p2Rank,
                    isRevealed = entry.isRevealed
                )
            }
        }
    }

    if (showActionMenu) {
        MessageActionDialog(
            senderName = "DUEL REVEAL",
            messageText = "${entry.p1Name} vs ${entry.p2Name}",
            showReplyOption = false,
            onReply = {},
            onCopy = {},
            onDelete = {
                showActionMenu = false
                onDelete()
            },
            onDismiss = { showActionMenu = false }
        )
    }
}

@Composable
fun CompactDuelCard(
    playerName: String,
    cardType: CardType?,
    rank: WagerRank?,
    isRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    val rankCode = rank?.code ?: "C"
    val rankColor = rank?.color ?: RankColorC
    val finalCardName = cardType?.displayName ?: "ROCK"
    val finalCardColor = cardType?.color ?: RockColor

    var revealStage by remember(
    cardType,
    rank,
    isRevealed
) {
    mutableIntStateOf(0)
}

var displayedCard by remember(
    cardType,
    rank,
    isRevealed
) {
    mutableStateOf("        ")
}

var displayedWager by remember(
    cardType,
    rank,
    isRevealed
) {
    mutableStateOf(" ")
}

LaunchedEffect(cardType, rank, isRevealed) {
    if (!isRevealed) {
        displayedCard = "        "
        displayedWager = " "
        revealStage = 0
        return@LaunchedEffect
    }

    val target = finalCardName
    val random = kotlin.random.Random

    // Start completely hidden.
    displayedCard = "        "
    displayedWager = " "
    revealStage = 0

    // Tiny suspense before wager.
    delay(180)

    // WAGER FIRST
    displayedWager = " $rankCode "
    revealStage = 1

    delay(140)

    // Card reveal.
    val maxWidth = target.length + 6

    repeat(6) { step ->
        val revealedCount = minOf(step + 1, target.length)

        val revealedPart = target.take(revealedCount)

        val remaining = maxWidth - revealedPart.length

        val leftDots = remaining / 2
        val rightDots = remaining - leftDots

        val left = ".".repeat(leftDots)
        val right = ".".repeat(rightDots)

        // Small randomized character disturbance before settling.
        val middle =
            if (revealedCount < target.length) {
                buildString {
                    append(revealedPart)

                    repeat(target.length - revealedCount) {
                        append(
                            when (random.nextInt(3)) {
                                0 -> '.'
                                1 -> target[random.nextInt(target.length)]
                                else -> '?'
                            }
                        )
                    }
                }
            } else {
                revealedPart
            }

        displayedCard = left + middle + right
        revealStage = 2

        delay(70)
    }

    // Final clean state.
    displayedCard = target
    revealStage = 3
}

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Player name box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalCardBg)
                .border(
                    1.dp,
                    Color(0xFF9EA7AD),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playerName.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextPrimary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Card + wager combined box
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalCardBg)
                .border(
                    1.dp,
                    Color(0xFF9EA7AD),
                    RoundedCornerShape(4.dp)
                )
        ) {

            // Card name section
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedCard,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (revealStage >= 3) {
                        finalCardColor
                    } else {
                        TerminalAmber
                    }
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(25.dp)
                    .background(Color(0xFF9EA7AD))
            )

            // Wager section
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedWager,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (displayedWager == rankCode) {
                        rankColor
                    } else {
                        TerminalAmber
                    }
                )
            }
        }
    }
}
/**
 * Result of round resolution with actual player names and individual chosen score deltas.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RevealOutcomeItem(
    entry: TerminalEntry.RevealOutcome,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winTitle = when (entry.winner) {
        0 -> "IT'S A DRAW!"
        1 -> "${entry.p1Name.uppercase()} WINS!"
        2 -> "${entry.p2Name.uppercase()} WINS!"
        else -> "ROUND RESOLVED"
    }

    val bannerColor = when (entry.winner) {
        0 -> TerminalAmber
        1 -> PhosphorGreen
        2 -> CyberCyan
        else -> TerminalTextPrimary
    }

    var showActionMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A121E))
            .border(1.dp, bannerColor, RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { showActionMenu = true }
            )
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "═══ $winTitle ═══",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = bannerColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = entry.reason,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TerminalTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${entry.p1Name}: ${entry.p1Score} (${if (entry.p1Delta >= 0) "+${entry.p1Delta}" else "${entry.p1Delta}"})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.p1Delta >= 0) PhosphorGreen else TerminalCrimson
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${entry.p2Name}: ${entry.p2Score} (${if (entry.p2Delta >= 0) "+${entry.p2Delta}" else "${entry.p2Delta}"})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.p2Delta >= 0) CyberCyan else TerminalCrimson
                )
            }
        }
    }

    if (showActionMenu) {
        MessageActionDialog(
            senderName = "ROUND RESULT",
            messageText = entry.reason,
            showReplyOption = false,
            onReply = {},
            onCopy = {},
            onDelete = {
                showActionMenu = false
                onDelete()
            },
            onDismiss = { showActionMenu = false }
        )
    }
}

/**
 * Sleek modal dialog triggered on message long-press
 * (Reply, Copy, Delete)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionDialog(
    senderName: String,
    messageText: String,
    showReplyOption: Boolean = true,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalDarkSurface)
                .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MESSAGE ACTIONS // $senderName",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"${messageText.take(60)}${if (messageText.length > 60) "..." else ""}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TerminalTextSecondary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (showReplyOption) {
                    ActionRow(
                        icon = Icons.Default.Reply,
                        label = "REPLY TO MESSAGE",
                        color = CyberCyan,
                        onClick = onReply
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ActionRow(
                    icon = Icons.Default.ContentCopy,
                    label = "COPY TEXT",
                    color = PhosphorGreen,
                    onClick = onCopy
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionRow(
                    icon = Icons.Default.Delete,
                    label = "DELETE FROM LOG",
                    color = TerminalCrimson,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
