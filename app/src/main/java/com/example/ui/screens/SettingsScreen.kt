package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.components.ProBadge
import com.example.ui.dialogs.PinDialog
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onOpenPaywall: () -> Unit,
    onTogglePro: (Boolean) -> Unit,
    onSetupPin: (String?) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onUpdateSafetySettings: (dailyLimit: Int, delaySec: Int, safeguardsActive: Boolean) -> Unit,
    onSetHighThinking: (Boolean) -> Unit,
    onSetAutoDeleteDays: (Int) -> Unit,
    onClearVault: () -> Unit,
    onImportContacts: () -> Unit
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showComplianceDialog by remember { mutableStateOf(false) }

    var dailyLimit by remember(uiState.dailySafetyLimit) { mutableStateOf(uiState.dailySafetyLimit) }
    var delaySec by remember(uiState.delayBetweenMessagesSec) { mutableStateOf(uiState.delayBetweenMessagesSec) }
    var safeguardsActive by remember(uiState.safeguardsActive) { mutableStateOf(uiState.safeguardsActive) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settings & Privacy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Security configuration, safeguards, and account options",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Subscription Tier Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isPremium) ProGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isPremium) ProGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (uiState.isPremium) "Bharat Pro Plan (₹)" else "Free Tier (Demo)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ProBadge(isPro = uiState.isPremium)
                        }
                        Text(
                            text = if (uiState.isPremium)
                                "Unlimited WhatsApp automations, Gemini 3.1 Pro AI, ₹499/yr"
                            else
                                "3 active scheduled messages • Basic templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onOpenPaywall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isPremium) IndiaSaffron else IndiaGreen
                        )
                    ) {
                        Text(
                            text = if (uiState.isPremium) "Manage (₹)" else "Upgrade (₹)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Automation Safeguards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = WhatsAppGreenPrimary)
                            Text("Anti-Spam Safeguards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = safeguardsActive,
                            onCheckedChange = {
                                safeguardsActive = it
                                onUpdateSafetySettings(dailyLimit, delaySec, it)
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Daily Message Cap: $dailyLimit messages / day", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = dailyLimit.toFloat(),
                            onValueChange = {
                                dailyLimit = it.toInt()
                                onUpdateSafetySettings(it.toInt(), delaySec, safeguardsActive)
                            },
                            valueRange = 10f..150f,
                            steps = 13,
                            colors = SliderDefaults.colors(thumbColor = WhatsAppGreenPrimary, activeTrackColor = WhatsAppGreenPrimary)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Inter-message Safe Delay: ${delaySec}s", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = delaySec.toFloat(),
                            onValueChange = {
                                delaySec = it.toInt()
                                onUpdateSafetySettings(dailyLimit, it.toInt(), safeguardsActive)
                            },
                            valueRange = 5f..30f,
                            steps = 4,
                            colors = SliderDefaults.colors(thumbColor = WhatsAppGreenPrimary, activeTrackColor = WhatsAppGreenPrimary)
                        )
                    }
                }
            }
        }

        // Vault Privacy & Security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreenPrimary)
                        Text("Notification Vault Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vault PIN Lock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (uiState.vaultPinConfigured) "PIN protection enabled" else "No PIN set (Vault unlocked)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                if (uiState.vaultPinConfigured) "Change" else "Setup",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (uiState.vaultPinConfigured) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Biometric Unlock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Use fingerprint or device unlock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.isBiometricEnabled,
                                onCheckedChange = onToggleBiometric
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Auto-Delete Retention:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days", -1 to "Keep All").forEach { (days, label) ->
                            FilterChip(
                                selected = uiState.autoDeleteDays == days,
                                onClick = { onSetAutoDeleteDays(days) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("System Notification Access →")
                        }
                        TextButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Vault History")
                        }
                    }
                }
            }
        }

        // AI Engine Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = ProGold)
                        Text("Gemini AI Engine Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gemini 3.1 Pro Thinking Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("High reasoning level for complex message crafting and follow-up strategy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.highThinkingEnabled,
                            onCheckedChange = onSetHighThinking,
                            colors = SwitchDefaults.colors(checkedThumbColor = ProGold)
                        )
                    }
                }
            }
        }

        // Data & Contacts Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = WhatsAppGreenPrimary)
                        Text("Contacts & Campaign Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Device Contacts Import", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${uiState.contacts.size} contacts currently loaded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = onImportContacts) {
                            Text("Import Now")
                        }
                    }
                }
            }
        }

        // Transparency & Compliance
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showComplianceDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = WhatsAppGreenPrimary)
                        Text("WhatsApp Compliance & Technical Transparency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            isSetupMode = true,
            onDismiss = { showPinDialog = false },
            onSubmitPin = { pin ->
                onSetupPin(pin)
                showPinDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Notification Vault?") },
            text = { Text("This will permanently delete all encrypted WhatsApp notification records stored on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearVault()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showComplianceDialog) {
        AlertDialog(
            onDismissRequest = { showComplianceDialog = false },
            title = { Text("Technical Transparency & Google Play Compliance") },
            text = {
                Text(
                    "WA Automation Pro operates strictly within standard Android platform boundaries:\n\n" +
                            "1. Notification Vault uses the official Android NotificationListenerService to read user-visible notifications. It never modifies WhatsApp databases or bypasses end-to-end encryption.\n\n" +
                            "2. Automated dispatch utilizes Android Intents and exact alarm scheduling. We incorporate configurable delay safeguards and daily caps to comply with anti-spam standards.\n\n" +
                            "3. Privacy-first: All data is saved on device with Android KeyStore encryption.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showComplianceDialog = false }) { Text("Close") }
            }
        )
    }
}
