package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationWorkflowEntity
import com.example.ui.MainUiState
import com.example.ui.components.ProBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(
    uiState: MainUiState,
    onToggleWorkflow: (AutomationWorkflowEntity) -> Unit,
    onDeleteWorkflow: (AutomationWorkflowEntity) -> Unit,
    onSaveWorkflow: (AutomationWorkflowEntity) -> Unit,
    onOpenPaywall: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Visual Builder & Workflows, 1: Follow-up Sequences
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = IndiaSaffron,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Workflow", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Automation Engine",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndiaGreen.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, IndiaGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "BHARAT RULES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndiaGreenLight,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Visual Workflows: Trigger → Indian Business Hours → Auto Action",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProBadge(isPro = uiState.isPremium, onClick = onOpenPaywall)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                contentColor = IndiaSaffron
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Rules (${uiState.workflows.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Follow-up Sequences") }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = WhatsAppGreenPrimary)
                                    Text("Visual Automation Architecture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Automations execute using Android AlarmManager & WorkManager. Respects Android background battery optimizations and user anti-spam limits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(uiState.workflows, key = { it.id }) { workflow ->
                        WorkflowCard(
                            workflow = workflow,
                            onToggle = { onToggleWorkflow(workflow) },
                            onDelete = { onDeleteWorkflow(workflow) }
                        )
                    }
                }
            } else {
                FollowUpSequencesList(
                    isPro = uiState.isPremium,
                    onOpenPaywall = onOpenPaywall,
                    onCreateSequence = { showCreateDialog = true }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateWorkflowDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { entity ->
                onSaveWorkflow(entity)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun WorkflowCard(
    workflow: AutomationWorkflowEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = workflow.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = workflow.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = workflow.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = WhatsAppGreenPrimary)
                )
            }

            // Visual Pipeline Diagram
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PipelineStep(
                        label = "TRIGGER",
                        value = "${workflow.triggerType} (${workflow.triggerValue})",
                        icon = Icons.Default.Bolt,
                        color = ProGold
                    )
                    PipelineConnector()
                    PipelineStep(
                        label = "CONDITION",
                        value = workflow.conditionText,
                        icon = Icons.Default.FilterAlt,
                        color = InfoBlue
                    )
                    PipelineConnector()
                    PipelineStep(
                        label = "ACTION",
                        value = "${workflow.actionType} → \"${workflow.actionPayload.take(45)}...\"",
                        icon = Icons.AutoMirrored.Filled.Send,
                        color = WhatsAppGreenPrimary
                    )
                    if (workflow.delayMinutes > 0) {
                        PipelineConnector()
                        PipelineStep(
                            label = "DELAY / STEP 2",
                            value = "Wait ${workflow.delayMinutes / 60}h → Send Follow-up ping",
                            icon = Icons.Default.HourglassTop,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fired ${workflow.runCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PipelineStep(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PipelineConnector() {
    Box(
        modifier = Modifier
            .padding(start = 11.dp)
            .height(10.dp)
            .width(2.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    )
}

@Composable
fun FollowUpSequencesList(
    isPro: Boolean,
    onOpenPaywall: () -> Unit,
    onCreateSequence: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Automated Follow-up Chains", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Configure structured 3-step outreach: Initial Message → Wait 2 Days → Follow-up Reminder → Wait 3 Days → Final Ping.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val sequences = listOf(
            Triple("New Lead Conversion", "3-Step Chain (0h → 24h → 72h)", "94% Response Rate"),
            Triple("Unpaid Invoice Reminder", "2-Step Chain (On due date → +3 days)", "88% Recovery"),
            Triple("Demo Request Booking", "3-Step Chain (Instant → +2 days → +5 days)", "Demo follow-up")
        )

        items(sequences) { (title, cadence, stats) ->
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
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WhatsAppGreenPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stats,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(text = cadence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { if (isPro) onCreateSequence() else onOpenPaywall() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(if (isPro) "Use Sequence" else "Unlock Sequence (Pro)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWorkflowDialog(
    onDismiss: () -> Unit,
    onSave: (AutomationWorkflowEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("TIME_SCHEDULE") }
    var triggerValue by remember { mutableStateOf("09:00 AM") }
    var conditionText by remember { mutableStateOf("Only Weekdays & Safe Quota valid") }
    var actionPayload by remember { mutableStateOf("") }

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
                Text("Create Automation Rule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workflow Name") },
                    placeholder = { Text("e.g. Daily Standup Ping") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Trigger Type:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TIME_SCHEDULE" to "Time", "CONTACT_TAG" to "Tag", "RECURRING_INTERVAL" to "Recurring").forEach { (type, label) ->
                        FilterChip(
                            selected = triggerType == type,
                            onClick = { triggerType = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = triggerValue,
                    onValueChange = { triggerValue = it },
                    label = { Text("Trigger Value (Time / Tag / Interval)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = actionPayload,
                    onValueChange = { actionPayload = it },
                    label = { Text("Message Action Content") },
                    placeholder = { Text("Good morning {name}, here is your schedule...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && actionPayload.isNotBlank()) {
                                onSave(
                                    AutomationWorkflowEntity(
                                        name = name,
                                        description = "Automated execution on $triggerValue",
                                        triggerType = triggerType,
                                        triggerValue = triggerValue,
                                        conditionText = conditionText,
                                        actionType = "SEND_MESSAGE",
                                        actionPayload = actionPayload,
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        Text("Create Workflow")
                    }
                }
            }
        }
    }
}
