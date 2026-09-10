package com.example.utils

import android.graphics.Bitmap
import android.util.Log
import com.example.api.ParsedContact
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.regex.Pattern

/**
 * Helper to process physical business cards 100% on-device (offline)
 * using Google ML Kit Text Recognition and specialized Brazilian contact extraction heuristics.
 */
object OfflineCardScanner {

    private const val TAG = "OfflineCardScanner"

    // Lazily initialize ML Kit Latin Text Recognizer (on-device)
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun analyzeCardOffline(bitmap: Bitmap): ParsedContact {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = suspendCancellableCoroutine<Text> { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) {
                            continuation.resume(text)
                        }
                    }
                    .addOnFailureListener { ex ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(ex)
                        }
                    }
            }
            extractContactData(result)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no reconhecimento de texto offline: ${e.message}", e)
            throw e
        }
    }

    private fun extractContactData(visionText: Text): ParsedContact {
        val fullText = visionText.text
        val allLines = mutableListOf<String>()

        visionText.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val trimmed = line.text.trim()
                if (trimmed.isNotEmpty()) {
                    allLines.add(trimmed)
                }
            }
        }

        // 1. Phone extraction
        val phonesFound = extractPhoneNumbers(allLines)
        var primaryPhone = ""
        var secondaryPhone = ""

        if (phonesFound.isNotEmpty()) {
            primaryPhone = phonesFound[0]
            if (phonesFound.size > 1) {
                secondaryPhone = phonesFound[1]
            }
        }

        // 2. Instagram extraction
        val instagram = extractInstagram(allLines, fullText)

        // 3. Address extraction
        val address = extractAddress(allLines)

        // 4. Name extraction
        val name = extractName(allLines, primaryPhone, secondaryPhone, instagram, address)

        // 5. Observations / Services extraction
        val observations = extractObservations(allLines, name, primaryPhone, secondaryPhone, instagram, address)

        return ParsedContact(
            name = name,
            primaryPhone = primaryPhone,
            secondaryPhone = secondaryPhone,
            address = address,
            observations = observations,
            instagram = instagram
        )
    }

    /**
     * Extracts phone numbers matching Brazilian mobile/landline patterns
     * Prioritizes numbers near 'WhatsApp', 'Whats', 'Zap', or WhatsApp icon symbols.
     */
    private fun extractPhoneNumbers(lines: List<String>): List<String> {
        val phoneRegex = Regex("""(?:\+?55\s*)?(?:\(?([1-9]{2})\)?\s*)?(?:(9\d{4})|(\d{4}))[\s.-]?(\d{4})""")
        val whatsAppKeywords = listOf("whatsapp", "whats", "zap", "wpp", "contato", "celular", "tel")

        val priorityPhones = mutableListOf<String>()
        val regularPhones = mutableListOf<String>()

        for (line in lines) {
            val lower = line.lowercase()
            val hasWhatsAppKeyword = whatsAppKeywords.any { lower.contains(it) }

            val matches = phoneRegex.findAll(line)
            for (match in matches) {
                val rawDigits = match.value.replace(Regex("""\D"""), "")
                // Standard Brazilian phone should be between 8 and 13 digits (with 55 + DDD + 8/9 digits)
                if (rawDigits.length in 8..13) {
                    val formatted = formatBrazilianPhone(rawDigits)
                    if (hasWhatsAppKeyword) {
                        if (!priorityPhones.contains(formatted)) priorityPhones.add(formatted)
                    } else {
                        if (!regularPhones.contains(formatted)) regularPhones.add(formatted)
                    }
                }
            }
        }

        val all = mutableListOf<String>()
        all.addAll(priorityPhones)
        for (p in regularPhones) {
            if (!all.contains(p)) {
                all.add(p)
            }
        }
        return all
    }

    private fun formatBrazilianPhone(digits: String): String {
        var clean = digits
        // Remove national country code 55 if present at start and total length > 11
        if (clean.startsWith("55") && clean.length >= 12) {
            clean = clean.substring(2)
        }

        return when (clean.length) {
            11 -> "(${clean.substring(0, 2)}) ${clean.substring(2, 7)}-${clean.substring(7)}"
            10 -> "(${clean.substring(0, 2)}) ${clean.substring(2, 6)}-${clean.substring(6)}"
            9 -> "${clean.substring(0, 5)}-${clean.substring(5)}"
            8 -> "${clean.substring(0, 4)}-${clean.substring(4)}"
            else -> clean
        }
    }

    /**
     * Extracts Instagram handles from '@handle' or 'instagram.com/handle' or lines near 'Instagram'
     */
    private fun extractInstagram(lines: List<String>, fullText: String): String {
        // Pattern 1: @username
        val atRegex = Regex("""@([a-zA-Z0-9._]{3,30})""")
        val atMatch = atRegex.find(fullText)
        if (atMatch != null) {
            return atMatch.groupValues[1]
        }

        // Pattern 2: instagram.com/username
        val urlRegex = Regex("""instagram\.com/([a-zA-Z0-9._]{3,30})""", RegexOption.IGNORE_CASE)
        val urlMatch = urlRegex.find(fullText)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }

        // Pattern 3: Line preceded or containing 'instagram' or 'insta'
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()
            if (lower.contains("instagram") || lower.contains("insta")) {
                val cleaned = line.replace(Regex("""(?i)(instagram|insta|:|@)"""), "").trim()
                if (cleaned.isNotEmpty() && !cleaned.contains(" ")) {
                    return cleaned
                }
                // Check next line
                if (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (next.isNotEmpty() && !next.contains(" ") && next.length in 3..30) {
                        return next.replace("@", "")
                    }
                }
            }
        }

        return ""
    }

    /**
     * Extracts addresses by looking for street types (Rua, Av, Rodovia, etc.), CEP or city patterns
     */
    private fun extractAddress(lines: List<String>): String {
        val addressTriggers = listOf(
            "rua", "r.", "av.", "avenida", "rodovia", "rod.", "estrada", "travessa",
            "trav.", "alameda", "al.", "bairro", "lote", "quadra", "qd.", "lt.", "sala",
            "bloco", "km", "cep", "nº", "numero", "centro"
        )

        val foundLines = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            if (addressTriggers.any { lower.contains(it) } || lower.matches(Regex(""".*\d{5}-?\d{3}.*"""))) {
                foundLines.add(line)
            }
        }

        return if (foundLines.isNotEmpty()) {
            foundLines.joinToString(", ")
        } else {
            ""
        }
    }

    /**
     * Selects best line as Name/Company: usually first prominent text line that is not a phone,
     * address, email, website or Instagram.
     */
    private fun extractName(
        lines: List<String>,
        primaryPhone: String,
        secondaryPhone: String,
        instagram: String,
        address: String
    ): String {
        val ignoredKeywords = listOf(
            "whatsapp", "whats", "zap", "tel", "telefone", "cel", "contato",
            "instagram", "insta", "facebook", "fb", "email", "e-mail",
            "www.", "http", ".com", ".br", "rua", "av.", "avenida", "cep"
        )

        val cleanPrimary = primaryPhone.replace(Regex("""\D"""), "")
        val cleanSecondary = secondaryPhone.replace(Regex("""\D"""), "")

        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()

            // Skip if contains phone digits
            val lineDigits = trimmed.replace(Regex("""\D"""), "")
            if (cleanPrimary.isNotEmpty() && lineDigits.contains(cleanPrimary)) continue
            if (cleanSecondary.isNotEmpty() && lineDigits.contains(cleanSecondary)) continue

            // Skip if contains ignored keywords
            if (ignoredKeywords.any { lower.contains(it) }) continue

            // Skip if part of detected address or instagram
            if (address.isNotEmpty() && address.contains(trimmed, ignoreCase = true)) continue
            if (instagram.isNotEmpty() && lower.contains(instagram.lowercase())) continue

            // A candidate name usually has at least 3 characters and some letters
            if (trimmed.length >= 3 && trimmed.any { it.isLetter() }) {
                return trimmed
            }
        }

        return lines.firstOrNull { it.any { c -> c.isLetter() } } ?: ""
    }

    /**
     * Extracts services or bullet points (e.g. 'Mecânica, Freio, Suspensão')
     */
    private fun extractObservations(
        lines: List<String>,
        name: String,
        primaryPhone: String,
        secondaryPhone: String,
        instagram: String,
        address: String
    ): String {
        val cleanPrimary = primaryPhone.replace(Regex("""\D"""), "")
        val cleanSecondary = secondaryPhone.replace(Regex("""\D"""), "")

        val observationLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()

            if (trimmed == name) continue
            if (cleanPrimary.isNotEmpty() && trimmed.replace(Regex("""\D"""), "").contains(cleanPrimary)) continue
            if (cleanSecondary.isNotEmpty() && trimmed.replace(Regex("""\D"""), "").contains(cleanSecondary)) continue
            if (address.isNotEmpty() && address.contains(trimmed, ignoreCase = true)) continue
            if (instagram.isNotEmpty() && lower.contains(instagram.lowercase())) continue
            if (lower.startsWith("www.") || lower.contains("@") || lower.contains("http")) continue

            // Check if it looks like a list of services or descriptions
            if (trimmed.contains("•") || trimmed.contains("-") || trimmed.contains(",") || trimmed.contains("/") ||
                trimmed.split(" ").size >= 2
            ) {
                // Avoid lines that are purely phones or URLs
                if (!lower.contains("tel") && !lower.contains("whats")) {
                    observationLines.add(trimmed)
                }
            }
        }

        return observationLines.take(3).joinToString(" | ")
    }
}
