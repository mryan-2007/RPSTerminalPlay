package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameUiState
import com.example.model.PlayerRole
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PhosphorGreen
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
fun TerminalScoreBoard(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalDarkSurface)
            .border(width = 1.dp, color = TerminalBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("terminal_scoreboard")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ASCII Top line with Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCORE // ROUND ${uiState.roundNumber}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalAmber
                )

                // State indicator badge
                Text(
                    text = "[${uiState.stateName.replace('_', ' ')}]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = when (uiState.stateName) {
                        "WAITING_FOR_CHOICES" -> CyberCyan
                        "BOTH_CHOICES_LOCKED", "REVEAL" -> PhosphorGreen
                        else -> TerminalTextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Score Row: P1 vs P2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 (Host / Left box)
                PlayerScoreCard(
                    playerName = uiState.player1Name,
                    roleLabel = "PLAYER 1",
                    score = uiState.player1Score,
                    isReady = uiState.player1Ready,
                    isLocked = uiState.player1Locked,
                    wagerRank = uiState.player1Rank?.code,
                    isLocal = uiState.role == PlayerRole.PLAYER1,
                    modifier = Modifier.weight(1f)
                )

                // VS Divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "VS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextMuted
                    )
                }

                // Player 2 (Client / Right box)
                PlayerScoreCard(
                    playerName = uiState.player2Name,
                    roleLabel = "PLAYER 2",
                    score = uiState.player2Score,
                    isReady = uiState.player2Ready,
                    isLocked = uiState.player2Locked,
                    wagerRank = uiState.player2Rank?.code,
                    isLocal = uiState.role == PlayerRole.PLAYER2,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    playerName: String,
    roleLabel: String,
    score: Int,
    isReady: Boolean,
    isLocked: Boolean,
    wagerRank: String?,
    isLocal: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isLocked) PhosphorGreen else if (isReady) CyberCyan else TerminalBorder,
        animationSpec = tween(300),
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalCardBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocal) PhosphorGreen else CyberCyan,
                    maxLines = 1
                )

                if (isLocal) {
                    Text(
                        text = "(YOU)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = PhosphorGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$score",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TerminalTextPrimary
                )

                // Status chip
                val statusText = when {
                    isLocked -> if (wagerRank != null) "[$wagerRank]" else "[LOCKED]"
                    isReady -> "[READY]"
                    else -> "[WAITING]"
                }

                val statusColor = when {
                    isLocked -> PhosphorGreen
                    isReady -> CyberCyan
                    else -> TerminalTextMuted
                }

                Text(
                    text = statusText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
