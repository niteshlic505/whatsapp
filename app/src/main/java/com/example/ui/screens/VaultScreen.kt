package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationVaultEntity
import com.example.data.service.NotificationVaultService
import com.example.ui.MainUiState
import com.example.ui.components.ProBadge
import com.example.ui.dialogs.PinDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    uiState: MainUiState,
    onUnlockWithPin: (String) -> Boolean,
    onUnlockWithBiometrics: () -> Unit,
    onSetupPin: () -> Unit,
    onLockVault: () -> Unit,
    onExcludeChat: (String) -> Unit,
    onClearVault: () -> Unit,
    onDeleteNotification: (Long) -> Unit,
    onSimulateCapture: (sender: String, message: String, isDeleted: Boolean) -> Unit,
    onGenerateSmartReplies: (messageText: String, sender: String) -> Unit,
    onDismissSmartReplies: () -> Unit,
    onSummarizeChat: (sender: String) -> Unit,
    onDismissChatSummary: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("ALL") } // ALL, DELETED_DETECTED
    var searchQuery by remember { mutableStateOf("") }
    var showSimulateDialog by remember { mutableStateOf(false) }

    // Check notification listener permission status
    var isPermissionGranted by remember {
        mutableStateOf(NotificationVaultService.isPermissionGranted(context))
    }

    // Refresh permission status when screen is viewed
    LaunchedEffect(Unit) {
        isPermissionGranted = NotificationVaultService.isPermissionGranted(context)
    }

    // If locked, render lock wall
    if (uiState.vaultPinConfigured && !uiState.isVaultUnlocked) {
        VaultLockedScreen(
            isBiometricEnabled = uiState.isBiometricEnabled,
            onEnterPin = { showPinDialog = true },
            onBiometricAuth = onUnlockWithBiometrics
        )

        if (showPinDialog) {
            PinDialog(
                isSetupMode = false,
                onDismiss = { showPinDialog = false },
                onSubmitPin = { pin ->
                    if (onUnlockWithPin(pin)) {
                        showPinDialog = false
                    }
                }
            )
        }
        return
    }

    val filteredList = uiState.vaultNotifications.filter {
        val matchesType = when (filterType) {
            "DELETED_DETECTED" -> it.isDeletedDetected
            "INCOMING" -> it.direction != "OUTGOING"
            "OUTGOING" -> it.direction == "OUTGOING"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                it.senderTitle.contains(searchQuery, ignoreCase = true) ||
                it.messageContent.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    val totalCount = uiState.vaultNotifications.size
    val deletedCount = uiState.vaultNotifications.count { it.isDeletedDetected }
    val incomingCount = uiState.vaultNotifications.count { it.direction != "OUTGOING" }
    val outgoingCount = uiState.vaultNotifications.count { it.direction == "OUTGOING" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "History & Recovery Vault",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndiaChakraBlue.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, IndiaChakraBlue.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🇮🇳 SECURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndiaChakraBlueLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Encrypted local archive with WhatsApp deletion recovery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.vaultPinConfigured) {
                        IconButton(onClick = onLockVault) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = ProGold)
                        }
                    }
                    IconButton(onClick = { showSimulateDialog = true }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Test Simulator", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Live Permission Status Banner
        item {
            if (!isPermissionGranted) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ProGold.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ProGold)
                            Text(
                                text = "Notification Listener Access Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "To automatically capture incoming WhatsApp notifications and preserve deleted messages, enable WA Automation Pro in Android Notification Listener settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                NotificationVaultService.openPermissionSettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProGold),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Enable in Android Settings", color = Color(0xFF382307), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WhatsAppGreenPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WhatsAppGreenPrimary))
                            Text(
                                text = "Notification Listener: ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenPrimary
                            )
                        }
                        TextButton(
                            onClick = { NotificationVaultService.openPermissionSettings(context) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Settings", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Mandatory Transparency & Privacy Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                        Text(
                            text = "Privacy & Technical Transparency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "This Vault archives WhatsApp notifications received while notification access is enabled on this device. Android notification listeners cannot recover messages that never triggered a notification, and this software does NOT alter, decrypt, or access WhatsApp's internal encrypted database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-delete: ${if (uiState.autoDeleteDays > 0) "${uiState.autoDeleteDays} days" else "Indefinite"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhatsAppGreenPrimary
                        )
                        Text(
                            text = "Total Protected: ${uiState.vaultNotifications.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search and Filters
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search archived notifications...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        label = { Text("All ($totalCount)") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "DELETED_DETECTED",
                        onClick = { filterType = "DELETED_DETECTED" },
                        label = { Text("⚠️ Deleted ($deletedCount)", color = if (deletedCount > 0) ProGold else MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProGold.copy(alpha = 0.25f)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "INCOMING",
                        onClick = { filterType = "INCOMING" },
                        label = { Text("📥 Incoming ($incomingCount)") },
                        leadingIcon = { Icon(Icons.Default.CallReceived, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InfoBlue.copy(alpha = 0.2f)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "OUTGOING",
                        onClick = { filterType = "OUTGOING" },
                        label = { Text("📤 Outgoing ($outgoingCount)") },
                        leadingIcon = { Icon(Icons.Default.CallMade, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppGreenPrimary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Text("No archived notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "When WhatsApp messages arrive, their notification text is securely encrypted and archived locally in Room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showSimulateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Sample WhatsApp Message")
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                VaultNotificationCard(
                    notification = item,
                    onExcludeChat = { onExcludeChat(item.senderTitle) },
                    onDelete = { onDeleteNotification(item.id) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Vault Message", item.messageContent))
                    },
                    onAiSmartReply = {
                        onGenerateSmartReplies(item.messageContent, item.senderTitle)
                    },
                    onAiSummarize = {
                        onSummarizeChat(item.senderTitle)
                    }
                )
            }
        }
    }

    if (uiState.isGeneratingReplies || uiState.activeSmartReplies.isNotEmpty()) {
        SmartRepliesDialog(
            targetMessage = uiState.selectedReplyTargetMessage ?: "",
            targetSender = uiState.selectedReplyTargetSender ?: "Contact",
            isLoading = uiState.isGeneratingReplies,
            replies = uiState.activeSmartReplies,
            onDismiss = onDismissSmartReplies,
            onSendWhatsApp = { text ->
                val intent = com.example.engine.WhatsAppDispatcher.createWhatsAppIntent("", text)
                context.startActivity(intent)
            }
        )
    }

    if (uiState.isGeneratingSummary || uiState.activeChatSummary != null) {
        ChatSummaryDialog(
            isLoading = uiState.isGeneratingSummary,
            summary = uiState.activeChatSummary,
            onDismiss = onDismissChatSummary,
            onSendSuggestedReply = { text ->
                val intent = com.example.engine.WhatsAppDispatcher.createWhatsAppIntent("", text)
                context.startActivity(intent)
            }
        )
    }

    if (showSimulateDialog) {
        SimulateNotificationDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { sender, message, isDeleted ->
                onSimulateCapture(sender, message, isDeleted)
                showSimulateDialog = false
            }
        )
    }
}

@Composable
private fun VaultLockedScreen(
    isBiometricEnabled: Boolean,
    onEnterPin: () -> Unit,
    onBiometricAuth: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(28.dp))
                }
                Text("Notification Vault Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Your archived WhatsApp messages and deleted notification history are protected by encrypted local storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onEnterPin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with PIN", fontWeight = FontWeight.Bold)
                }

                if (isBiometricEnabled) {
                    OutlinedButton(
                        onClick = onBiometricAuth,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics")
                    }
                }
            }
        }
    }
}

