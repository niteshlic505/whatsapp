package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.dialogs.ScheduleMessageDialog
import com.example.ui.dialogs.SubscriptionPaywallDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                var selectedTab by remember { mutableIntStateOf(0) }
                var showScheduleDialog by remember { mutableStateOf(false) }
                var showPaywallDialog by remember { mutableStateOf(false) }

                // Notification permission launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Notification permission handled
                }

                // Contacts permission launcher
                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.importDeviceContacts()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbar()
                    }
                }

                // BackHandler: If on secondary tab, return to Home screen
                BackHandler(enabled = selectedTab != 0) {
                    selectedTab = 0
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Column {
                                // Tiranga Accent Banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaSaffron))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaWhite))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaGreen))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(IndiaSaffron.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🇮🇳", fontSize = 18.sp)
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "WA Automation Pro",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = IndiaSaffron.copy(alpha = 0.18f),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, IndiaSaffron.copy(alpha = 0.6f))
                                                ) {
                                                    Text(
                                                        text = "BHARAT",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = IndiaSaffron,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Gemini AI • +91 WhatsApp Suite",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = IndiaGreen.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, IndiaGreen.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { showScheduleDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("🙏 Namaste", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndiaGreenLight)
                                            }
                                        }

                                        if (uiState.isPremium) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = IndiaMarigold.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, IndiaMarigold.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "PRO ₹",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IndiaMarigold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { showPaywallDialog = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, IndiaSaffron.copy(alpha = 0.6f))
                                            ) {
                                                Text("PRO ₹", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IndiaSaffron)
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndiaSaffron,
                                selectedTextColor = IndiaSaffron,
                                indicatorColor = IndiaSaffron.copy(alpha = 0.18f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text(stringResource(R.string.nav_home)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_home_button")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Bolt, contentDescription = "Automations") },
                                label = { Text(stringResource(R.string.nav_automations)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_automations_button")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Messages") },
                                label = { Text(stringResource(R.string.nav_messages)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_messages_button")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = "Vault") },
                                label = { Text(stringResource(R.string.nav_vault)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_vault_button")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text(stringResource(R.string.nav_settings)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_settings_button")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                uiState = uiState,
                                onScheduleClick = { showScheduleDialog = true },
                                onOpenPaywall = { showPaywallDialog = true },
                                onNavigateToAutomations = { selectedTab = 1 },
                                onNavigateToMessages = { selectedTab = 2 },
                                onNavigateToVault = { selectedTab = 3 },
                                onNavigateToSettings = { selectedTab = 4 },
                                onDispatchMessage = { ctx, msg -> viewModel.dispatchDirectly(ctx, msg) },
                                onCancelMessage = { id -> viewModel.cancelScheduledMessage(id) },
                                onDeleteMessage = { id -> viewModel.deleteScheduledMessage(id) }
                            )
                            1 -> AutomationsScreen(
                                uiState = uiState,
                                onToggleWorkflow = { w -> viewModel.toggleWorkflow(w) },
                                onDeleteWorkflow = { w -> viewModel.deleteWorkflow(w) },
                                onSaveWorkflow = { w -> viewModel.saveWorkflow(w) },
                                onOpenPaywall = { showPaywallDialog = true }
                            )
                            2 -> MessagesScreen(
                                uiState = uiState,
                                onSaveTemplate = { title, content, cat, fav ->
                                    viewModel.saveTemplate(title, content, cat, fav)
                                },
                                onToggleFavorite = { t -> viewModel.toggleTemplateFavorite(t) },
                                onDeleteTemplate = { t -> viewModel.deleteTemplate(t) },
                                onRewriteWithAi = { text, tone, name, extra ->
                                    viewModel.rewriteMessageWithAi(text, tone, name, extra)
                                },
                                onGenerateWithAi = { brief ->
                                    viewModel.generateFromBriefWithAi(brief)
                                },
                                onCheckSpam = { text -> viewModel.checkSpamRisk(text) },
                                onClearAiResult = { viewModel.clearAiResult() },
                                onCreateCampaign = { name, tpl, tag, delay ->
                                    viewModel.createCampaign(name, tpl, tag, delay)
                                },
                                onImportContactsFromFile = { uri, mime, name ->
                                    viewModel.startAiContactExtractionFromFile(uri, mime, name, this@MainActivity)
                                },
                                onImportContactsFromPreset = { preset ->
                                    viewModel.startAiContactExtractionFromPreset(preset)
                                },
                                onSaveManualContact = { name, phone, tag, notes ->
                                    viewModel.saveContact(name, phone, tag, notes)
                                },
                                onDeleteContact = { contact ->
                                    viewModel.deleteContact(contact)
                                },
                                onOpenPaywall = { showPaywallDialog = true },
                                onScheduleClick = { showScheduleDialog = true }
                            )
                            3 -> VaultScreen(
                                uiState = uiState,
                                onUnlockWithPin = { pin -> viewModel.unlockVault(pin) },
                                onUnlockWithBiometrics = { viewModel.unlockVaultWithBiometrics() },
                                onSetupPin = { /* Handled in Settings */ },
                                onLockVault = { viewModel.lockVault() },
                                onExcludeChat = { chat -> viewModel.addExcludedChat(chat) },
                                onClearVault = { viewModel.clearVault() },
                                onDeleteNotification = { id -> viewModel.deleteVaultNotification(id) },
                                onSimulateCapture = { sender, msg, isDeleted ->
                                    viewModel.simulateCaptureNotification(sender, msg, isDeleted)
                                },
                                onGenerateSmartReplies = { msg, sender ->
                                    viewModel.generateSmartRepliesForMessage(msg, sender)
                                },
                                onDismissSmartReplies = { viewModel.dismissSmartReplies() },
                                onSummarizeChat = { sender -> viewModel.summarizeChat(sender) },
                                onDismissChatSummary = { viewModel.dismissChatSummary() },
                                onOpenPaywall = { showPaywallDialog = true }
                            )
                            4 -> SettingsScreen(
                                uiState = uiState,
                                onOpenPaywall = { showPaywallDialog = true },
                                onTogglePro = { pro -> viewModel.setPremium(pro) },
                                onSetupPin = { pin -> viewModel.setupVaultPin(pin) },
                                onToggleBiometric = { bio -> viewModel.setBiometricEnabled(bio) },
                                onUpdateSafetySettings = { limit, delay, active ->
                                    viewModel.updateSafetySettings(limit, delay, active)
                                },
                                onSetHighThinking = { high -> viewModel.setHighThinkingEnabled(high) },
                                onSetAutoDeleteDays = { days -> viewModel.setAutoDeleteDays(days) },
                                onClearVault = { viewModel.clearVault() },
                                onImportContacts = {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            )
                        }
                    }
                }

                if (showScheduleDialog) {
                    ScheduleMessageDialog(
                        templates = uiState.templates,
                        contacts = uiState.contacts,
                        onDismiss = { showScheduleDialog = false },
                        onSchedule = { name, phone, text, time, rec ->
                            viewModel.scheduleMessage(name, phone, text, time, rec)
                        }
                    )
                }

                if (showPaywallDialog) {
                    SubscriptionPaywallDialog(
                        isPro = uiState.isPremium,
                        onDismiss = { showPaywallDialog = false },
                        onTogglePro = { pro -> viewModel.setPremium(pro) }
                    )
                }

                if (uiState.showAiContactImportSheet) {
                    com.example.ui.dialogs.AiContactImportDialog(
                        isLoading = uiState.isExtractingContacts,
                        sourceFileName = uiState.importSourceFileName,
                        sourceType = uiState.importSourceType,
                        extractedContacts = uiState.extractedContactsPreview,
                        onToggleSelect = { idx -> viewModel.toggleExtractedContactSelection(idx) },
                        onUpdateTag = { idx, tag -> viewModel.updateExtractedContactTag(idx, tag) },
                        onConfirmImport = { viewModel.confirmImportContacts() },
                        onDismiss = { viewModel.dismissAiContactImportSheet() }
                    )
                }
            }
        }
    }
}
