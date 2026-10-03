package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalEntry
import com.example.ui.components.ServerConnectDialog
import com.example.ui.components.TerminalHeader
import com.example.ui.components.TerminalInputBar
import com.example.ui.components.TerminalMessageItem
import com.example.ui.components.TerminalScoreBoard
import com.example.ui.components.TermuxGuideDialog
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalTextMuted
import com.example.viewmodel.GameViewModel

@Composable
fun TerminalScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showConnectDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Auto-scroll to bottom whenever a new log message arrives or input changes
    LaunchedEffect(uiState.terminalEntries.size) {
        if (uiState.terminalEntries.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalEntries.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBlack),
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = TerminalBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Only the bottom input bar rides up with the keyboard!
                .navigationBarsPadding()
                .background(TerminalBlack)
        ) {
            // Pinned top header bar
            TerminalHeader(
                uiState = uiState,
                onOpenConnectDialog = { showConnectDialog = true },
                onOpenGuideDialog = { showGuideDialog = true },
                onClearTerminal = { viewModel.clearTerminalLog() }
            )

            // Score Board (shown conditionally only when in active 2-player game)
            TerminalScoreBoard(uiState = uiState)

            // Terminal Chat / Battle Log (fills available height, smoothly resizes with keyboard)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (uiState.terminalEntries.isEmpty()) {
                    // Empty state clean welcome
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "╔══════════════════════════════════════╗\n" +
                                   "║       RPS // BATTLE TERMINAL         ║\n" +
                                   "╚══════════════════════════════════════╝",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = PhosphorGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "COMMAND PROTOCOL ACTIVE.\nType /local to play solo, /help for guide,\nor tap 🌐 to connect online.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("terminal_log_list"),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.terminalEntries,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            // Message grouping (like Messenger / WhatsApp):
                            // If previous message was from the same sender, do not repeat the sender name header!
                            val showSenderHeader = if (entry is TerminalEntry.Chat && index > 0) {
                                val prev = uiState.terminalEntries[index - 1]
                                !(prev is TerminalEntry.Chat && prev.senderName == entry.senderName && prev.isLocal == entry.isLocal)
                            } else {
                                true
                            }

                            TerminalMessageItem(
                                entry = entry,
                                showSenderHeader = showSenderHeader,
                                onReply = { chat -> viewModel.setReplyingTo(chat) },
                                onDelete = { id -> viewModel.deleteMessage(id) }
                                
                            )
                        }
                    }
                }
            }

            // Input Bar at the bottom (with Reply banner preview and EXE button)
            TerminalInputBar(
                value = uiState.inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                onSubmit = { viewModel.submitCurrentInput() },
                replyingTo = uiState.replyingTo,
                onCancelReply = { viewModel.cancelReply() }
            )
        }
    }

    // Connect Dialog
    if (showConnectDialog) {
        ServerConnectDialog(
            initialAddress = uiState.serverAddress,
            initialPlayerName = uiState.localPlayerName,
            onConnectTermux = { address, name ->
                viewModel.connectToTermuxServer(address, name)
            },
            onStartLocalEngine = { name ->
                viewModel.startLocalEngine(name)
            },
            onDismiss = { showConnectDialog = false }
        )
    }

    // Guide Dialog
    if (showGuideDialog) {
        TermuxGuideDialog(
            onDismiss = { showGuideDialog = false }
        )
    }
}