@Composable
fun VaultNotificationCard(
    notification: NotificationVaultEntity,
    onExcludeChat: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onAiSmartReply: () -> Unit,
    onAiSummarize: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(notification.timestamp))

    val isOutgoing = notification.direction == "OUTGOING"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                notification.isDeletedDetected -> Color(0xFF2B1D12)
                isOutgoing -> Color(0xFF0F261D)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                notification.isDeletedDetected -> ProGold
                isOutgoing -> WhatsAppGreenPrimary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    notification.isDeletedDetected -> ProGold.copy(alpha = 0.25f)
                                    isOutgoing -> WhatsAppGreenPrimary.copy(alpha = 0.25f)
                                    else -> InfoBlue.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                notification.isDeletedDetected -> Icons.Default.DeleteSweep
                                isOutgoing -> Icons.Default.DoneAll
                                else -> Icons.Default.ChatBubble
                            },
                            contentDescription = null,
                            tint = when {
                                notification.isDeletedDetected -> ProGold
                                isOutgoing -> WhatsAppGreenPrimary
                                else -> InfoBlue
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column {
                        Text(
                            text = notification.senderTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOutgoing) {
                            Text(
                                text = "Sent from this device • $dateStr",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (notification.isDeletedDetected) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ProGold.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ProGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ProGold, modifier = Modifier.size(11.dp))
                            Text(
                                text = "DELETED BY SENDER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProGold
                            )
                        }
                    }
                } else if (isOutgoing) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WhatsAppGreenPrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(11.dp))
                            Text(
                                text = "OUTGOING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (notification.isDeletedDetected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E140C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LockClock, contentDescription = null, tint = ProGold, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Recovered Original Message (Deleted from WhatsApp chat)",
                            fontSize = 11.sp,
                            color = ProGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (isOutgoing) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF071D14),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(12.dp))
                        Text(
                            text = "Dispatched via WhatsApp Automation Engine",
                            fontSize = 10.sp,
                            color = WhatsAppGreenPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = notification.messageContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // AI Action Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = onAiSmartReply,
                    label = { Text("AI Smart Reply", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(14.dp))
                    }
                )
                SuggestionChip(
                    onClick = onAiSummarize,
                    label = { Text("AI Chat Digest", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(Icons.Default.Summarize, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(14.dp))
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encrypted in local vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Row {
                    TextButton(onClick = onExcludeChat, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Exclude Chat", fontSize = 11.sp)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SmartRepliesDialog(
    targetMessage: String,
    targetSender: String,
    isLoading: Boolean,
    replies: List<com.example.data.ai.SmartReply>,
    onDismiss: () -> Unit,
    onSendWhatsApp: (String) -> Unit
) {
    val context = LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppGreenPrimary)
                    Column {
                        Text("Gemini AI Smart Replies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Tailored responses for $targetSender", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Incoming Message:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(targetMessage.take(120), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = WhatsAppGreenPrimary)
                            Text("Generating contextual replies with Gemini...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        replies.forEach { rep ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rep.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WhatsAppGreenPrimary)
                                        Text(rep.tone, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(rep.text, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("AI Reply", rep.text))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        }
                                        Button(
                                            onClick = { onSendWhatsApp(rep.text) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Send WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun ChatSummaryDialog(
    isLoading: Boolean,
    summary: com.example.data.ai.ChatSummaryResult?,
    onDismiss: () -> Unit,
    onSendSuggestedReply: (String) -> Unit
) {
    val context = LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Summarize, contentDescription = null, tint = InfoBlue)
                    Column {
                        Text("AI Chat Digest & Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(summary?.sender ?: "Chat Summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isLoading || summary == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = InfoBlue)
                            Text("Analyzing chat history with Gemini...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = InfoBlue.copy(alpha = 0.15f)
                        ) {
                            Text("Sentiment: ${summary.sentiment}", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (summary.urgencyLevel.contains("High") || summary.urgencyLevel.contains("Urgent")) ProGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text("Urgency: ${summary.urgencyLevel}", fontSize = 11.sp, color = if (summary.urgencyLevel.contains("High") || summary.urgencyLevel.contains("Urgent")) ProGold else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Text(summary.summary, style = MaterialTheme.typography.bodyMedium)

                    if (summary.actionItems.isNotEmpty()) {
                        Text("Key Action Items:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.actionItems.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(14.dp))
                                    Text(item, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Recommended Next Reply:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreenPrimary)
                            Text(summary.suggestedReply, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { onSendSuggestedReply(summary.suggestedReply) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Send Reply", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun SimulateNotificationDialog(
    onDismiss: () -> Unit,
    onSimulate: (sender: String, message: String, isDeleted: Boolean) -> Unit
) {
    var sender by remember { mutableStateOf("Sarah Jenkins") }
    var message by remember { mutableStateOf("Hey, please check the revised project contract when you have a moment!") }
    var isDeleted by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Test WhatsApp Notification Capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text(
                    text = "Simulate an incoming WhatsApp notification being captured and encrypted into the Room database, or simulate the sender revoking a message.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick preset buttons for instant testing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sender = "David Chen"
                            message = "Can we move our demo to 4 PM?"
                            isDeleted = false
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("1. Sample Msg", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            sender = "David Chen"
                            message = "This message was deleted"
                            isDeleted = true
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("2. Delete It", fontSize = 11.sp, color = ProGold)
                    }
                }

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender / Group Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Text") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isDeleted,
                        onCheckedChange = { isDeleted = it },
                        colors = CheckboxDefaults.colors(checkedColor = ProGold)
                    )
                    Text("Flag as 'Deleted by Sender' notification", style = MaterialTheme.typography.bodyMedium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (sender.isNotBlank() && message.isNotBlank()) {
                                onSimulate(sender, message, isDeleted)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        Text("Execute Capture")
                    }
                }
            }
        }
    }
}
