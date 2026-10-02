package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCardBg
import com.example.ui.theme.TerminalDarkSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxGuideDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Online / Far Away since user asked

    val termuxBasicCommands = """
pkg update && pkg upgrade -y
pkg install python git -y
pip install websockets
python server.py
""".trimIndent()

    val pinggyCommand = "ssh -p 443 -R0:localhost:8765 a.pinggy.io"
    val cloudflaredCommands = """
pkg install cloudflared -y
cloudflared tunnel --url http://localhost:8765
""".trimIndent()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("termux_guide_dialog")
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalDarkSurface)
                .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SERVER & ONLINE GUIDE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CyberCyan
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

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher: [Same Wi-Fi] vs [Play Online (Far Away)]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalCardBg)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selectedTab == 1) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (selectedTab == 1) CyberCyan else TerminalTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ONLINE (FAR AWAY)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) CyberCyan else TerminalTextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selectedTab == 0) PhosphorGreen.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (selectedTab == 0) PhosphorGreen else TerminalTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SAME WI-FI (LAN)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) PhosphorGreen else TerminalTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 1) {
                    // TAB 1: PLAY ONLINE OVER INTERNET (FAR AWAY)
                    Text(
                        text = "HOW TO PLAY OVER THE INTERNET",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Because you and your friend are on different networks, mobile phones cannot directly see each other without a tunnel or VPN. Here are the 3 easiest free solutions:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option A: Pinggy Tunnel (Easiest, zero install)
                    StepSection(
                        step = "A",
                        title = "Pinggy Tunnel (Instant, No Install!)",
                        detail = "In Termux, while server.py is running in another session, run this single SSH command to get a free public link:"
                    )

                    CopyCommandCard(
                        command = pinggyCommand,
                        label = "COPY PINGGY COMMAND",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Termux will output a public URL like: https://xyz.a.pinggy.link\nSend that URL to your friend. In the app, connect using:\nwss://xyz.a.pinggy.link",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = PhosphorGreen,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option B: Cloudflare Tunnel
                    StepSection(
                        step = "B",
                        title = "Cloudflare Tunnel (Free & Unlimited)",
                        detail = "Create a free Cloudflare tunnel directly to your Termux server:"
                    )

                    CopyCommandCard(
                        command = cloudflaredCommands,
                        label = "COPY CLOUDFLARE COMMANDS",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gives a URL like: https://xxxx.trycloudflare.com\nConnect in app using: wss://xxxx.trycloudflare.com",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyberCyan,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option C: Tailscale
                    StepSection(
                        step = "C",
                        title = "Tailscale (Free Mesh VPN)",
                        detail = "1. Both you and your friend install the free 'Tailscale' app from Google Play.\n2. Log in with Google.\n3. Your friend enters your 100.x.y.z IP in the app: ws://100.x.y.z:8765"
                    )

                } else {
                    // TAB 0: SAME WI-FI (LAN)
                    Text(
                        text = "Follow these steps on Host phone:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TerminalTextPrimary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StepSection(
                        step = "1",
                        title = "Install Termux on Android",
                        detail = "Get Termux from F-Droid or GitHub. Open Termux."
                    )

                    StepSection(
                        step = "2",
                        title = "Run Setup Commands in Termux",
                        detail = "Install Python and start server.py:"
                    )

                    CopyCommandCard(
                        command = termuxBasicCommands,
                        label = "COPY SETUP COMMANDS",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StepSection(
                        step = "3",
                        title = "Connect Friend",
                        detail = "Both phones connect to same Wi-Fi (or Host hotspot). Look at the Host Local IP printed in Termux (e.g. 192.168.1.15). In the app on Player 2's phone, enter ws://192.168.1.15:8765."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.2f))
                        .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                        .clickable(onClick = onDismiss)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyCommandCard(
    command: String,
    label: String,
    context: Context,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalCardBg)
            .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = PhosphorGreen,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Command", command))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun StepSection(
    step: String,
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[$step]",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalAmber
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TerminalTextSecondary,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}
