package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WAApplication
import com.example.data.ai.ChatSummaryResult
import com.example.data.ai.ExtractedContact
import com.example.data.ai.GeminiAiService
import com.example.data.ai.RewriteTone
import com.example.data.ai.SmartReply
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppRepository
import com.example.data.security.VaultSecurityManager
import com.example.data.util.DocumentContactParser
import com.example.engine.ScheduledMessageWorker
import com.example.engine.WhatsAppDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val scheduledMessages: List<ScheduledMessageEntity> = emptyList(),
    val templates: List<MessageTemplateEntity> = emptyList(),
    val vaultNotifications: List<NotificationVaultEntity> = emptyList(),
    val workflows: List<AutomationWorkflowEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val campaigns: List<CampaignEntity> = emptyList(),
    val vaultDeletedCount: Int = 0,
    val vaultIncomingCount: Int = 0,
    val vaultOutgoingCount: Int = 0,
    val isPremium: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val vaultPinConfigured: Boolean = false,
    val isVaultUnlocked: Boolean = true, // true if no PIN set, otherwise locked until auth
    val todaySentCount: Int = 0,
    val dailySafetyLimit: Int = 50,
    val delayBetweenMessagesSec: Int = 8,
    val highThinkingEnabled: Boolean = true,
    val autoDeleteDays: Int = 14,
    val safeguardsActive: Boolean = true,
    val excludedChats: Set<String> = emptySet(),
    val isGeneratingAi: Boolean = false,
    val aiResultText: String? = null,
    val aiError: String? = null,
    val spamRiskAnalysis: AppRepository.SpamRiskAnalysis? = null,
    val activeSmartReplies: List<SmartReply> = emptyList(),
    val selectedReplyTargetMessage: String? = null,
    val selectedReplyTargetSender: String? = null,
    val isGeneratingReplies: Boolean = false,
    val activeChatSummary: ChatSummaryResult? = null,
    val isGeneratingSummary: Boolean = false,
    val isExtractingContacts: Boolean = false,
    val extractedContactsPreview: List<ExtractedContact> = emptyList(),
    val importSourceFileName: String? = null,
    val importSourceType: String? = null,
    val showAiContactImportSheet: Boolean = false,
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appInstance: WAApplication? = application as? WAApplication
    private val repository: AppRepository = appInstance?.let {
        try { it.repository } catch (e: Throwable) { null }
    } ?: AppRepository(application, AppDatabase.getInstance(application), VaultSecurityManager(application))

    private val preferencesRepository: UserPreferencesRepository = appInstance?.let {
        try { it.preferencesRepository } catch (e: Throwable) { null }
    } ?: UserPreferencesRepository(application)

    private val securityManager: VaultSecurityManager = appInstance?.let {
        try { it.securityManager } catch (e: Throwable) { null }
    } ?: VaultSecurityManager(application)

    private val aiService: GeminiAiService = appInstance?.let {
        try { it.aiService } catch (e: Throwable) { null }
    } ?: GeminiAiService()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.allScheduledMessages.collect { list ->
                _uiState.update { it.copy(scheduledMessages = list) }
            }
        }
        viewModelScope.launch {
            repository.allTemplates.collect { list ->
                _uiState.update { it.copy(templates = list) }
            }
        }
        viewModelScope.launch {
            repository.allVaultNotifications.collect { list ->
                _uiState.update { it.copy(vaultNotifications = list) }
            }
        }
        viewModelScope.launch {
            repository.deletedVaultCount.collect { count ->
                _uiState.update { it.copy(vaultDeletedCount = count) }
            }
        }
        viewModelScope.launch {
            repository.incomingVaultCount.collect { count ->
                _uiState.update { it.copy(vaultIncomingCount = count) }
            }
        }
        viewModelScope.launch {
            repository.outgoingVaultCount.collect { count ->
                _uiState.update { it.copy(vaultOutgoingCount = count) }
            }
        }
        viewModelScope.launch {
            repository.allWorkflows.collect { list ->
                _uiState.update { it.copy(workflows = list) }
            }
        }
        viewModelScope.launch {
            repository.allContacts.collect { list ->
                _uiState.update { it.copy(contacts = list) }
            }
        }
        viewModelScope.launch {
            repository.allCampaigns.collect { list ->
                _uiState.update { it.copy(campaigns = list) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isPremium.collect { isPro ->
                _uiState.update { it.copy(isPremium = isPro) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.vaultPinHash.collect { hash ->
                val hasPin = !hash.isNullOrBlank()
                _uiState.update {
                    it.copy(
                        vaultPinConfigured = hasPin,
                        isVaultUnlocked = !hasPin || it.isVaultUnlocked
                    )
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isBiometricEnabled.collect { bio ->
                _uiState.update { it.copy(isBiometricEnabled = bio) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.todaySentCount.collect { count ->
                _uiState.update { it.copy(todaySentCount = count) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.dailySafetyLimit.collect { limit ->
                _uiState.update { it.copy(dailySafetyLimit = limit) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.delayBetweenMessagesSec.collect { delay ->
                _uiState.update { it.copy(delayBetweenMessagesSec = delay) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.highThinkingEnabled.collect { high ->
                _uiState.update { it.copy(highThinkingEnabled = high) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.autoDeleteDays.collect { days ->
                _uiState.update { it.copy(autoDeleteDays = days) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.safeguardsActive.collect { active ->
                _uiState.update { it.copy(safeguardsActive = active) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.excludedChats.collect { excluded ->
                _uiState.update { it.copy(excludedChats = excluded) }
            }
        }
    }

    // --- Message Scheduling ---
    fun scheduleMessage(
        recipientName: String,
        recipientPhone: String,
        messageText: String,
        scheduledTimestamp: Long,
        recurrence: String = "NONE"
    ) {
        viewModelScope.launch {
            // Free plan constraint check: limit to 3 active schedules
            val pendingCount = _uiState.value.scheduledMessages.count { it.status == "PENDING" }
            if (!_uiState.value.isPremium && pendingCount >= 3) {
                _uiState.update {
                    it.copy(snackbarMessage = "Free plan limit reached (3 pending). Upgrade to Pro for unlimited!")
                }
                return@launch
            }

            val entity = ScheduledMessageEntity(
                recipient = recipientPhone,
                recipientName = recipientName,
                messageText = messageText,
                scheduledTimestamp = scheduledTimestamp,
                recurrence = recurrence,
                status = "PENDING"
            )
            repository.scheduleMessage(entity)

            // Calculate delay for WorkManager notification
            val delay = (scheduledTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
            ScheduledMessageWorker.enqueueOneTimeCheck(getApplication(), delay)

            _uiState.update { it.copy(snackbarMessage = "Message scheduled successfully!") }
        }
    }

    fun cancelScheduledMessage(id: Long) {
        viewModelScope.launch {
            repository.updateMessageStatus(id, "CANCELLED")
            _uiState.update { it.copy(snackbarMessage = "Message cancelled") }
        }
    }

    fun deleteScheduledMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
            _uiState.update { it.copy(snackbarMessage = "Message deleted") }
        }
    }

    fun dispatchDirectly(context: Context, message: ScheduledMessageEntity) {
        viewModelScope.launch {
            val intent = WhatsAppDispatcher.createWhatsAppIntent(message.recipient, message.messageText)
            try {
                context.startActivity(intent)
                repository.updateMessageStatus(message.id, "SENT")
                preferencesRepository.incrementTodaySentCount()
                repository.recordOutgoingMessage(message.recipientName, message.recipient, message.messageText)
                _uiState.update { it.copy(snackbarMessage = "Dispatched via WhatsApp & tracked in Vault") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "WhatsApp not found or could not be opened.") }
            }
        }
    }

    fun recordOutgoingMessage(name: String, phone: String, messageText: String) {
        viewModelScope.launch {
            repository.recordOutgoingMessage(name, phone, messageText)
            preferencesRepository.incrementTodaySentCount()
        }
    }

    // --- Templates ---
    fun saveTemplate(title: String, content: String, category: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.saveTemplate(
                MessageTemplateEntity(
                    title = title,
                    content = content,
                    category = category,
                    isFavorite = isFavorite
                )
            )
            _uiState.update { it.copy(snackbarMessage = "Template saved") }
        }
    }

    fun toggleTemplateFavorite(template: MessageTemplateEntity) {
        viewModelScope.launch {
            repository.toggleTemplateFavorite(template.id, template.isFavorite)
        }
    }

    fun deleteTemplate(template: MessageTemplateEntity) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
            _uiState.update { it.copy(snackbarMessage = "Template removed") }
        }
    }

    // --- AI Assistant ---
    fun rewriteMessageWithAi(
        originalText: String,
        tone: RewriteTone,
        recipientName: String = "",
        extraPrompt: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAi = true, aiError = null) }
            val result = aiService.rewriteMessage(
                originalText = originalText,
                tone = tone,
                enableHighThinking = _uiState.value.highThinkingEnabled,
                recipientName = recipientName,
                extraInstructions = extraPrompt
            )
            result.onSuccess { rewritten ->
                _uiState.update {
                    it.copy(
                        isGeneratingAi = false,
                        aiResultText = rewritten,
                        spamRiskAnalysis = repository.analyzeSpamRisk(rewritten)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingAi = false,
                        aiError = "AI Generation failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun generateFromBriefWithAi(brief: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAi = true, aiError = null) }
            val result = aiService.generateFromBrief(brief, _uiState.value.highThinkingEnabled)
            result.onSuccess { text ->
                _uiState.update {
                    it.copy(
                        isGeneratingAi = false,
                        aiResultText = text,
                        spamRiskAnalysis = repository.analyzeSpamRisk(text)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingAi = false,
                        aiError = "AI Generation failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun checkSpamRisk(text: String) {
        val analysis = repository.analyzeSpamRisk(text)
        _uiState.update { it.copy(spamRiskAnalysis = analysis) }
    }

    fun clearAiResult() {
        _uiState.update { it.copy(aiResultText = null, aiError = null, spamRiskAnalysis = null) }
    }

    // --- Workflows ---
    fun saveWorkflow(workflow: AutomationWorkflowEntity) {
        viewModelScope.launch {
            if (!_uiState.value.isPremium && _uiState.value.workflows.size >= 2) {
                _uiState.update { it.copy(snackbarMessage = "Free plan allows up to 2 automations. Upgrade to Pro for unlimited!") }
                return@launch
            }
            repository.saveWorkflow(workflow)
            _uiState.update { it.copy(snackbarMessage = "Workflow saved") }
        }
    }

    fun toggleWorkflow(workflow: AutomationWorkflowEntity) {
        viewModelScope.launch {
            repository.setWorkflowEnabled(workflow.id, !workflow.isEnabled)
        }
    }

    fun deleteWorkflow(workflow: AutomationWorkflowEntity) {
        viewModelScope.launch {
            repository.deleteWorkflow(workflow)
            _uiState.update { it.copy(snackbarMessage = "Workflow deleted") }
        }
    }

    // --- Contacts & Campaigns ---
    fun addContact(name: String, phone: String, tag: String, notes: String = "") {
        viewModelScope.launch {
            repository.addContact(ContactEntity(name = name, phoneNumber = phone, tag = tag, notes = notes))
            _uiState.update { it.copy(snackbarMessage = "Contact added") }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
        }
    }

    fun importDeviceContacts() {
        viewModelScope.launch {
            val count = repository.importDeviceContacts()
            _uiState.update {
                it.copy(
                    snackbarMessage = if (count > 0) "Imported $count contacts" else "No contacts found or permission not granted"
                )
            }
        }
    }

    fun createCampaign(name: String, templateText: String, targetTag: String, delaySec: Int) {
        viewModelScope.launch {
            val targetContacts = _uiState.value.contacts.filter { it.tag.equals(targetTag, ignoreCase = true) }
            val campaign = CampaignEntity(
                name = name,
                templateText = templateText,
                targetTag = targetTag,
                totalRecipients = targetContacts.size.coerceAtLeast(1),
                delaySeconds = delaySec,
                status = "RUNNING"
            )
            repository.createCampaign(campaign)
            _uiState.update { it.copy(snackbarMessage = "Campaign created with ${targetContacts.size} recipients!") }
        }
    }

    // --- Vault Security ---
    fun unlockVault(pin: String): Boolean {
        var success = false
        viewModelScope.launch {
            val storedHash = preferencesRepository.vaultPinHash.first()
            if (storedHash.isNullOrBlank() || storedHash == securityManager.hashPin(pin)) {
                _uiState.update { it.copy(isVaultUnlocked = true) }
                success = true
            } else {
                _uiState.update { it.copy(snackbarMessage = "Incorrect PIN") }
            }
        }
        return success
    }

    fun unlockVaultWithBiometrics() {
        _uiState.update { it.copy(isVaultUnlocked = true) }
    }

    fun lockVault() {
        if (_uiState.value.vaultPinConfigured) {
            _uiState.update { it.copy(isVaultUnlocked = false) }
        }
    }

    fun setupVaultPin(pin: String?) {
        viewModelScope.launch {
            val hash = if (!pin.isNullOrBlank()) securityManager.hashPin(pin) else null
            preferencesRepository.setVaultPin(hash)
            _uiState.update {
                it.copy(
                    vaultPinConfigured = hash != null,
                    isVaultUnlocked = true,
                    snackbarMessage = if (hash != null) "Vault PIN secured" else "Vault PIN removed"
                )
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearVault()
            _uiState.update { it.copy(snackbarMessage = "Vault notification history cleared") }
        }
    }

    fun deleteVaultNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteVaultNotification(id)
            _uiState.update { it.copy(snackbarMessage = "Archived notification removed") }
        }
    }

    fun simulateCaptureNotification(senderTitle: String, messageContent: String, isDeleted: Boolean = false) {
        viewModelScope.launch {
            if (isDeleted) {
                // Perform smart deletion marking identical to real WhatsApp listener
                val marked = repository.markDeletedSmart(senderTitle, senderTitle, System.currentTimeMillis() - (48 * 3600 * 1000L))
                _uiState.update {
                    it.copy(snackbarMessage = if (marked) "Deletion detected & flagged for: $senderTitle" else "Message deletion notice archived")
                }
            } else {
                val inserted = repository.storeVaultNotificationIfNotDuplicate(
                    NotificationVaultEntity(
                        packageName = "com.whatsapp",
                        senderTitle = senderTitle,
                        messageContent = messageContent,
                        timestamp = System.currentTimeMillis(),
                        isDeletedDetected = false,
                        chatKey = senderTitle
                    )
                )
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (inserted) "Captured notification into Vault: $senderTitle"
                        else "Duplicate notification filtered out: $senderTitle"
                    )
                }
            }
        }
    }

    fun generateSmartRepliesForMessage(messageText: String, sender: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingReplies = true,
                    activeSmartReplies = emptyList(),
                    selectedReplyTargetMessage = messageText,
                    selectedReplyTargetSender = sender
                )
            }
            val highThinking = _uiState.value.highThinkingEnabled
            val res = aiService.generateSmartReplies(messageText, sender, highThinking)
            res.onSuccess { replies ->
                _uiState.update { it.copy(isGeneratingReplies = false, activeSmartReplies = replies) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingReplies = false,
                        snackbarMessage = "AI reply generation: ${err.message}"
                    )
                }
            }
        }
    }

    fun dismissSmartReplies() {
        _uiState.update {
            it.copy(
                activeSmartReplies = emptyList(),
                selectedReplyTargetMessage = null,
                selectedReplyTargetSender = null,
                isGeneratingReplies = false
            )
        }
    }

    fun summarizeChat(sender: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true, activeChatSummary = null) }
            val history = repository.getDecryptedRecentChatHistory(sender, 15)
            val messages = history.map { "${it.senderTitle}: ${it.messageContent}" }
            val highThinking = _uiState.value.highThinkingEnabled
            val res = aiService.summarizeChatHistory(sender, messages, highThinking)
            res.onSuccess { summary ->
                _uiState.update { it.copy(isGeneratingSummary = false, activeChatSummary = summary) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isGeneratingSummary = false, snackbarMessage = "Failed to summarize chat: ${err.message}")
                }
            }
        }
    }

    fun dismissChatSummary() {
        _uiState.update { it.copy(activeChatSummary = null, isGeneratingSummary = false) }
    }

    fun addExcludedChat(chat: String) {
        viewModelScope.launch {
            preferencesRepository.addExcludedChat(chat)
            _uiState.update { it.copy(snackbarMessage = "Excluded '$chat' from Vault") }
        }
    }

    fun removeExcludedChat(chat: String) {
        viewModelScope.launch {
            preferencesRepository.removeExcludedChat(chat)
        }
    }

    // --- Contacts & AI Contact Extraction ---
    fun startAiContactExtractionFromFile(
        uri: Uri,
        mimeType: String?,
        fileName: String,
        context: Context
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtractingContacts = true,
                    importSourceFileName = fileName,
                    importSourceType = when {
                        mimeType?.contains("pdf") == true || fileName.endsWith(".pdf", true) -> "PDF Document"
                        mimeType?.startsWith("image/") == true || fileName.endsWith(".png", true) || fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "Image / OCR"
                        else -> "Excel / Spreadsheet"
                    },
                    extractedContactsPreview = emptyList(),
                    showAiContactImportSheet = true
                )
            }

            try {
                val parsed = DocumentContactParser.parseUri(context, uri, mimeType, fileName)
                val result = if (parsed.base64Image != null && parsed.docType == "IMAGE") {
                    aiService.extractContactsFromImage(parsed.base64Image, mimeType ?: "image/jpeg")
                } else if (parsed.base64Image != null && parsed.docType == "PDF") {
                    val imgRes = aiService.extractContactsFromImage(parsed.base64Image, "image/jpeg")
                    if (imgRes.isSuccess && imgRes.getOrNull()?.isNotEmpty() == true) {
                        imgRes
                    } else {
                        aiService.extractContactsFromText(parsed.textContent, "PDF Document", _uiState.value.highThinkingEnabled)
                    }
                } else {
                    aiService.extractContactsFromText(parsed.textContent, "Spreadsheet / Table", _uiState.value.highThinkingEnabled)
                }

                result.onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isExtractingContacts = false,
                            extractedContactsPreview = list,
                            snackbarMessage = "AI Extracted ${list.size} contacts from $fileName"
                        )
                    }
                }.onFailure {
                    val fallback = DocumentContactParser.heuristicExtractContacts(parsed.textContent)
                    _uiState.update {
                        it.copy(
                            isExtractingContacts = false,
                            extractedContactsPreview = fallback,
                            snackbarMessage = "Extracted ${fallback.size} contacts"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExtractingContacts = false,
                        snackbarMessage = "Error parsing document: ${e.message}"
                    )
                }
            }
        }
    }

    fun startAiContactExtractionFromPreset(presetType: String) {
        viewModelScope.launch {
            val (fileName, content) = DocumentContactParser.getPresetTestData(presetType)
            _uiState.update {
                it.copy(
                    isExtractingContacts = true,
                    importSourceFileName = fileName,
                    importSourceType = when (presetType) {
                        "PDF" -> "PDF Document"
                        "IMAGE" -> "Image / Business Card"
                        else -> "Excel / Spreadsheet"
                    },
                    extractedContactsPreview = emptyList(),
                    showAiContactImportSheet = true
                )
            }

            val result = aiService.extractContactsFromText(content, presetType, _uiState.value.highThinkingEnabled)
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isExtractingContacts = false,
                        extractedContactsPreview = list,
                        snackbarMessage = "AI extracted ${list.size} contacts from preset"
                    )
                }
            }.onFailure {
                val fallback = DocumentContactParser.heuristicExtractContacts(content)
                _uiState.update {
                    it.copy(
                        isExtractingContacts = false,
                        extractedContactsPreview = fallback,
                        snackbarMessage = "Loaded ${fallback.size} sample contacts"
                    )
                }
            }
        }
    }

    fun toggleExtractedContactSelection(index: Int) {
        _uiState.update { state ->
            val updated = state.extractedContactsPreview.toMutableList()
            if (index in updated.indices) {
                val cur = updated[index]
                updated[index] = cur.copy(isSelected = !cur.isSelected)
            }
            state.copy(extractedContactsPreview = updated)
        }
    }

    fun updateExtractedContactTag(index: Int, newTag: String) {
        _uiState.update { state ->
            val updated = state.extractedContactsPreview.toMutableList()
            if (index in updated.indices) {
                val cur = updated[index]
                updated[index] = cur.copy(tag = newTag)
            }
            state.copy(extractedContactsPreview = updated)
        }
    }

    fun confirmImportContacts() {
        viewModelScope.launch {
            val selected = _uiState.value.extractedContactsPreview.filter { it.isSelected }
            if (selected.isEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "No contacts selected for import") }
                return@launch
            }
            val entities = selected.map { c ->
                ContactEntity(
                    name = c.name,
                    phoneNumber = c.phoneNumber,
                    tag = c.tag,
                    notes = c.notes,
                    source = _uiState.value.importSourceType ?: "AI Import"
                )
            }
            repository.addContacts(entities)
            _uiState.update {
                it.copy(
                    showAiContactImportSheet = false,
                    extractedContactsPreview = emptyList(),
                    snackbarMessage = "Successfully imported ${entities.size} contacts into WA Automation!"
                )
            }
        }
    }

    fun dismissAiContactImportSheet() {
        _uiState.update {
            it.copy(
                showAiContactImportSheet = false,
                isExtractingContacts = false,
                extractedContactsPreview = emptyList()
            )
        }
    }

    fun saveContact(name: String, phone: String, tag: String, notes: String) {
        viewModelScope.launch {
            repository.addContact(
                ContactEntity(
                    name = name,
                    phoneNumber = phone,
                    tag = tag,
                    notes = notes,
                    source = "Manual"
                )
            )
            _uiState.update { it.copy(snackbarMessage = "Contact saved: $name") }
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact.id)
            _uiState.update { it.copy(snackbarMessage = "Contact deleted: ${contact.name}") }
        }
    }

    // --- Settings & Monetization ---
    fun setPremium(isPro: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPremium(isPro)
            _uiState.update {
                it.copy(
                    isPremium = isPro,
                    snackbarMessage = if (isPro) "Unlocked WA Automation Pro!" else "Switched to Free tier"
                )
            }
        }
    }

    fun updateSafetySettings(dailyLimit: Int, delaySec: Int, safeguardsActive: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailySafetyLimit(dailyLimit)
            preferencesRepository.setDelayBetweenMessages(delaySec)
            preferencesRepository.setSafeguardsActive(safeguardsActive)
            _uiState.update { it.copy(snackbarMessage = "Safety safeguards updated") }
        }
    }

    fun setHighThinkingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHighThinkingEnabled(enabled)
        }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setAutoDeleteDays(days)
            repository.pruneOldVaultRecords(days)
            _uiState.update { it.copy(snackbarMessage = "Auto-delete retention updated ($days days)") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
