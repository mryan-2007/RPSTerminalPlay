package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBorderHighlight
import com.example.ui.theme.TerminalCardBg
import com.example.ui.theme.TerminalDarkSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConnectDialog(
    initialAddress: String,
    initialPlayerName: String,
    onConnectTermux: (address: String, name: String) -> Unit,
    onStartLocalEngine: (name: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var address by remember { mutableStateOf(initialAddress) }
    var playerName by remember { mutableStateOf(initialPlayerName) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("server_connect_dialog")
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalDarkSurface)
                .border(1.dp, PhosphorGreen, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lan,
                            contentDescription = null,
                            tint = PhosphorGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NETWORK LINK CONFIG",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PhosphorGreen
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TerminalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Player Name Field
                Text(
                    text = "OPERATIVE CALLSIGN (NAME):",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalCardBg)
                        .border(1.dp, TerminalBorderHighlight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TerminalTextPrimary
                        ),
                        cursorBrush = SolidColor(PhosphorGreen),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("player_name_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Server Address Field
                Text(
                    text = "SERVER ADDRESS (WS:// OR WSS://):",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalCardBg)
                        .border(1.dp, TerminalBorderHighlight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = address,
                        onValueChange = { address = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TerminalTextPrimary
                        ),
                        cursorBrush = SolidColor(PhosphorGreen),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().testTag("server_address_input")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Local: ws://192.168.x.x:8765 | Online: wss://tunnel-url",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TerminalTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("ws://127.0.0.1:8765", onClick = { address = "ws://127.0.0.1:8765" })
                    PresetChip("ws://10.0.2.2:8765", onClick = { address = "ws://10.0.2.2:8765" })
                    PresetChip("Pinggy wss://", onClick = { address = "wss://" })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Connect Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PhosphorGreen.copy(alpha = 0.2f))
                        .border(1.dp, PhosphorGreen, RoundedCornerShape(4.dp))
                        .clickable {
                            onConnectTermux(address, playerName)
                            onDismiss()
                        }
                        .padding(10.dp)
                        .testTag("connect_termux_action_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CONNECT TO TERMUX SERVER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PhosphorGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Local Engine Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                        .clickable {
                            onStartLocalEngine(playerName)
                            onDismiss()
                        }
                        .padding(10.dp)
                        .testTag("local_engine_action_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PLAY LOCAL BATTLE ENGINE (SOLO / DEMO)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalCardBg)
            .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TerminalTextMuted
        )
    }
}
