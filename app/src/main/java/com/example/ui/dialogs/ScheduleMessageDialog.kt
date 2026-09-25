package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ContactEntity
import com.example.data.model.MessageTemplateEntity
import com.example.ui.theme.IndiaChakraBlue
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndiaGreenLight
import com.example.ui.theme.IndiaSaffron
import com.example.ui.theme.WhatsAppGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMessageDialog(
    templates: List<MessageTemplateEntity>,
    contacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onSchedule: (name: String, phone: String, text: String, timeMillis: Long, recurrence: String) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("NONE") }
    var delayHoursOption by remember { mutableStateOf(1) } // 1h, 2h, 4h, 8h, 24h
    var showTemplateSelector by remember { mutableStateOf(false) }
    var showContactSelector by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedule Message",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Recipient selector / input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Contact Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    if (contacts.isNotEmpty()) {
                        FilledTonalIconButton(onClick = { showContactSelector = !showContactSelector }) {
                            Icon(Icons.Default.Contacts, contentDescription = "Pick Contact")
                        }
                    }
                }

                if (showContactSelector && contacts.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Select Contact:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            contacts.take(5).forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            recipientName = contact.name
                                            recipientPhone = contact.phoneNumber
                                            showContactSelector = false
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = { recipientPhone = it },
                        label = { Text("WhatsApp Phone") },
                        placeholder = { Text("+91 98201 55012") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    Button(
                        onClick = {
                            val digitsOnly = recipientPhone.replace(Regex("[^0-9]"), "")
                            recipientPhone = when {
                                recipientPhone.startsWith("+91") -> recipientPhone
                                recipientPhone.startsWith("91") && digitsOnly.length == 12 -> "+91 ${digitsOnly.substring(2)}"
                                digitsOnly.length == 10 -> "+91 $digitsOnly"
                                recipientPhone.isBlank() -> "+91 "
                                else -> "+91 $recipientPhone"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndiaSaffron),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("🇮🇳 +91", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Indian Quick Greeting Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick Greetings:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val greetings = listOf(
                            "Namaste 🙏 " to "Namaste",
                            "Shubh Prabhat ☀️ " to "Morning",
                            "Diwali Wishes 🪔 " to "Festive",
                            "Jai Hind 🇮🇳 " to "Patriotic",
                            "GST Invoice: " to "Invoice"
                        )
                        items(greetings) { (greetingPrefix, label) ->
                            SuggestionChip(
                                onClick = {
                                    if (!messageText.startsWith(greetingPrefix)) {
                                        messageText = greetingPrefix + messageText
                                    }
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Message Text & Template Inserter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Message Text", style = MaterialTheme.typography.labelMedium)
                        TextButton(
                            onClick = { showTemplateSelector = !showTemplateSelector },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Insert Template", fontSize = 12.sp)
                        }
                    }

                    if (showTemplateSelector && templates.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Choose Template:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                templates.take(4).forEach { t ->
                                    Text(
                                        text = t.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                messageText = t.content
                                                showTemplateSelector = false
                                            }
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text("Hi {name}, following up on our discussion...") }
                    )

                    // Quick variable chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val vars = listOf("{name}", "{date}", "{time}", "{first_name}")
                        items(vars) { v ->
                            SuggestionChip(
                                onClick = { messageText += " $v" },
                                label = { Text(v, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Schedule Time Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Deliver in:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1 hr", 2 to "2 hrs", 4 to "4 hrs", 24 to "1 day").forEach { (hrs, label) ->
                            FilterChip(
                                selected = delayHoursOption == hrs,
                                onClick = { delayHoursOption = hrs },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Recurrence
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recurrence:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NONE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Weekly").forEach { (rec, label) ->
                            FilterChip(
                                selected = recurrence == rec,
                                onClick = { recurrence = rec },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val targetTime = System.currentTimeMillis() + (delayHoursOption * 3600_000L)
                        val finalName = if (recipientName.isNotBlank()) recipientName else "Client"
                        onSchedule(finalName, recipientPhone, messageText, targetTime, recurrence)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientPhone.isNotBlank() && messageText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ScheduleSend, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Schedule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
