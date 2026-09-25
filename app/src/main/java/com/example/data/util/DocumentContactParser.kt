package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.ai.ExtractedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object DocumentContactParser {

    data class ParsedDocumentResult(
        val docType: String, // "IMAGE", "PDF", "EXCEL_CSV", "TEXT"
        val fileName: String,
        val textContent: String = "",
        val base64Image: String? = null
    )

    /**
     * Reads and parses a file from Uri into either text content or an image representation for Gemini AI
     */
    suspend fun parseUri(context: Context, uri: Uri, mimeType: String?, fileName: String): ParsedDocumentResult =
        withContext(Dispatchers.IO) {
            val resolvedMime = mimeType?.lowercase() ?: ""
            val lowerName = fileName.lowercase()

            when {
                resolvedMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> {
                    val base64 = readImageAsBase64(context, uri)
                    ParsedDocumentResult(
                        docType = "IMAGE",
                        fileName = fileName,
                        base64Image = base64
                    )
                }

                resolvedMime.contains("pdf") || lowerName.endsWith(".pdf") -> {
                    // Try to render first page of PDF using native PdfRenderer
                    val pdfBitmap = renderPdfFirstPage(context, uri)
                    val base64 = pdfBitmap?.let { bitmapToBase64(it) }
                    val extractedText = readRawTextFromStream(context, uri)
                    ParsedDocumentResult(
                        docType = "PDF",
                        fileName = fileName,
                        textContent = extractedText,
                        base64Image = base64
                    )
                }

                resolvedMime.contains("excel") || resolvedMime.contains("spreadsheet") ||
                        lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") -> {
                    val text = readSpreadsheetText(context, uri, lowerName)
                    ParsedDocumentResult(
                        docType = "EXCEL_CSV",
                        fileName = fileName,
                        textContent = text
                    )
                }

                else -> {
                    // CSV, TSV, or plain text
                    val text = readRawTextFromStream(context, uri)
                    ParsedDocumentResult(
                        docType = "EXCEL_CSV",
                        fileName = fileName,
                        textContent = text
                    )
                }
            }
        }

    private fun readImageAsBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val scaled = scaleBitmapIfNeeded(original, 1024)
                bitmapToBase64(scaled)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        return try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()
                    scaleBitmapIfNeeded(bitmap, 1024)
                } else {
                    renderer.close()
                    pfd.close()
                    null
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun readSpreadsheetText(context: Context, uri: Uri, fileName: String): String {
        return try {
            if (fileName.endsWith(".xlsx")) {
                // XLSX is a zip file. Extract text from sharedStrings.xml if present
                val sb = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val zip = ZipInputStream(stream)
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.contains("sharedStrings.xml", ignoreCase = true) ||
                            entry.name.contains("sheet1.xml", ignoreCase = true)
                        ) {
                            val reader = BufferedReader(InputStreamReader(zip))
                            var line = reader.readLine()
                            while (line != null) {
                                // Extract XML text content inside <t>...</t> tags
                                val regex = Regex("<t[^>]*>(.*?)</t>")
                                regex.findAll(line).forEach { match ->
                                    sb.append(match.groupValues[1]).append(" ")
                                }
                                line = reader.readLine()
                            }
                            sb.append("\n")
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (sb.isNotEmpty()) return sb.toString()
            }
            readRawTextFromStream(context, uri)
        } catch (e: Exception) {
            readRawTextFromStream(context, uri)
        }
    }

    private fun readRawTextFromStream(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth = if (width > height) maxDim else (maxDim * ratio).toInt()
        val newHeight = if (height >= width) maxDim else (maxDim / ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Local heuristic contact extractor that analyzes CSV, VCF, or tabular text lines
     */
    fun heuristicExtractContacts(text: String, defaultTag: String = "Leads"): List<ExtractedContact> {
        val results = mutableListOf<ExtractedContact>()
        val phoneRegex = Regex("(?:\\+|00)?\\d{1,3}[\\s.-]?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}")

        val lines = text.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue

            // CSV detection (comma, semicolon, or tab)
            val parts = if (line.contains(",")) line.split(",")
            else if (line.contains("\t")) line.split("\t")
            else if (line.contains(";")) line.split(";")
            else emptyList()

            if (parts.size >= 2) {
                // Check if line is a header like "Name, Phone"
                if (parts[0].contains("name", ignoreCase = true) && parts[1].contains("phone", ignoreCase = true)) {
                    continue
                }

                var name = parts[0].trim().replace("\"", "")
                var phone = ""
                var tag = defaultTag
                var notes = ""

                // Find phone in other parts
                for (p in parts.drop(1)) {
                    val candidate = p.trim().replace("\"", "")
                    if (phoneRegex.containsMatchIn(candidate) && phone.isBlank()) {
                        phone = candidate
                    } else if (candidate.equals("VIP", true) || candidate.equals("Customers", true) ||
                        candidate.equals("Leads", true) || candidate.equals("Follow-up", true)
                    ) {
                        tag = candidate
                    } else if (notes.isBlank() && candidate.isNotBlank()) {
                        notes = candidate
                    }
                }

                if (name.isNotBlank() && phone.isNotBlank()) {
                    results.add(
                        ExtractedContact(
                            name = name,
                            phoneNumber = sanitizePhoneNumber(phone),
                            tag = tag,
                            notes = notes,
                            confidence = "High (Local CSV/Table)"
                        )
                    )
                    continue
                }
            }

            // Line-based regex search: Name: [John Doe] Phone: [+1 555 1234]
            val phoneMatch = phoneRegex.find(line)
            if (phoneMatch != null) {
                val phone = phoneMatch.value.trim()
                val potentialName = line.replace(phone, "").replace(":", "").replace("-", " ").trim()
                val cleanName = potentialName.take(30).ifBlank { "Contact ${results.size + 1}" }
                if (cleanName.length >= 2 && !cleanName.all { it.isDigit() }) {
                    results.add(
                        ExtractedContact(
                            name = cleanName,
                            phoneNumber = sanitizePhoneNumber(phone),
                            tag = defaultTag,
                            notes = "Extracted from document line",
                            confidence = "Medium (Pattern Match)"
                        )
                    )
                }
            }
        }

        return results.distinctBy { it.phoneNumber }
    }

    fun sanitizePhoneNumber(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        val digitsOnly = trimmed.replace(Regex("[^0-9+]"), "")
        return if (!digitsOnly.startsWith("+") && digitsOnly.length == 10 && digitsOnly[0] in '6'..'9') {
            "+91 $digitsOnly"
        } else if (!digitsOnly.startsWith("+") && digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            "+91 ${digitsOnly.substring(2)}"
        } else if (!digitsOnly.startsWith("+") && digitsOnly.length >= 10) {
            "+91 $digitsOnly"
        } else {
            trimmed
        }
    }

    /**
     * Built-in rich test presets for instant one-click AI contact extraction demonstration
     */
    fun getPresetTestData(presetType: String): Pair<String, String> {
        return when (presetType) {
            "EXCEL" -> {
                val csvContent = """
                    Full Name,Phone Number,Company / Role,Tag,Notes
                    Rahul Sharma,+91 98201 55012,Bharat Retail Ltd,VIP,Key retail distribution partner (Mumbai)
                    Priya Patel,+91 98795 44210,Ahmedabad Textiles,Customers,Active bulk order buyer
                    Vikram Singhania,+91 98100 88231,Singhania Enterprises,VIP,Managing Director (Delhi NCR)
                    Ananya Deshmukh,+91 99220 33145,Pune Tech Innovations,Leads,Inquired about automated WhatsApp customer support
                    Amitav Sengupta,+91 98300 77192,Kolkata Logistics Hub,Follow-up,Requested GST tax invoice and delivery updates
                    Sunita Iyer,+91 94440 12890,Chennai Cloud Labs,Customers,Annual subscription renewal due next month
                """.trimIndent()
                Pair("Indian_Business_Leads.xlsx (Spreadsheet)", csvContent)
            }
            "PDF" -> {
                val pdfContent = """
                    CONFEDERATION OF INDIAN INDUSTRY (CII) - DELEGATE DIRECTORY 2026
                    
                    Delegate 1:
                    Name: Rajeshwar Varma
                    Mobile: +91 98450 67123
                    Organization: Bengaluru Semiconductor Park
                    Category: VIP Partner
                    Remarks: Keynote Speaker - Made in India AI Initiative
                    
                    Delegate 2:
                    Name: Meera Nambiar
                    Mobile: +91 94470 55198
                    Organization: Kerala Agro Exports
                    Category: Leads
                    Remarks: Looking for automated WhatsApp export order status tracking
                    
                    Delegate 3:
                    Name: Harshwardhan Rathore
                    Mobile: +91 98290 41238
                    Organization: Jaipur Heritage Handicrafts
                    Category: Customers
                    Remarks: High value regular client for festive campaigns
                    
                    Delegate 4:
                    Name: Dr. Kavita Reddy
                    Mobile: +91 98490 22781
                    Organization: Hyderabad HealthTech
                    Category: VIP
                    Remarks: Founder & Chief Medical Officer
                """.trimIndent()
                Pair("CII_India_Summit_Delegates.pdf (Document)", pdfContent)
            }
            else -> {
                val imageOcrContent = """
                    ========================================
                    INDIAN EXECUTIVE BUSINESS CARD (SCANNED OCR)
                    ========================================
                    ARJUN KAPOOR
                    Senior Director, Digital Banking & UPI Solutions
                    Kotak & Indus Innovations
                    
                    Direct WhatsApp: +91 98210 99450
                    Office: +91 22 6123 4567
                    Email: arjun.kapoor@indusinnovations.in
                    Branch: Bandra Kurla Complex (BKC), Mumbai
                    Category: VIP
                    ========================================
                    DEEPIKA CHAUHAN
                    Head of Marketing & Brand Partnerships
                    Gurugram D2C Growth Labs
                    WhatsApp: +91 98110 33499
                    Category: Leads
                """.trimIndent()
                Pair("Visiting_Card_Scan_India.png (Image OCR)", imageOcrContent)
            }
        }
    }
}
