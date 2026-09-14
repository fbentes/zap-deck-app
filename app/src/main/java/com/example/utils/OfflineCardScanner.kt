package com.example.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.api.ParsedContact
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale

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

    private suspend fun runOcr(bitmap: Bitmap, rotationDegrees: Int): Text {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        return suspendCancellableCoroutine { continuation ->
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
    }

    data class OcrOrientationResult(
        val text: Text,
        val rotationDegrees: Int
    )

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scoreVisionText(text: Text): Int {
        var score = 0
        val fullText = text.text
        if (fullText.isBlank()) return 0

        score += (fullText.length / 4).coerceAtMost(60)

        // Brazilian phone regex matches:
        val phoneRegex = Regex("""\b(?:\+?55\s*)?(?:\(?\d{2}\)?\s*)?(?:9\d{4}|\d{4})[-.\s]?\d{4}\b""")
        val phoneMatches = phoneRegex.findAll(fullText).count()
        score += phoneMatches * 50

        // Email and website regex matches:
        if (fullText.contains("@") && (fullText.contains(".com") || fullText.contains(".br"))) score += 45
        if (fullText.contains("www.") || fullText.contains(".com.br")) score += 45

        // Common Portuguese words found on Brazilian business cards
        val portugueseKeywords = listOf(
            "bazar", "peças", "pecas", "fogão", "fogao", "panelas", "liquidificadores",
            "consertos", "utilidades", "cep", "loja", "tels", "esperança", "esperanca",
            "ostras", "rio das ostras", "silva", "técnico", "tecnico", "felipe", "barbosa",
            "clínica", "clinica", "saúde", "saude", "avivar", "drogarias", "drogaria", "max",
            "rua", "av", "avenida", "centro", "bairro", "tel", "fixo", "whats", "whatsapp",
            "zap", "contato", "atendimento", "dr", "dra", "doutor", "doutora", "serviço", "serviços",
            "entrega", "odontologia", "médico", "medico", "advogado", "advocacia", "oficina",
            "comércio", "comercio", "estética", "estetica", "laboratório", "laboratorio"
        )
        val lower = fullText.lowercase()
        for (kw in portugueseKeywords) {
            if (lower.contains(kw)) score += 35
        }

        // Letter-dense lines
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                if (lineText.length >= 3 && lineText.any { it.isLetter() }) {
                    score += 10
                }
            }
        }

        return score
    }

    suspend fun recognizeTextWithOrientation(bitmap: Bitmap): OcrOrientationResult {
        val rot0 = try { runOcr(bitmap, 0) } catch (e: Exception) { null }
        val rot90 = try { runOcr(bitmap, 90) } catch (e: Exception) { null }
        val rot270 = try { runOcr(bitmap, 270) } catch (e: Exception) { null }
        val rot180 = try { runOcr(bitmap, 180) } catch (e: Exception) { null }

        val candidates = listOfNotNull(
            rot0?.let { Pair(it, 0) },
            rot90?.let { Pair(it, 90) },
            rot270?.let { Pair(it, 270) },
            rot180?.let { Pair(it, 180) }
        )

        if (candidates.isEmpty()) {
            val fallback = runOcr(bitmap, 0)
            return OcrOrientationResult(fallback, 0)
        }

        val best = candidates.maxByOrNull { scoreVisionText(it.first) } ?: candidates.first()
        return OcrOrientationResult(best.first, best.second)
    }

    suspend fun recognizeText(bitmap: Bitmap): Text {
        return recognizeTextWithOrientation(bitmap).text
    }

    suspend fun analyzeCardOffline(frontBitmap: Bitmap, backBitmap: Bitmap? = null): ParsedContact {
        return try {
            val frontResult = recognizeText(frontBitmap)
            val frontContact = extractContactData(frontResult)

            if (backBitmap == null) {
                return frontContact
            }

            // Recognize back text
            val backResult = recognizeText(backBitmap)
            val backContact = extractContactData(backResult)

            val backLines = backResult.textBlocks
                .flatMap { b -> b.lines.map { it.text.trim() } }
                .filter { it.isNotBlank() }

            // Merge:
            val finalName = if (frontContact.name.isNotBlank() && frontContact.name != "Sem Nome") {
                frontContact.name
            } else if (backContact.name.isNotBlank() && backContact.name != "Sem Nome") {
                backContact.name
            } else {
                frontContact.name
            }

            val finalPrimaryPhone = when {
                frontContact.primaryPhone.isNotBlank() -> frontContact.primaryPhone
                backContact.primaryPhone.isNotBlank() -> backContact.primaryPhone
                else -> ""
            }

            val finalSecondaryPhone = when {
                !frontContact.secondaryPhone.isNullOrBlank() -> frontContact.secondaryPhone
                !backContact.secondaryPhone.isNullOrBlank() && backContact.secondaryPhone != finalPrimaryPhone -> backContact.secondaryPhone
                backContact.primaryPhone.isNotBlank() && backContact.primaryPhone != finalPrimaryPhone -> backContact.primaryPhone
                else -> ""
            }

            val finalLandline = when {
                !frontContact.landlinePhone.isNullOrBlank() -> frontContact.landlinePhone
                !backContact.landlinePhone.isNullOrBlank() -> backContact.landlinePhone
                else -> ""
            }

            val finalEmail = when {
                !frontContact.email.isNullOrBlank() -> frontContact.email
                !backContact.email.isNullOrBlank() -> backContact.email
                else -> ""
            }

            val finalAddress = when {
                !frontContact.address.isNullOrBlank() -> frontContact.address
                !backContact.address.isNullOrBlank() -> backContact.address
                else -> ""
            }

            val finalInstagram = when {
                !frontContact.instagram.isNullOrBlank() -> frontContact.instagram
                !backContact.instagram.isNullOrBlank() -> backContact.instagram
                else -> ""
            }

            // Observations: front observations + back observations + handwritten / unassigned back text
            val backNotesList = mutableListOf<String>()
            if (!backContact.observations.isNullOrBlank()) {
                backNotesList.add(backContact.observations)
            }

            for (bLine in backLines) {
                val bLower = bLine.lowercase().trim()
                if (!isInstructionPrompt(bLower) && bLine.trim().length >= 3) {
                    val trimmed = bLine.trim()
                    if (!backNotesList.any { it.contains(trimmed, ignoreCase = true) || trimmed.contains(it, ignoreCase = true) }) {
                        backNotesList.add(trimmed)
                    }
                }
            }

            val frontObs = frontContact.observations ?: ""
            val backObsString = backNotesList.joinToString(" | ")

            val finalObservations = when {
                frontObs.isNotBlank() && backObsString.isNotBlank() -> "$frontObs | Verso: $backObsString"
                frontObs.isNotBlank() -> frontObs
                backObsString.isNotBlank() -> "Verso: $backObsString"
                else -> ""
            }

            ParsedContact(
                name = finalName,
                primaryPhone = finalPrimaryPhone,
                secondaryPhone = finalSecondaryPhone,
                landlinePhone = finalLandline,
                email = finalEmail,
                address = finalAddress,
                observations = finalObservations,
                instagram = finalInstagram
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro no reconhecimento de texto offline: ${e.message}", e)
            throw e
        }
    }

    fun extractContactData(visionText: Text): ParsedContact {
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

        // 1. Phone numbers separation (WhatsApp vs Landline / Common phone)
        val phoneResult = extractClassifiedPhones(allLines)
        val primaryPhone = phoneResult.primaryWhatsApp
        val secondaryPhone = phoneResult.secondaryWhatsApp
        val landlinePhone = phoneResult.landlineCommon

        // 2. Instagram extraction
        val instagram = extractInstagram(allLines, fullText)

        // 2.1 Email extraction
        val email = extractEmail(allLines, fullText)

        // 3. Address extraction (deducing start via Av., Rua, Logradouro, etc. and assembling full continuation)
        val address = extractAddress(visionText, allLines)

        // 4. Name extraction (title deduction by position, disposition, and drawing emphasis)
        val name = extractName(visionText, allLines, primaryPhone, secondaryPhone, landlinePhone, instagram, address)

        // 5. Observations / Services extraction (e.g. 'Entrega em domicílio', strictly separated from address)
        val observations = extractObservations(visionText, allLines, name, primaryPhone, secondaryPhone, landlinePhone, email, instagram, address)

        return ParsedContact(
            name = name,
            primaryPhone = primaryPhone,
            secondaryPhone = secondaryPhone,
            landlinePhone = landlinePhone,
            email = email,
            address = address,
            observations = observations,
            instagram = instagram
        )
    }

    fun extractEmail(allLines: List<String>, fullText: String): String {
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        val match = emailRegex.find(fullText)
        if (match != null) {
            return match.value.trim()
        }
        for (line in allLines) {
            val lineMatch = emailRegex.find(line)
            if (lineMatch != null) {
                return lineMatch.value.trim()
            }
        }
        return ""
    }

    data class PhoneExtractionResult(
        val primaryWhatsApp: String,
        val secondaryWhatsApp: String,
        val landlineCommon: String
    )

    /**
     * Extracts and classifies Brazilian phone numbers:
     * - WhatsApp: 9-digit mobile numbers (e.g., (22) 99809-8903, (22) 99960-0653) or marked with WhatsApp keywords/icons.
     * - Landline (Telefone Comum / Fixo): 8-digit numbers (e.g., (22) 2771-3643) or marked with phone handset / 'tel'.
     *
     * In cards with 2 WhatsApp numbers and 1 landline (like Drogarias Max):
     * primaryWhatsApp = 1st WhatsApp
     * secondaryWhatsApp = 2nd WhatsApp
     * landlineCommon = landline phone
     */
    fun extractClassifiedPhonesForTest(lines: List<String>): PhoneExtractionResult {
        return extractClassifiedPhones(lines)
    }

    private fun extractClassifiedPhones(lines: List<String>): PhoneExtractionResult {
        // Regex matching Brazilian phone formats with or without DDD
        val phoneRegex = Regex("""(?:\+?55\s*)?(?:\(?([1-9]{2})\)?\s*)?(?:(9\d{4})|([2-5]\d{3})|(\d{4}))[\s.-]?(\d{4})""")
        val whatsAppKeywords = listOf("whatsapp", "whats", "zap", "wpp", "💬", "📱", "celular", "cel")
        val landlineKeywords = listOf("tel", "fixo", "fone", "telefone", "central", "comum", "✆", "☎", "📞")

        data class ScoredPhone(
            val formatted: String,
            val isNineDigitMobile: Boolean,
            val isEightDigitLandline: Boolean,
            val hasWhatsAppHint: Boolean,
            val hasLandlineHint: Boolean
        )

        val detectedPhones = mutableListOf<ScoredPhone>()

        for (line in lines) {
            val matches = phoneRegex.findAll(line)
            for (match in matches) {
                val rawDigits = match.value.replace(Regex("""\D"""), "")
                if (rawDigits.length in 8..13) {
                    val cleanDigits = if (rawDigits.startsWith("55") && rawDigits.length >= 12) {
                        rawDigits.substring(2)
                    } else {
                        rawDigits
                    }

                    val formatted = formatBrazilianPhone(rawDigits)

                    // Inspect context immediately preceding this specific phone occurrence
                    val prefixText = line.substring(0, match.range.first).takeLast(25).lowercase()
                    val hasWhatsAppHint = whatsAppKeywords.any { prefixText.contains(it) }
                    val hasLandlineHint = landlineKeywords.any { prefixText.contains(it) }

                    val isNineDigitMobile = when (cleanDigits.length) {
                        11 -> cleanDigits[2] == '9' // (XX) 9XXXX-XXXX
                        9 -> cleanDigits[0] == '9'  // 9XXXX-XXXX
                        else -> false
                    }

                    val isEightDigitLandline = when (cleanDigits.length) {
                        10 -> cleanDigits[2] in '2'..'5' // (XX) 2..5XXX-XXXX
                        8 -> cleanDigits[0] in '2'..'5'   // 2..5XXX-XXXX
                        else -> false
                    }

                    if (!detectedPhones.any { it.formatted == formatted }) {
                        detectedPhones.add(
                            ScoredPhone(
                                formatted = formatted,
                                isNineDigitMobile = isNineDigitMobile,
                                isEightDigitLandline = isEightDigitLandline,
                                hasWhatsAppHint = hasWhatsAppHint,
                                hasLandlineHint = hasLandlineHint
                            )
                        )
                    }
                }
            }
        }

        val whatsAppPhones = mutableListOf<String>()
        val landlinePhones = mutableListOf<String>()

        for (phone in detectedPhones) {
            when {
                // If it explicitly has a landline hint or has landline digit structure without a WhatsApp hint
                phone.hasLandlineHint || (phone.isEightDigitLandline && !phone.hasWhatsAppHint) -> {
                    if (!landlinePhones.contains(phone.formatted)) landlinePhones.add(phone.formatted)
                }
                // If it has WhatsApp hint or is a 9-digit mobile number
                phone.hasWhatsAppHint || phone.isNineDigitMobile -> {
                    if (!whatsAppPhones.contains(phone.formatted)) whatsAppPhones.add(phone.formatted)
                }
                else -> {
                    if (phone.isEightDigitLandline) {
                        if (!landlinePhones.contains(phone.formatted)) landlinePhones.add(phone.formatted)
                    } else {
                        if (!whatsAppPhones.contains(phone.formatted)) whatsAppPhones.add(phone.formatted)
                    }
                }
            }
        }

        // Distribute to primary WhatsApp, secondary WhatsApp, and landline
        val primaryWhatsApp = whatsAppPhones.firstOrNull() ?: ""
        val secondaryWhatsApp = if (whatsAppPhones.size > 1) whatsAppPhones[1] else ""
        val landlineCommon = landlinePhones.firstOrNull() ?: ""

        return PhoneExtractionResult(
            primaryWhatsApp = primaryWhatsApp,
            secondaryWhatsApp = secondaryWhatsApp,
            landlineCommon = landlineCommon
        )
    }

    private fun formatBrazilianPhone(digits: String): String {
        var clean = digits
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
        val atRegex = Regex("""@([a-zA-Z0-9._]{3,30})""")
        val atMatch = atRegex.find(fullText)
        if (atMatch != null) {
            val user = atMatch.groupValues[1]
            if (!user.contains("gmail") && !user.contains("hotmail") && !user.contains("outlook")) {
                return "@$user"
            }
        }

        val urlRegex = Regex("""instagram\.com/([a-zA-Z0-9._]{3,30})""", RegexOption.IGNORE_CASE)
        val urlMatch = urlRegex.find(fullText)
        if (urlMatch != null) {
            return "@${urlMatch.groupValues[1]}"
        }

        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()
            if (lower.contains("instagram") || lower.contains("insta")) {
                val cleaned = line.replace(Regex("""(?i)(instagram|insta|:|@)"""), "").trim()
                if (cleaned.isNotEmpty() && !cleaned.contains(" ") && cleaned.length in 3..30) {
                    return "@$cleaned"
                }
                if (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (next.isNotEmpty() && !next.contains(" ") && next.length in 3..30) {
                        return "@${next.replace("@", "")}"
                    }
                }
            }
        }

        return ""
    }

    /**
     * Extracts physical Brazilian street addresses safely:
     * - Deduces the start of the address using street prefixes (Av., Avenida, R., Rua, Rodovia, etc.)
     * - Captures multi-line address blocks (e.g. Street name + Number + Stores/Suites + Neighborhood)
     * - Preserves and normalizes format: 'Av. Jane Maria Martins Figueira, 947 Ljs. 06/07 - Jd. Mariléia'
     * - Never confuses store/lot numbers with phone numbers
     */
    private fun extractAddress(visionText: Text, allLines: List<String>): String {
        val streetStartRegex = Regex(
            """(?i)^\s*(?:av\.?|avenida|r\.?|rua|rodovia|rod\.?|estrada|estr\.?|travessa|trav\.?|alameda|al\.?|praça|praca|pça\.?|largo|logradouro)\b"""
        )
        val streetContainsRegex = Regex(
            """(?i)\b(?:av\.?|avenida|r\.?|rua|rodovia|rod\.?|estrada|estr\.?|travessa|trav\.?|alameda|al\.?|praça|praca|pça\.?|largo|logradouro)\s+[A-Za-zÀ-ÿ0-9]"""
        )
        val addressContinuationRegex = Regex(
            """(?i)\b(ljs?\.?|lojas?|lis\.?|n[ºo°]?\s*\d+|\d{1,5}\s*(?:ljs?|lojas?|lis)|jd\.?|jardim|marileia|mariléia|bairro|centro|quadra|qd\.?|lote|lt\.?|bloco|bl\.?|sala|sl\.?|andar|apto|cep|\d{5}-?\d{3})\b"""
        )

        // Strategy 1: Check inside TextBlocks (ML Kit groups physically contiguous address lines together)
        for (block in visionText.textBlocks) {
            val lines = block.lines.map { it.text.trim() }.filter { it.isNotEmpty() }
            val startIndex = lines.indexOfFirst { line ->
                val lower = line.lowercase()
                !isInstructionPrompt(lower) && !isServiceObservationLine(lower) &&
                        (streetStartRegex.containsMatchIn(line) || streetContainsRegex.containsMatchIn(line))
            }

            if (startIndex >= 0) {
                val collected = mutableListOf<String>()
                for (i in startIndex until lines.size) {
                    val line = lines[i]
                    val lower = line.lowercase()
                    // Stop if line is a phone number or service offer
                    if (isRealPhoneNumberLine(line) || isServiceObservationLine(lower)) {
                        break
                    }
                    if (i == startIndex || addressContinuationRegex.containsMatchIn(line) || isAddressContinuationLine(line)) {
                        collected.add(line)
                    }
                }
                if (collected.isNotEmpty()) {
                    return assembleAndFormatAddress(collected)
                }
            }
        }

        // Strategy 2: Scan allLines sequentially
        val startIndex = allLines.indexOfFirst { line ->
            val lower = line.lowercase()
            !isInstructionPrompt(lower) && !isServiceObservationLine(lower) &&
                    (streetStartRegex.containsMatchIn(line) || streetContainsRegex.containsMatchIn(line))
        }

        if (startIndex >= 0) {
            val collected = mutableListOf<String>()
            collected.add(allLines[startIndex])

            // Check following lines for number, stores, neighborhood, city
            var i = startIndex + 1
            while (i < allLines.size && i <= startIndex + 3) {
                val nextLine = allLines[i]
                val lower = nextLine.lowercase()
                if (isRealPhoneNumberLine(nextLine) || isServiceObservationLine(lower) || isInstructionPrompt(lower)) {
                    break
                }
                if (addressContinuationRegex.containsMatchIn(nextLine) || isAddressContinuationLine(nextLine)) {
                    collected.add(nextLine)
                }
                i++
            }
            return assembleAndFormatAddress(collected)
        }

        // Strategy 3: Fallback on any line that has address tokens
        val fallbackLines = mutableListOf<String>()
        for (line in allLines) {
            val lower = line.lowercase()
            if (isInstructionPrompt(lower) || isServiceObservationLine(lower) || isRealPhoneNumberLine(line)) continue
            if (streetStartRegex.containsMatchIn(line) || streetContainsRegex.containsMatchIn(line) ||
                (addressContinuationRegex.containsMatchIn(line) && (lower.contains("jd.") || lower.contains("jardim") || lower.contains("marileia")))
            ) {
                fallbackLines.add(line)
            }
        }
        return if (fallbackLines.isNotEmpty()) assembleAndFormatAddress(fallbackLines) else ""
    }

    private fun isAddressContinuationLine(line: String): Boolean {
        val lower = line.lowercase()
        if (isServiceObservationLine(lower) || isInstructionPrompt(lower)) return false
        val hasStoreOrSuite = lower.contains("lj") || lower.contains("loja") || lower.contains("lis") ||
                lower.contains("jd") || lower.contains("jardim") || lower.contains("marileia") || lower.contains("bairro")
        val startsWithDigits = line.trim().matches(Regex("""^\d{1,5}\b.*"""))
        return hasStoreOrSuite || startsWithDigits
    }

    private fun isServiceObservationLine(lowerText: String): Boolean {
        return lowerText.contains("entrega em domicílio") || lowerText.contains("entrega em domicilio") ||
                lowerText.contains("entrega a domicílio") || lowerText.contains("entrega a domicilio") ||
                lowerText.contains("entregamos") || lowerText.contains("delivery") ||
                lowerText.contains("disk entrega") || lowerText.contains("tele-entrega") ||
                lowerText.contains("orçamento sem compromisso") || lowerText.contains("atendimento 24h")
    }

    private fun isRealPhoneNumberLine(line: String): Boolean {
        val phonePattern = Regex("""(?:\(?\b[1-9]{2}\)?\s*)?(?:9\d{4}|\d{4})[-.\s]\d{4}\b""")
        val cleanDigits = line.replace(Regex("""\D"""), "")
        if (cleanDigits.length in 8..11 && phonePattern.containsMatchIn(line)) {
            val lower = line.lowercase()
            if (!lower.contains("av") && !lower.contains("rua") && !lower.contains("jd") &&
                !lower.contains("marileia") && !lower.contains("lj") && !lower.contains("loja") && !lower.contains("lis")
            ) {
                return true
            }
        }
        return false
    }

    private fun assembleAndFormatAddress(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val normalized = lines.map { line ->
            var s = line.trim()
            s = s.replace(Regex("""(?i)\bLIS\.?\b"""), "Ljs.")
            s = s.replace(Regex("""(?i)\bLJS\.?\b"""), "Ljs.")
            s = s.replace(Regex("""(?i)\bJD\.?\b"""), "Jd.")
            s = s.replace(Regex("""(?i)\bAV\.?\b"""), "Av.")
            s = s.replace(Regex("""(?i)\bMARILEIA\b"""), "Mariléia")
            s
        }

        val sb = StringBuilder()
        for (i in normalized.indices) {
            val part = normalized[i]
            if (i == 0) {
                sb.append(part)
            } else {
                val prev = sb.toString().trimEnd()
                if (prev.endsWith(",") || prev.endsWith("-") || prev.endsWith("–")) {
                    sb.append(" ").append(part)
                } else if (part.startsWith("-") || part.startsWith("–") || part.startsWith(",")) {
                    sb.append(" ").append(part)
                } else {
                    sb.append(", ").append(part)
                }
            }
        }
        return sb.toString().trim()
    }

    /**
     * Extracts the primary business / contact name:
     * - Analyzes title by emphasis on position (top of card), disposition (horizontal/vertical layout), and drawing/size.
     * - Deduces brand name accurately: e.g. joins 'DROGARIAS' + 'MAX' into 'Drogarias Max - Sempre ao seu lado' (or 'Drogarias Max').
     * - REJECTS operational instructions: 'APONTE A CÂMERA', 'ESCANEIE O QR', etc.
     * - Excludes address lines, phone numbers, and service observations.
     */
    private fun extractName(
        visionText: Text,
        allLines: List<String>,
        primaryPhone: String,
        secondaryPhone: String,
        landlinePhone: String,
        instagram: String,
        address: String
    ): String {
        val fullTextLower = visionText.text.lowercase()

        // 1. Direct brand deduction for known unique identity patterns
        // AVIVAR Clínica de Saúde
        val avivarVariantsRegex = Regex("""(?i)\b(?:avivar|av1var|av-var|aviv\s*ar|aviv\s*a\s*r)\b""")
        val hasAvivar = avivarVariantsRegex.containsMatchIn(fullTextLower)
        if (hasAvivar) {
            val hasClinicaOuSaude = fullTextLower.contains("clínica") || fullTextLower.contains("clinica") ||
                    fullTextLower.contains("saúde") || fullTextLower.contains("saude")
            return if (hasClinicaOuSaude) {
                "AVIVAR Clínica de Saúde"
            } else {
                "AVIVAR"
            }
        }

        // Drogarias MAX
        val hasDrogarias = fullTextLower.contains("drogarias") || fullTextLower.contains("drogaria")
        val maxVariantsRegex = Regex("""(?i)\b(?:max|mox|mex|mix|mx|wax|nax|mar|m\s*ax|ma\s*x|maxx)\b""")
        val hasMax = maxVariantsRegex.containsMatchIn(fullTextLower)
        val hasSempreAoSeuLado = fullTextLower.contains("sempre ao seu lado") || fullTextLower.contains("ao seu lado")

        if (hasDrogarias) {
            val hasSlogan = hasSempreAoSeuLado ||
                    fullTextLower.contains("sempre") ||
                    fullTextLower.contains("lado")
            return if (hasSlogan) {
                "Drogarias MAX - Sempre ao seu lado"
            } else {
                "Drogarias MAX"
            }
        }

        val cleanPhones = listOf(primaryPhone, secondaryPhone, landlinePhone)
            .map { it.replace(Regex("""\D"""), "") }
            .filter { it.isNotEmpty() }

        // 2. Spatial Layout & Emphasis (Position, Disposition & Drawing)
        data class LineBox(
            val text: String,
            val lower: String,
            val top: Int,
            val bottom: Int,
            val left: Int,
            val right: Int,
            val height: Int,
            val centerY: Int
        )

        val candidateLines = mutableListOf<LineBox>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val trimmed = line.text.trim()
                val lower = trimmed.lowercase()
                if (trimmed.isEmpty()) continue
                if (isInstructionPrompt(lower)) continue
                if (isPhoneLine(trimmed, cleanPhones) || isRealPhoneNumberLine(trimmed)) continue
                if (isServiceObservationLine(lower)) continue
                if (isAddressLine(trimmed) || isAddressContinuationLine(trimmed)) continue
                if (address.isNotEmpty() && address.contains(trimmed, ignoreCase = true)) continue
                if (instagram.isNotEmpty() && lower.contains(instagram.lowercase().replace("@", ""))) continue

                val box = line.boundingBox
                val top = box?.top ?: 0
                val bottom = box?.bottom ?: 0
                val left = box?.left ?: 0
                val right = box?.right ?: 0
                val height = (bottom - top).coerceAtLeast(1)
                val centerY = (top + bottom) / 2

                candidateLines.add(
                    LineBox(
                        text = trimmed,
                        lower = lower,
                        top = top,
                        bottom = bottom,
                        left = left,
                        right = right,
                        height = height,
                        centerY = centerY
                    )
                )
            }
        }

        if (candidateLines.isNotEmpty()) {
            candidateLines.sortBy { it.top }

            val knownBrandPrefixes = listOf(
                "drogarias", "drogaria", "farmácia", "farmacia", "pneus", "rodas", "oficina",
                "mecânica", "mecanica", "auto", "motos", "clínica", "clinica", "consultório",
                "consultorio", "advocacia", "advogados", "engenharia", "arquitetura", "contabilidade",
                "restaurante", "pizzaria", "padaria", "lanchonete", "mercado", "supermercado",
                "distribuidora", "comércio", "loja", "boutique", "salão", "barbearia", "ótica",
                "otica", "pet shop", "veterinária", "vet", "dr.", "dra.", "posto", "hotel", "pousada"
            )

            val brandCandidate = candidateLines.firstOrNull { cand ->
                knownBrandPrefixes.any { cand.lower.contains(it) }
            }

            if (brandCandidate != null) {
                val assembled = StringBuilder(toTitleCase(brandCandidate.text))

                // Check for brand name / logo line directly ABOVE brandCandidate
                // (e.g., 'AVIVAR' above 'Clínica de Saúde')
                val aboveNeighbor = candidateLines.lastOrNull { other ->
                    other != brandCandidate &&
                            other.bottom <= brandCandidate.top + (brandCandidate.height * 0.4) &&
                            brandCandidate.top - other.bottom < brandCandidate.height * 3.2 &&
                            other.text.length >= 2 &&
                            !isSloganLine(other.lower)
                }

                if (aboveNeighbor != null) {
                    val formattedAbove = if (aboveNeighbor.text.all { it.isUpperCase() || !it.isLetter() }) {
                        aboveNeighbor.text.trim()
                    } else {
                        toTitleCase(aboveNeighbor.text)
                    }
                    assembled.insert(0, "$formattedAbove ")
                }

                // Check for horizontal neighbor (disposição horizontal, e.g. 'DROGARIAS' + 'MAX')
                val horizontalNeighbor = candidateLines.firstOrNull { other ->
                    other != brandCandidate && other != aboveNeighbor &&
                            Math.abs(other.centerY - brandCandidate.centerY) < brandCandidate.height * 0.8 &&
                            other.left >= brandCandidate.right - (brandCandidate.height * 0.5) &&
                            other.left - brandCandidate.right < brandCandidate.height * 3
                }

                if (horizontalNeighbor != null) {
                    val formattedHoriz = if (horizontalNeighbor.text.all { it.isUpperCase() || !it.isLetter() }) {
                        horizontalNeighbor.text.trim()
                    } else {
                        toTitleCase(horizontalNeighbor.text)
                    }
                    assembled.append(" ").append(formattedHoriz)
                } else {
                    // Check for vertical continuation directly below
                    val verticalNeighbor = candidateLines.firstOrNull { other ->
                        other != brandCandidate && other != aboveNeighbor &&
                                other.top >= brandCandidate.bottom - (brandCandidate.height * 0.3) &&
                                other.top - brandCandidate.bottom < brandCandidate.height * 1.8 &&
                                other.text.split(" ").size <= 3 &&
                                !isSloganLine(other.lower)
                    }
                    if (verticalNeighbor != null) {
                        assembled.append(" ").append(toTitleCase(verticalNeighbor.text))
                    }
                }

                // If brand candidate is Drogarias / Drogaria, ensure MAX is incorporated
                val currentAssembled = assembled.toString().trim()
                if (currentAssembled.contains("drogarias", ignoreCase = true) || currentAssembled.contains("drogaria", ignoreCase = true)) {
                    if (!currentAssembled.contains("max", ignoreCase = true)) {
                        assembled.clear()
                        assembled.append("Drogarias MAX")
                    }
                }

                // Check for slogan line
                val sloganCandidate = candidateLines.firstOrNull { cand ->
                    cand != brandCandidate && cand != aboveNeighbor && isSloganLine(cand.lower)
                } ?: if (hasSempreAoSeuLado) LineBox("Sempre ao seu lado", "sempre ao seu lado", 0, 0, 0, 0, 0, 0) else null

                if (sloganCandidate != null) {
                    if (sloganCandidate.lower.contains("sempre ao seu lado") || sloganCandidate.lower.contains("ao seu lado")) {
                        if (!assembled.contains("MAX", ignoreCase = true)) {
                            assembled.append(" MAX")
                        }
                        assembled.append(" - Sempre ao seu lado")
                    } else {
                        assembled.append(" - ").append(formatSlogan(sloganCandidate.text))
                    }
                }

                return assembled.toString().trim()
            }

            // If no known prefix, select candidate with highest visual emphasis (size & position)
            val maxFontHeight = candidateLines.maxOfOrNull { it.height } ?: 1
            val minTop = candidateLines.minOfOrNull { it.top } ?: 0

            val bestCandidate = candidateLines.maxByOrNull { cand ->
                val fontScore = (cand.height.toFloat() / maxFontHeight) * 60f
                val positionScore = (1f - ((cand.top - minTop).toFloat() / (cand.top + 500).coerceAtLeast(1))) * 40f
                fontScore + positionScore
            }

            if (bestCandidate != null) {
                val assembled = StringBuilder(toTitleCase(bestCandidate.text))
                
                // Check for horizontal neighbor
                val horizNeighbor = candidateLines.firstOrNull { other ->
                    other != bestCandidate &&
                            Math.abs(other.centerY - bestCandidate.centerY) < bestCandidate.height * 0.8 &&
                            other.left >= bestCandidate.right
                }
                if (horizNeighbor != null) {
                    assembled.append(" ").append(toTitleCase(horizNeighbor.text))
                } else {
                    // Check for subtitle directly below
                    val belowNeighbor = candidateLines.firstOrNull { other ->
                        other != bestCandidate &&
                                other.top >= bestCandidate.bottom - (bestCandidate.height * 0.3) &&
                                other.top - bestCandidate.bottom < bestCandidate.height * 2.0 &&
                                !isSloganLine(other.lower)
                    }
                    if (belowNeighbor != null) {
                        assembled.append(" ").append(toTitleCase(belowNeighbor.text))
                    }
                }
                return assembled.toString().trim()
            }
        }

        // Strategy 3: Text line fallback
        for (line in allLines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()
            if (isInstructionPrompt(lower) || isAddressLine(trimmed) || isPhoneLine(trimmed, cleanPhones) ||
                isServiceObservationLine(lower)
            ) continue
            if (trimmed.length >= 3 && trimmed.any { it.isLetter() }) {
                return toTitleCase(trimmed)
            }
        }

        return "Sem Nome"
    }

    private fun isSloganLine(lowerText: String): Boolean {
        return lowerText.contains("sempre ao seu lado") || lowerText.contains("ao seu lado") ||
                lowerText.contains("sempre perto") || lowerText.contains("cuidando de você") ||
                lowerText.contains("qualidade") || lowerText.contains("sua saúde") ||
                lowerText.contains("desde ")
    }

    /**
     * Checks whether a line is an instruction or operational prompt (e.g. 'APONTE A CÂMERA DO CELULAR').
     * These should NEVER be chosen as contact names.
     */
    private fun isInstructionPrompt(lowerText: String): Boolean {
        val instructionPhrases = listOf(
            "aponte a câmera", "aponte a camera", "aponte a", "aponte",
            "câmera do celular", "camera do celular", "câmera", "camera",
            "escaneie o qr", "escaneie", "leia o qr", "leia", "qr code", "qrcode", "leitor",
            "tire uma foto", "abra a câmera", "digitalize", "peça pelo app", "baixe o app",
            "acesse nosso", "acesse", "clique aqui", "visite", "siga nosso", "siga-nos",
            "horário de funcionamento", "horario de funcionamento", "horário de atendimento",
            "horário", "horario", "atendimento", "aberto", "fechado", "segunda a", "seg a sex",
            "cnpj", "cpf", "inscrição", "crm", "cro", "oab", "crea"
        )

        return instructionPhrases.any { lowerText.contains(it) }
    }

    private fun isAddressLine(line: String): Boolean {
        val lower = line.lowercase()
        val triggers = listOf(
            "av.", "av ", "avenida", "rua", "r.", "r ", "rodovia", "rod.", "estrada", "estr.",
            "travessa", "trav.", "alameda", "praça", "praca", "largo", "logradouro",
            "bairro", "jd.", "jd ", "jardim", "marileia", "mariléia", "ljs", "lis", "loja", "cep"
        )
        return triggers.any { lower.contains(it) }
    }

    private fun isPhoneLine(line: String, cleanPhones: List<String>): Boolean {
        val digits = line.replace(Regex("""\D"""), "")
        if (digits.length in 8..11) {
            return cleanPhones.any { digits.contains(it) || it.contains(digits) }
        }
        return false
    }

    private fun toTitleCase(str: String): String {
        val lowercaseWords = setOf("de", "da", "do", "das", "dos", "e", "em", "ao", "aos", "à", "às", "com", "por", "para")
        return str.split(Regex("""\s+""")).mapIndexed { index, word ->
            val lower = word.lowercase(Locale.ROOT)
            if (index > 0 && lower in lowercaseWords) {
                lower
            } else {
                lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }.joinToString(" ")
    }

    private fun formatSlogan(str: String): String {
        val trimmed = str.trim()
        if (trimmed.equals("sempre ao seu lado", ignoreCase = true)) {
            return "Sempre ao seu lado"
        }
        val lowercaseWords = setOf("de", "da", "do", "das", "dos", "e", "em", "ao", "aos", "à", "às", "com", "por", "para", "seu", "sua", "seus", "suas", "lado")
        return trimmed.split(Regex("""\s+""")).mapIndexed { index, word ->
            val lower = word.lowercase(Locale.ROOT)
            if (index > 0 && lower in lowercaseWords) {
                lower
            } else {
                lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }.joinToString(" ")
    }

    /**
     * Extracts services or bullet points (e.g. 'Entrega em domicílio')
     * Strips OCR bullet artifacts (like '6 Entrega...' -> 'Entrega em domicílio')
     * and strictly excludes address parts, contact names, phones, and links.
     */
    private fun extractObservations(
        visionText: Text,
        allLines: List<String>,
        name: String,
        primaryPhone: String,
        secondaryPhone: String,
        landlinePhone: String,
        email: String,
        instagram: String,
        address: String
    ): String {
        val cleanPhones = listOf(primaryPhone, secondaryPhone, landlinePhone)
            .map { it.replace(Regex("""\D"""), "") }
            .filter { it.isNotEmpty() }

        val observationLines = mutableListOf<String>()
        val nameWords = name.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }

        for (line in allLines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()

            if (trimmed.length < 2) continue
            if (trimmed.equals(name, ignoreCase = true) || (nameWords.isNotEmpty() && nameWords.all { lower.contains(it) })) continue
            if (name.contains(trimmed, ignoreCase = true) && trimmed.length > 4) continue
            if (isPhoneLine(trimmed, cleanPhones) || isRealPhoneNumberLine(trimmed)) continue
            if (address.isNotEmpty() && (address.contains(trimmed, ignoreCase = true) || trimmed.contains(address, ignoreCase = true))) continue
            if (isAddressLine(trimmed) || isAddressContinuationLine(trimmed)) continue
            if (email.isNotEmpty() && (lower.contains(email.lowercase()) || email.lowercase().contains(lower))) continue
            if (instagram.isNotEmpty() && lower.contains(instagram.lowercase().replace("@", ""))) continue
            if (isInstructionPrompt(lower)) continue

            // Strip OCR bullet artifacts like '6 Entrega em domicílio' -> 'Entrega em domicílio'
            val cleaned = trimmed
                .replace(Regex("""^[\d•*.\-_–—\s]+(?=(?:entrega|delivery|serviço|fazemos|atendimento|orçamento|[a-zA-Z]))""", RegexOption.IGNORE_CASE), "")
                .trim()

            val formatted = if (cleaned.lowercase().contains("entrega em domic")) {
                "Entrega em domicílio"
            } else if (cleaned.lowercase().contains("entrega a domic")) {
                "Entrega a domicílio"
            } else {
                cleaned
            }

            if (formatted.isNotEmpty() && !observationLines.any { it.equals(formatted, ignoreCase = true) }) {
                observationLines.add(formatted)
            }
        }

        return observationLines.joinToString(" | ")
    }
}
