package com.example.data.ai

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class RewriteTone(val label: String, val promptInstruction: String) {
    PROFESSIONAL("Professional", "Rewrite the message in a polished, courteous, and professional business tone suitable for clients."),
    FRIENDLY("Friendly & Warm", "Rewrite the message in an approachable, warm, and cordial tone while maintaining clarity."),
    CONCISE("Concise & Direct", "Condense the message to be punchy, clear, and under 2-3 sentences without losing key information."),
    FORMAL("Formal / Corporate", "Make the message formal, respectful, and structured for corporate communication."),
    PROMOTIONAL("High-Converting Sales", "Make the message compelling, persuasive, and action-oriented with a clear call-to-action."),
    URGENT("Gentle Urgency", "Rewrite to gently emphasize prompt response, timeline sensitivity, or deadline.")
}

data class SmartReply(
    val title: String,
    val text: String,
    val tone: String
)

data class ChatSummaryResult(
    val sender: String,
    val summary: String,
    val actionItems: List<String>,
    val sentiment: String,
    val urgencyLevel: String,
    val suggestedReply: String
)

data class ExtractedContact(
    val name: String,
    val phoneNumber: String,
    val tag: String = "Leads", // Customers, Leads, VIP, Follow-up
    val notes: String = "",
    val confidence: String = "High",
    val isSelected: Boolean = true
)

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/"

    /**
     * Rewrites message using Gemini 3.1 Pro Preview with HIGH thinking mode when requested,
     * or Gemini 3.5 Flash for fast rewriting.
     */
    suspend fun rewriteMessage(
        originalText: String,
        tone: RewriteTone,
        enableHighThinking: Boolean = true,
        recipientName: String = "",
        extraInstructions: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local heuristic rewrite when API key is not yet set
            val localRewrite = generateLocalRewrite(originalText, tone, recipientName)
            return@withContext Result.success(localRewrite)
        }

        val model = if (enableHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val prompt = buildString {
            append("You are an expert WhatsApp productivity copywriter.\n")
            append("Instruction: ${tone.promptInstruction}\n")
            if (recipientName.isNotBlank()) {
                append("Target recipient placeholder: Use {name} or $recipientName.\n")
            }
            if (extraInstructions.isNotBlank()) {
                append("Additional requirements: $extraInstructions\n")
            }
            append("Keep the output formatted nicely for WhatsApp (support clean emojis, bullet points, and *bold* syntax where fitting).\n")
            append("Output ONLY the rewritten message content without introductory or concluding conversational chat.\n\n")
            append("Original message:\n\"$originalText\"")
        }

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            configObj.put("temperature", 0.7)
            if (enableHighThinking) {
                val thinkingObj = JSONObject()
                thinkingObj.put("thinkingLevel", "HIGH")
                configObj.put("thinkingConfig", thinkingObj)
            }
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallback = generateLocalRewrite(originalText, tone, recipientName)
                    return@withContext Result.success("$fallback\n\n*(Heuristic Mode: ${response.code})*")
                }
                val respStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val respJson = JSONObject(respStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")?.trim()
                    if (!text.isNullOrBlank()) {
                        return@withContext Result.success(text)
                    }
                }
                Result.success(generateLocalRewrite(originalText, tone, recipientName))
            }
        } catch (e: Exception) {
            // Local fallback
            Result.success(generateLocalRewrite(originalText, tone, recipientName))
        }
    }

    /**
     * Generates a complete message sequence or template from a brief user idea
     */
    suspend fun generateFromBrief(
        brief: String,
        enableHighThinking: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                "Hi {name},\n\nHope this message finds you well! Regarding $brief, let's connect at your earliest convenience.\n\nBest regards,\nWA Pro"
            )
        }

        val model = if (enableHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val prompt = "Create a high-converting, professional WhatsApp message based on this concept: \"$brief\". " +
                "Include personalization variables like {name} and {date}. " +
                "Format clearly for WhatsApp. Return ONLY the message."

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            if (enableHighThinking) {
                configObj.put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
            }
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val text = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim()

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.success("Hello {name},\n\nFollowing up on our conversation regarding $brief. Please let me know what day works best to review!")
                }
            }
        } catch (e: Exception) {
            Result.success("Hello {name},\n\nFollowing up on our conversation regarding $brief. Looking forward to your thoughts!")
        }
    }

    /**
     * Generates 3 intelligent, contextual smart reply variations for an incoming message
     */
    suspend fun generateSmartReplies(
        incomingMessage: String,
        sender: String,
        enableHighThinking: Boolean = true
    ): Result<List<SmartReply>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(generateLocalSmartReplies(incomingMessage, sender))
        }

        val model = if (enableHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val prompt = "You are an intelligent WhatsApp assistant. The user received this message from '$sender':\n" +
                "\"$incomingMessage\"\n\n" +
                "Generate exactly 3 smart reply suggestions for WhatsApp:\n" +
                "1. Professional acknowledgment & next step\n" +
                "2. Friendly, cordial response\n" +
                "3. Short, direct confirmation\n\n" +
                "Output as strict JSON format with an array 'replies' containing objects with keys: 'title', 'text', 'tone'."

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            if (enableHighThinking) {
                configObj.put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
            }
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val rawText = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim() ?: ""

                val cleanJson = if (rawText.contains("{")) {
                    val start = rawText.indexOf("{")
                    val end = rawText.lastIndexOf("}")
                    rawText.substring(start, end + 1)
                } else rawText

                val parsed = JSONObject(cleanJson)
                val array = parsed.optJSONArray("replies") ?: JSONArray()
                val list = mutableListOf<SmartReply>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SmartReply(
                            title = obj.optString("title", "Reply #${i + 1}"),
                            text = obj.optString("text", "Thank you, noted!"),
                            tone = obj.optString("tone", "General")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    Result.success(list)
                } else {
                    Result.success(generateLocalSmartReplies(incomingMessage, sender))
                }
            }
        } catch (e: Exception) {
            Result.success(generateLocalSmartReplies(incomingMessage, sender))
        }
    }

    /**
     * Summarizes recent WhatsApp chat history with action items and sentiment
     */
    suspend fun summarizeChatHistory(
        sender: String,
        messages: List<String>,
        enableHighThinking: Boolean = true
    ): Result<ChatSummaryResult> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY" || messages.isEmpty()) {
            return@withContext Result.success(
                ChatSummaryResult(
                    sender = sender,
                    summary = "Recent thread with $sender covers ${messages.size} messages regarding coordination and updates.",
                    actionItems = listOf("Review latest request from $sender", "Confirm timeline or send follow-up"),
                    sentiment = "Neutral / Business Positive",
                    urgencyLevel = if (messages.any { it.contains("urgent", true) || it.contains("asap", true) }) "High (Urgent)" else "Normal",
                    suggestedReply = "Hi $sender, thanks for the update. Reviewing this now and getting right back to you!"
                )
            )
        }

        val model = if (enableHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val formattedChat = messages.joinToString("\n- ") { it.take(200) }
        val prompt = "You are an executive WhatsApp assistant. Analyze this chat history with '$sender':\n" +
                "- $formattedChat\n\n" +
                "Produce a structured JSON summary with the following keys:\n" +
                "- 'summary': 2-sentence executive summary\n" +
                "- 'actionItems': string array of key actionable tasks or questions asked\n" +
                "- 'sentiment': e.g. 'Positive', 'Inquiring', 'Concerned', or 'Neutral'\n" +
                "- 'urgencyLevel': 'Low', 'Normal', 'Urgent', or 'Critical'\n" +
                "- 'suggestedReply': best recommended immediate WhatsApp response"

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            if (enableHighThinking) {
                configObj.put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
            }
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val rawText = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim() ?: ""

                val cleanJson = if (rawText.contains("{")) {
                    val start = rawText.indexOf("{")
                    val end = rawText.lastIndexOf("}")
                    rawText.substring(start, end + 1)
                } else rawText

                val parsed = JSONObject(cleanJson)
                val actionList = mutableListOf<String>()
                val itemsArr = parsed.optJSONArray("actionItems")
                if (itemsArr != null) {
                    for (i in 0 until itemsArr.length()) {
                        actionList.add(itemsArr.getString(i))
                    }
                }

                Result.success(
                    ChatSummaryResult(
                        sender = sender,
                        summary = parsed.optString("summary", "Summary of conversation with $sender."),
                        actionItems = if (actionList.isNotEmpty()) actionList else listOf("Follow up with $sender"),
                        sentiment = parsed.optString("sentiment", "Neutral"),
                        urgencyLevel = parsed.optString("urgencyLevel", "Normal"),
                        suggestedReply = parsed.optString("suggestedReply", "Thank you $sender, I have received your messages and will update you shortly!")
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                ChatSummaryResult(
                    sender = sender,
                    summary = "Conversation contains ${messages.size} archived notifications from $sender.",
                    actionItems = listOf("Review conversation context", "Reply to pending question"),
                    sentiment = "Neutral",
                    urgencyLevel = "Normal",
                    suggestedReply = "Hi $sender, noted with thanks!"
                )
            )
        }
    }

    private fun generateLocalSmartReplies(incoming: String, sender: String): List<SmartReply> {
        val isQuestion = incoming.contains("?")
        val isUrgent = incoming.contains("urgent", true) || incoming.contains("asap", true) || incoming.contains("quick", true)

        val rep1 = if (isUrgent) {
            "Hi $sender, on it right away! Will update you in just a few minutes."
        } else if (isQuestion) {
            "Hello $sender, thank you for reaching out! Looking into this and will send you the details shortly."
        } else {
            "Thanks for the update, $sender! Noted and greatly appreciated."
        }

        val rep2 = "Hey $sender! Received your message. Let's touch base soon — hope you're having a great day!"
        val rep3 = "Noted, thank you $sender! 👍"

        return listOf(
            SmartReply("Professional", rep1, "Polished"),
            SmartReply("Warm / Friendly", rep2, "Cordial"),
            SmartReply("Quick Ack", rep3, "Direct")
        )
    }

    /**
     * Extracts structured WhatsApp contacts from text, tables, CSV, or document data using Gemini AI
     */
    suspend fun extractContactsFromText(
        rawContent: String,
        sourceDocType: String = "DOCUMENT",
        enableHighThinking: Boolean = true
    ): Result<List<ExtractedContact>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY" || rawContent.isBlank()) {
            val localExtracted = com.example.data.util.DocumentContactParser.heuristicExtractContacts(rawContent)
            return@withContext Result.success(localExtracted)
        }

        val model = if (enableHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val prompt = "You are an expert contact data extractor for a WhatsApp Automation app.\n" +
                "Carefully analyze this $sourceDocType content and extract ALL contacts:\n\n" +
                "\"\"\"\n${rawContent.take(12000)}\n\"\"\"\n\n" +
                "For each contact, determine:\n" +
                "- name: person's full name or business/company name\n" +
                "- phoneNumber: phone number in standard international format (e.g. +1 555-0192 or standard local format)\n" +
                "- tag: assign one of: 'VIP', 'Customers', 'Leads', 'Follow-up'\n" +
                "- notes: role, company, or any context found in the text\n\n" +
                "Return strictly valid JSON with an array 'contacts' of objects with keys: 'name', 'phoneNumber', 'tag', 'notes'."

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            if (enableHighThinking) {
                configObj.put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
            }
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val rawText = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim() ?: ""

                val cleanJson = if (rawText.contains("{")) {
                    val start = rawText.indexOf("{")
                    val end = rawText.lastIndexOf("}")
                    rawText.substring(start, end + 1)
                } else rawText

                val parsed = JSONObject(cleanJson)
                val contactsArray = parsed.optJSONArray("contacts") ?: JSONArray()
                val list = mutableListOf<ExtractedContact>()
                for (i in 0 until contactsArray.length()) {
                    val c = contactsArray.getJSONObject(i)
                    val name = c.optString("name", "Contact ${i + 1}")
                    val phone = c.optString("phoneNumber", "")
                    if (phone.isNotBlank()) {
                        list.add(
                            ExtractedContact(
                                name = name,
                                phoneNumber = com.example.data.util.DocumentContactParser.sanitizePhoneNumber(phone),
                                tag = c.optString("tag", "Leads"),
                                notes = c.optString("notes", "Imported via Gemini AI ($sourceDocType)"),
                                confidence = "High (Gemini AI)"
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    Result.success(list)
                } else {
                    Result.success(com.example.data.util.DocumentContactParser.heuristicExtractContacts(rawContent))
                }
            }
        } catch (e: Exception) {
            Result.success(com.example.data.util.DocumentContactParser.heuristicExtractContacts(rawContent))
        }
    }

    /**
     * Extracts contacts from an image (business card, roster screenshot, flyer) using Gemini Multimodal Vision
     */
    suspend fun extractContactsFromImage(
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): Result<List<ExtractedContact>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY" || base64Image.isBlank()) {
            // Offline fallback demo
            val sample = com.example.data.util.DocumentContactParser.getPresetTestData("IMAGE").second
            return@withContext Result.success(com.example.data.util.DocumentContactParser.heuristicExtractContacts(sample))
        }

        // Use gemini-2.5-flash for multimodal vision per guidelines
        val model = "gemini-2.5-flash"
        val endpoint = "$baseUrl$model:generateContent?key=$apiKey"

        val prompt = "You are a professional contact card & business document scanner.\n" +
                "Extract all people and contact details visible in this image.\n" +
                "For each contact, return:\n" +
                "- name: person or business name\n" +
                "- phoneNumber: phone or WhatsApp number formatted with country code\n" +
                "- tag: 'VIP', 'Customers', 'Leads', or 'Follow-up'\n" +
                "- notes: job title, company name, address, or email\n\n" +
                "Return strictly valid JSON with an array 'contacts' of objects with keys: 'name', 'phoneNumber', 'tag', 'notes'."

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().put("text", prompt))

            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", mimeType)
            inlineDataObj.put("data", base64Image)
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            requestJson.put("generationConfig", configObj)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val rawText = respJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim() ?: ""

                val cleanJson = if (rawText.contains("{")) {
                    val start = rawText.indexOf("{")
                    val end = rawText.lastIndexOf("}")
                    rawText.substring(start, end + 1)
                } else rawText

                val parsed = JSONObject(cleanJson)
                val contactsArray = parsed.optJSONArray("contacts") ?: JSONArray()
                val list = mutableListOf<ExtractedContact>()
                for (i in 0 until contactsArray.length()) {
                    val c = contactsArray.getJSONObject(i)
                    val name = c.optString("name", "Card Contact ${i + 1}")
                    val phone = c.optString("phoneNumber", "")
                    if (phone.isNotBlank()) {
                        list.add(
                            ExtractedContact(
                                name = name,
                                phoneNumber = com.example.data.util.DocumentContactParser.sanitizePhoneNumber(phone),
                                tag = c.optString("tag", "Leads"),
                                notes = c.optString("notes", "Scanned via Gemini Vision OCR"),
                                confidence = "High (Gemini Vision)"
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            val sample = com.example.data.util.DocumentContactParser.getPresetTestData("IMAGE").second
            Result.success(com.example.data.util.DocumentContactParser.heuristicExtractContacts(sample))
        }
    }

    /**
     * Local smart heuristic rewriter when offline or API key pending
     */
    private fun generateLocalRewrite(original: String, tone: RewriteTone, recipientName: String): String {
        val targetName = if (recipientName.isNotBlank()) recipientName else "{name}"
        return when (tone) {
            RewriteTone.PROFESSIONAL ->
                "Hello $targetName,\n\nI hope this message finds you well. $original\n\nPlease let me know your thoughts at your convenience.\n\nBest regards,"
            RewriteTone.FRIENDLY ->
                "Hey $targetName! 😊\n\nHope you're having a wonderful week! Just wanted to share: $original\n\nTalk soon! 🙌"
            RewriteTone.CONCISE ->
                "Hi $targetName: ${original.replace("\n", " ").take(140).trim()}... Please let me know when you're free!"
            RewriteTone.FORMAL ->
                "Dear $targetName,\n\nI am writing to communicate the following update: $original\n\nThank you for your attention to this matter.\n\nSincerely,"
            RewriteTone.PROMOTIONAL ->
                "🚀 Special Update for $targetName!\n\n$original\n\n👉 Act now to secure this exclusive advantage today. Limited availability!"
            RewriteTone.URGENT ->
                "⚠️ Quick Attention Needed, $targetName:\n\n$original\n\nPlease confirm as soon as possible so we can proceed without delay!"
        }
    }
}
