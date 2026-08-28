package com.example.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.ContactEntity
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64

object CardBeamTransferHelper {

    const val MIME_TYPE_ZAPDECK = "application/vnd.com.aistudio.zapdeck.card"
    const val URI_SCHEME_ZAPDECK = "zapdeck"

    /**
     * Converts a ContactEntity into a compressed JSON string representation
     * suitable for NFC NDEF payload and QR Code transmission.
     */
    fun contactToJson(contact: ContactEntity, includeImage: Boolean = true): String {
        val json = JSONObject()
        json.put("name", contact.name)
        json.put("primaryPhone", contact.primaryPhone)
        json.put("secondaryPhone", contact.secondaryPhone)
        json.put("address", contact.address)
        json.put("instagram", contact.instagram)
        json.put("observations", contact.observations)
        json.put("useWhatsAppBusiness", contact.useWhatsAppBusiness)
        json.put("instagramFollowed", contact.instagramFollowed)
        if (includeImage && contact.imageBase64.isNotEmpty()) {
            json.put("imageBase64", contact.imageBase64)
        }
        return json.toString()
    }

    /**
     * Parses a JSON string back into a ContactEntity object
     */
    fun jsonToContact(jsonStr: String): ContactEntity? {
        return try {
            val json = JSONObject(jsonStr)
            ContactEntity(
                id = 0,
                name = json.optString("name", ""),
                primaryPhone = json.optString("primaryPhone", ""),
                secondaryPhone = json.optString("secondaryPhone", ""),
                address = json.optString("address", ""),
                instagram = json.optString("instagram", ""),
                observations = json.optString("observations", ""),
                imageBase64 = json.optString("imageBase64", ""),
                useWhatsAppBusiness = json.optBoolean("useWhatsAppBusiness", false),
                instagramFollowed = json.optBoolean("instagramFollowed", false)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a vCard 3.0 standard text representation of the contact.
     * Native Android & iOS devices will automatically recognize and open this in their contacts app if scanned.
     */
    fun contactToVCard(contact: ContactEntity, includeImage: Boolean = true): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:${contact.name}")
        if (contact.primaryPhone.isNotBlank()) {
            sb.appendLine("TEL;TYPE=CELL,VOICE:${contact.primaryPhone}")
        }
        if (contact.secondaryPhone.isNotBlank()) {
            sb.appendLine("TEL;TYPE=WORK,VOICE:${contact.secondaryPhone}")
        }
        if (contact.address.isNotBlank()) {
            sb.appendLine("ADR;TYPE=WORK:;;${contact.address.replace(";", " ")};;;;")
        }
        if (contact.instagram.isNotBlank()) {
            sb.appendLine("X-SOCIALPROFILE;TYPE=instagram:https://instagram.com/${contact.instagram.replace("@", "")}")
        }
        if (contact.observations.isNotBlank()) {
            sb.appendLine("NOTE:${contact.observations.replace("\n", " ")}")
        }
        if (includeImage && contact.imageBase64.isNotBlank()) {
            val cleanBase64 = contact.imageBase64.replace("\r", "").replace("\n", "")
            sb.appendLine("PHOTO;ENCODING=b;TYPE=JPEG:$cleanBase64")
        }
        sb.appendLine("END:VCARD")
        return sb.toString()
    }

    /**
     * Parse vCard text into a ContactEntity
     */
    fun vCardToContact(vcardStr: String): ContactEntity {
        var name = ""
        var primaryPhone = ""
        var secondaryPhone = ""
        var address = ""
        var instagram = ""
        var observations = ""
        var imageBase64 = ""

        val lines = vcardStr.lines()
        var parsingPhoto = false
        val photoSb = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            if (parsingPhoto) {
                if (trimmed.startsWith(" ") || trimmed.startsWith("\t") || (!trimmed.contains(":") && trimmed.length > 10)) {
                    photoSb.append(trimmed)
                    continue
                } else {
                    parsingPhoto = false
                    imageBase64 = photoSb.toString()
                }
            }

            when {
                trimmed.startsWith("FN:", ignoreCase = true) -> {
                    name = trimmed.substring(3).trim()
                }
                trimmed.startsWith("N:", ignoreCase = true) && name.isEmpty() -> {
                    val parts = trimmed.substring(2).split(";")
                    name = parts.filter { it.isNotBlank() }.reversed().joinToString(" ").trim()
                }
                trimmed.startsWith("TEL", ignoreCase = true) -> {
                    val num = trimmed.substringAfter(":").trim()
                    if (primaryPhone.isEmpty()) {
                        primaryPhone = num
                    } else if (secondaryPhone.isEmpty()) {
                        secondaryPhone = num
                    }
                }
                trimmed.startsWith("ADR", ignoreCase = true) -> {
                    val adrPart = trimmed.substringAfter(":").replace(";", ", ").trim()
                    address = adrPart.trim(',', ' ')
                }
                trimmed.startsWith("X-SOCIALPROFILE", ignoreCase = true) -> {
                    val social = trimmed.substringAfter(":")
                    if (social.contains("instagram", ignoreCase = true)) {
                        instagram = social.substringAfterLast("/").trim()
                    }
                }
                trimmed.startsWith("NOTE:", ignoreCase = true) -> {
                    observations = trimmed.substring(5).trim()
                }
                trimmed.startsWith("PHOTO", ignoreCase = true) -> {
                    val photoData = trimmed.substringAfter(":").trim()
                    photoSb.setLength(0)
                    photoSb.append(photoData)
                    parsingPhoto = true
                }
            }
        }

        if (parsingPhoto && photoSb.isNotEmpty()) {
            imageBase64 = photoSb.toString()
        }

        return ContactEntity(
            id = 0,
            name = name,
            primaryPhone = primaryPhone,
            secondaryPhone = secondaryPhone,
            address = address,
            instagram = instagram,
            observations = observations,
            imageBase64 = imageBase64
        )
    }

    /**
     * Generates a standard, 100% compliant QR Code Bitmap using ZXing MultiFormatWriter
     * Compatible with all Android camera scanners, Google Lens, Samsung Camera, iOS, and ZapDeck scanner.
     */
    fun generateQrBitmap(content: String, size: Int = 600): Bitmap {
        return try {
            val hints = HashMap<com.google.zxing.EncodeHintType, Any>()
            hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[com.google.zxing.EncodeHintType.MARGIN] = 2
            hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M

            val bitMatrix = com.google.zxing.MultiFormatWriter().encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val fgColor = Color.BLACK
            val bgColor = Color.WHITE

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback bitmap
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Decodes a QR code from a Bitmap (e.g. from camera photo, gallery or preview)
     * Handles different rotations (0°, 90°, 180°, 270°) and multiple binarizers (Hybrid and GlobalHistogram).
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val formats = listOf(com.google.zxing.BarcodeFormat.QR_CODE)
        val hints = HashMap<com.google.zxing.DecodeHintType, Any>()
        hints[com.google.zxing.DecodeHintType.POSSIBLE_FORMATS] = formats
        hints[com.google.zxing.DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
        hints[com.google.zxing.DecodeHintType.CHARACTER_SET] = "UTF-8"

        // Try direct orientations
        for (rotation in listOf(0, 90, 180, 270)) {
            val rotatedBitmap = if (rotation == 0) {
                bitmap
            } else {
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val pixels = IntArray(width * height)
            rotatedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            
            // 1. Try HybridBinarizer
            try {
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                val result = com.google.zxing.MultiFormatReader().decode(binaryBitmap, hints)
                if (!result.text.isNullOrBlank()) {
                    return result.text
                }
            } catch (ignored: Exception) {}

            // 2. Try GlobalHistogramBinarizer
            try {
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                val result = com.google.zxing.MultiFormatReader().decode(binaryBitmap, hints)
                if (!result.text.isNullOrBlank()) {
                    return result.text
                }
            } catch (ignored: Exception) {}
        }
        return null
    }
}
