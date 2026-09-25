package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduledMessageEntity
import com.example.ui.MainUiState
import com.example.ui.components.ProBadge
import com.example.ui.components.SafeguardBanner
import com.example.ui.components.StatCard
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.IndiaChakraBlue
import com.example.ui.theme.IndiaChakraBlueLight
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndiaSaffron
import com.example.ui.theme.IndiaWhite
import com.example.ui.theme.ProGold
import com.example.ui.theme.WhatsAppAccentMint
import com.example.ui.theme.WhatsAppDarkSurfaceVariant
import com.example.ui.theme.WhatsAppGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onScheduleClick: () -> Unit,
    onOpenPaywall: () -> Unit,
    onNavigateToAutomations: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDispatchMessage: (Context, ScheduledMessageEntity) -> Unit,
    onCancelMessage: (Long) -> Unit,
    onDeleteMessage: (Long) -> Unit
) {
    val context = LocalContext.current
    val pendingMessages = uiState.scheduledMessages.filter { it.status == "PENDING" }
    val activeWorkflowsCount = uiState.workflows.count { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Top Header
        // Header with Indian Greeting
        item {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Shubh Prabhat 🙏 • Good Morning"
                hour < 17 -> "Shubh Madhyahn 🙏 • Good Afternoon"
                else -> "Shubh Sandhya 🙏 • Good Evening"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "WA Automation Pro",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IndiaSaffron
                    )
                }
                ProBadge(
                    isPro = uiState.isPremium,
                    onClick = onOpenPaywall
                )
            }
        }

        // Indian Theme Bharat Edition Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndiaSaffron.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Tiranga gradient bar
                    Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaSaffron))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaWhite))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaGreen))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(IndiaSaffron.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🇮🇳", fontSize = 20.sp)
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Bharat Edition v4.0",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IndiaSaffron
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = IndiaChakraBlue.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, IndiaChakraBlue.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "+91 Ready",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IndiaChakraBlueLight,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Indian Business, Festivals & Fast +91 WhatsApp Dialing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Service Status Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WhatsAppGreenPrimary.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreenPrimary)
                        )
                        Column {
                            Text(
                                text = "WhatsApp Automation Engine: Ready",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Exact Alarm & WorkManager schedules enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification Settings",
                            tint = WhatsAppGreenPrimary
                        )
                    }
                }
            }
        }

        // Metrics Grid (2x2 + Audit Flow)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "SCHEDULED",
                        value = "${pendingMessages.size}",
                        subtitle = "Pending delivery",
                        icon = Icons.Default.Schedule,
                        iconTint = WhatsAppGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "SENT TODAY",
                        value = "${uiState.todaySentCount}",
                        subtitle = "Under daily limit",
                        icon = Icons.AutoMirrored.Filled.Send,
                        iconTint = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "DELETED RECOVERED",
                        value = "${uiState.vaultDeletedCount}",
                        subtitle = "WhatsApp revoked msgs",
                        icon = Icons.Default.DeleteSweep,
                        iconTint = ProGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "CONTACTS LOADED",
                        value = "${uiState.contacts.size}",
                        subtitle = "AI / Excel / PDF / Phone",
                        icon = Icons.Default.Contacts,
                        iconTint = WhatsAppAccentMint,
                        modifier = Modifier.weight(1f)
                    )
                }

                // In/Out Message Flow Banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WhatsAppDarkSurfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Message Audit Timeline",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "📥 ${uiState.vaultIncomingCount} In",
                                fontSize = 11.sp,
                                color = InfoBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "📤 ${uiState.vaultOutgoingCount} Out",
                                fontSize = 11.sp,
                                color = WhatsAppGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Anti-spam Safeguard Banner
        item {
            SafeguardBanner(
                dailySent = uiState.todaySentCount,
                dailyLimit = uiState.dailySafetyLimit,
                delaySec = uiState.delayBetweenMessagesSec,
                onConfigure = onNavigateToSettings
            )
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "Schedule",
                    color = IndiaSaffron,
                    modifier = Modifier.weight(1f),
                    onClick = onScheduleClick
                )
                QuickActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "Import AI",
                    color = IndiaGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMessages
                )
                QuickActionButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "AI Rewriter",
                    color = ProGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMessages
                )
                QuickActionButton(
                    icon = Icons.Default.Shield,
                    label = "Vault",
                    color = IndiaChakraBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToVault
                )
            }
        }

        // Upcoming Message Queue Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Queue (${pendingMessages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingMessages.isNotEmpty()) {
                    TextButton(onClick = onScheduleClick) {
                        Text("+ New", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (pendingMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ScheduleSend,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "No pending scheduled messages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Schedule one-time or recurring messages with variable personalization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onScheduleClick,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Schedule First Message")
                        }
                    }
                }
            }
        } else {
            items(pendingMessages, key = { it.id }) { msg ->
                ScheduledMessageCard(
                    message = msg,
                    onSendNow = { onDispatchMessage(context, msg) },
                    onCancel = { onCancelMessage(msg.id) },
                    onDelete = { onDeleteMessage(msg.id) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ScheduledMessageCard(
    message: ScheduledMessageEntity,
    onSendNow: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(message.scheduledTimestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = message.recipientName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.recipient,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreenPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (message.recurrence != "NONE") "${message.recurrence} • $formattedTime" else formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppGreenPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = message.messageText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSendNow,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
