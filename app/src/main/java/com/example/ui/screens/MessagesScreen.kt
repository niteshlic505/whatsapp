package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.RewriteTone
import com.example.data.model.CampaignEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageTemplateEntity
import com.example.engine.WhatsAppDispatcher
import com.example.ui.MainUiState
import com.example.ui.components.ProBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    uiState: MainUiState,
    onSaveTemplate: (title: String, content: String, category: String, isFavorite: Boolean) -> Unit,
    onToggleFavorite: (MessageTemplateEntity) -> Unit,
    onDeleteTemplate: (MessageTemplateEntity) -> Unit,
    onRewriteWithAi: (String, RewriteTone, String, String) -> Unit,
    onGenerateWithAi: (String) -> Unit,
    onCheckSpam: (String) -> Unit,
    onClearAiResult: () -> Unit,
    onCreateCampaign: (name: String, template: String, tag: String, delaySec: Int) -> Unit,
    onImportContactsFromFile: (Uri, String?, String) -> Unit,
    onImportContactsFromPreset: (String) -> Unit,
    onSaveManualContact: (String, String, String, String) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onOpenPaywall: () -> Unit,
    onScheduleClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Templates, 1: AI Assistant, 2: Campaigns, 3: Contacts
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    // Launcher for PDF and Excel / CSV files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileNameFromUri(context, uri) ?: "imported_file"
            val mime = context.contentResolver.getType(uri)
            onImportContactsFromFile(uri, mime, name)
        }
    }

    // Launcher for Images (Business Cards, screenshots)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileNameFromUri(context, uri) ?: "business_card.jpg"
            onImportContactsFromFile(uri, "image/jpeg", name)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showNewTemplateDialog = true },
                    containerColor = WhatsAppGreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Template")
                }
            } else if (selectedTab == 3) {
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    containerColor = WhatsAppGreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                }
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
                    Text(
                        text = "Message Manager",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Templates, AI Rewriting & Bulk Campaigns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProBadge(isPro = uiState.isPremium, onClick = onOpenPaywall)
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Templates (${uiState.templates.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("AI Studio") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Campaigns") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Contacts (${uiState.contacts.size})") }
                )
            }

            when (selectedTab) {
                0 -> TemplatesTab(
                    templates = uiState.templates,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onToggleFavorite = onToggleFavorite,
                    onDeleteTemplate = onDeleteTemplate,
                    onCopyTemplate = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Template", text))
                    }
                )
                1 -> AiAssistantTab(
                    uiState = uiState,
                    onRewrite = onRewriteWithAi,
                    onGenerate = onGenerateWithAi,
                    onCheckSpam = onCheckSpam,
                    onClearResult = onClearAiResult,
                    onSaveAsTemplate = { text ->
                        onSaveTemplate("AI Generated Message", text, "Business", false)
                    },
                    onOpenPaywall = onOpenPaywall
                )
                2 -> CampaignsTab(
                    uiState = uiState,
                    onCreateCampaign = onCreateCampaign,
                    onOpenPaywall = onOpenPaywall
                )
                3 -> ContactsAiImportTab(
                    contacts = uiState.contacts,
                    onImportPdf = {
                        documentPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onImportImage = {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onImportExcel = {
                        documentPickerLauncher.launch(arrayOf(
                            "text/comma-separated-values",
                            "text/csv",
                            "text/plain",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "*/*"
                        ))
                    },
                    onImportPreset = onImportContactsFromPreset,
                    onAddManualClick = { showAddContactDialog = true },
                    onDeleteContact = onDeleteContact,
                    onDirectChat = { phone ->
                        val intent = WhatsAppDispatcher.createWhatsAppIntent(phone, "")
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onSave = { name, phone, tag, notes ->
                onSaveManualContact(name, phone, tag, notes)
                showAddContactDialog = false
            }
        )
    }

    if (showNewTemplateDialog) {
        NewTemplateDialog(
            onDismiss = { showNewTemplateDialog = false },
            onSave = { title, content, cat ->
                onSaveTemplate(title, content, cat, false)
                showNewTemplateDialog = false
            }
        )
    }
}

@Composable
private fun TemplatesTab(
    templates: List<MessageTemplateEntity>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorite: (MessageTemplateEntity) -> Unit,
    onDeleteTemplate: (MessageTemplateEntity) -> Unit,
    onCopyTemplate: (String) -> Unit
) {
    val categories = listOf("All", "Business", "Follow-up", "Greetings", "Support", "Sales", "Personal")
    val filtered = templates.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
                (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search templates & variables...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No templates found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Create quick templates with {name}, {date}, and {time} variables.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onToggleFavorite = { onToggleFavorite(template) },
                    onDelete = { onDeleteTemplate(template) },
                    onCopy = { onCopyTemplate(template.content) }
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: MessageTemplateEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
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
                    Text(text = template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WhatsAppGreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = template.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppGreenPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (template.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (template.isFavorite) ProGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = template.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used ${template.usageCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onCopy,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAssistantTab(
    uiState: MainUiState,
    onRewrite: (String, RewriteTone, String, String) -> Unit,
    onGenerate: (String) -> Unit,
    onCheckSpam: (String) -> Unit,
    onClearResult: () -> Unit,
    onSaveAsTemplate: (String) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(RewriteTone.PROFESSIONAL) }
    var briefInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // AI Badge Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProGold.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ProGold.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ProGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = ProGold)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Gemini 3.1 Pro AI Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(4.dp), color = ProGold) {
                                Text("HIGH THINKING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("High reasoning mode enabled for nuanced, tone-perfect messaging.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Message Rewriter Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AI Message Rewriter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            onCheckSpam(it)
                        },
                        label = { Text("Draft Message") },
                        placeholder = { Text("Type draft or bullet points to rewrite...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
                    )

                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Target Recipient (Optional)") },
                        placeholder = { Text("e.g. Sarah Jenkins") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Desired Tone:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(RewriteTone.values()) { tone ->
                            FilterChip(
                                selected = selectedTone == tone,
                                onClick = { selectedTone = tone },
                                label = { Text(tone.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = { onRewrite(inputText, selectedTone, recipientName, "") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputText.isNotBlank() && !uiState.isGeneratingAi,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        if (uiState.isGeneratingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deep Reasoning...")
                        } else {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rewrite with Gemini Pro", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Spam Risk Analysis Output
        if (uiState.spamRiskAnalysis != null) {
            item {
                val risk = uiState.spamRiskAnalysis
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (risk.riskScore > 50) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Spam Risk & Policy Checker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Score: ${risk.riskScore}% (${risk.riskLevel})", fontWeight = FontWeight.Bold, color = if (risk.riskScore > 50) ErrorRed else WhatsAppGreenPrimary)
                        }
                        if (risk.flags.isNotEmpty()) {
                            risk.flags.forEach { flag ->
                                Text("• $flag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text("Clean message structure. Safe for bulk & single dispatch.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                        }
                    }
                }
            }
        }

        // AI Generated Output
        if (uiState.aiResultText != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rewritten Output", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WhatsAppGreenPrimary)
                            IconButton(onClick = onClearResult) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                        Text(
                            text = uiState.aiResultText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSaveAsTemplate(uiState.aiResultText) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Save as Template", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI Message", uiState.aiResultText))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Generate from Brief
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Generate from Idea / Brief", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = briefInput,
                        onValueChange = { briefInput = it },
                        label = { Text("What message do you need?") },
                        placeholder = { Text("e.g. Follow-up after proposal submission offering a 10% coupon") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onGenerate(briefInput) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = briefInput.isNotBlank() && !uiState.isGeneratingAi,
                        colors = ButtonDefaults.buttonColors(containerColor = ProGold)
                    ) {
                        Text("Generate Message with Gemini", color = Color(0xFF382307), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignsTab(
    uiState: MainUiState,
    onCreateCampaign: (name: String, template: String, tag: String, delaySec: Int) -> Unit,
    onOpenPaywall: () -> Unit
) {
    var campaignName by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Customers") }
    var messageTemplate by remember { mutableStateOf("Hi {name}, thank you for choosing our services! Special offer inside.") }
    var delaySec by remember { mutableStateOf(8) }

    val tags = listOf("Customers", "VIP", "Leads", "Follow-up", "Friends")
    val eligibleContacts = uiState.contacts.filter { it.tag.equals(selectedTag, ignoreCase = true) }

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = WhatsAppGreenPrimary)
                        Text("Safe Bulk Messaging Campaigns", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Configurable safe delay (5s - 25s) between message dispatches to safeguard your WhatsApp account from rate limiting and spam detection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create New Campaign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = campaignName,
                        onValueChange = { campaignName = it },
                        label = { Text("Campaign Name") },
                        placeholder = { Text("e.g. Q4 Client Appreciation") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Target Contact Tag:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                label = { Text(tag, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Audience: ${eligibleContacts.size} contacts found with tag '$selectedTag'", style = MaterialTheme.typography.bodySmall, color = WhatsAppGreenPrimary)

                    OutlinedTextField(
                        value = messageTemplate,
                        onValueChange = { messageTemplate = it },
                        label = { Text("Personalized Message Template") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
                    )

                    // Delay slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Delay between recipients: ${delaySec}s (Safeguard)", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = delaySec.toFloat(),
                            onValueChange = { delaySec = it.toInt() },
                            valueRange = 5f..25f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = WhatsAppGreenPrimary, activeTrackColor = WhatsAppGreenPrimary)
                        )
                    }

                    Button(
                        onClick = {
                            if (campaignName.isNotBlank() && messageTemplate.isNotBlank()) {
                                onCreateCampaign(campaignName, messageTemplate, selectedTag, delaySec)
                                campaignName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch Safe Campaign", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Campaign History (${uiState.campaigns.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(uiState.campaigns, key = { it.id }) { campaign ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(campaign.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WhatsAppGreenPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(campaign.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreenPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Tag: ${campaign.targetTag} • Delay: ${campaign.delaySeconds}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { if (campaign.totalRecipients > 0) campaign.sentRecipients.toFloat() / campaign.totalRecipients else 1f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = WhatsAppGreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun NewTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Business") }

    val categories = listOf("Business", "Follow-up", "Greetings", "Support", "Sales", "Personal")

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
                Text("Create Message Template", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Template Content (supports {name}, {date}, {time})") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(title, content, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        Text("Save Template")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsAiImportTab(
    contacts: List<ContactEntity>,
    onImportPdf: () -> Unit,
    onImportImage: () -> Unit,
    onImportExcel: () -> Unit,
    onImportPreset: (String) -> Unit,
    onAddManualClick: () -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onDirectChat: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All") }
    val tags = listOf("All", "VIP", "Customers", "Leads", "Follow-up")

    val filteredContacts = contacts.filter { c ->
        val matchesTag = if (selectedTag == "All") true else c.tag.equals(selectedTag, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                c.notes.contains(searchQuery, ignoreCase = true)
        matchesTag && matchesSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // AI Contact Extractor Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WhatsAppDarkSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreenPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "AI Smart Contact Importer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Extract contacts from PDF, Image, & Excel with Gemini AI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ProGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "GEMINI AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Upload any PDF roster, photo of a business card, or Excel spreadsheet. Gemini will automatically extract names, phone numbers, and categories into WhatsApp Automation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 3 Upload Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onImportPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Doc", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onImportImage,
                            colors = ButtonDefaults.buttonColors(containerColor = ProGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Image OCR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onImportExcel,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel / CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test Presets for Instant One-Click Demo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Or Test Instantly with Sample Files:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onImportPreset("EXCEL") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sample Excel", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onImportPreset("PDF") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sample PDF", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onImportPreset("IMAGE") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Business Card", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Search & Manual Add Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ${contacts.size} contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onAddManualClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WhatsAppGreenPrimary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = Color.White)
                }
            }
        }

        // Tag Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    val count = if (tag == "All") contacts.size else contacts.count { it.tag.equals(tag, true) }
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text("$tag ($count)", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredContacts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Text("No contacts found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Import contacts from PDF, Excel, or Image using Gemini AI above, or add a contact manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredContacts, key = { it.id }) { contact ->
                ContactCardItem(
                    contact = contact,
                    onDirectChat = { onDirectChat(contact.phoneNumber) },
                    onDelete = { onDeleteContact(contact) }
                )
            }
        }
    }
}

@Composable
fun ContactCardItem(
    contact: ContactEntity,
    onDirectChat: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when (contact.tag) {
                                    "VIP" -> ProGold.copy(alpha = 0.2f)
                                    "Customers" -> InfoBlue.copy(alpha = 0.2f)
                                    else -> WhatsAppGreenPrimary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = when (contact.tag) {
                                "VIP" -> ProGold
                                "Customers" -> InfoBlue
                                else -> WhatsAppGreenPrimary
                            },
                            fontSize = 15.sp
                        )
                    }

                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreenPrimary, modifier = Modifier.size(12.dp))
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (contact.tag) {
                            "VIP" -> ProGold.copy(alpha = 0.2f)
                            "Customers" -> InfoBlue.copy(alpha = 0.2f)
                            else -> WhatsAppGreenPrimary.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = contact.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (contact.tag) {
                                "VIP" -> ProGold
                                "Customers" -> InfoBlue
                                else -> WhatsAppGreenPrimary
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (contact.notes.isNotBlank() || contact.source != "Manual") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contact.notes.isNotBlank()) {
                        Text(
                            text = contact.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (contact.source != "Manual") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = contact.source,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDirectChat,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat on WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, tag: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Customers") }
    var notes by remember { mutableStateOf("") }

    val tags = listOf("Customers", "VIP", "Leads", "Follow-up")

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
                Text("Add New Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name / Business Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (with Country Code)") },
                    placeholder = { Text("+1 555-0192") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tag / Category:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tags) { t ->
                        FilterChip(
                            selected = tag == t,
                            onClick = { tag = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Company / Role") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSave(name, phone, tag, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenPrimary)
                    ) {
                        Text("Save Contact")
                    }
                }
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && it.moveToFirst()) {
            name = it.getString(nameIndex)
        }
    }
    return name ?: uri.lastPathSegment
}
