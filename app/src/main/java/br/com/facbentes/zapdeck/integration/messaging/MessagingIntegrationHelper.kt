package br.com.facbentes.zapdeck.integration.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import br.com.facbentes.zapdeck.utils.ContactSystemSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object MessagingIntegrationHelper {

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isWhatsAppRegisteredOnDevice(context: Context, phone: String, isBusiness: Boolean): Boolean {
        if (!ContactSystemSync.hasContactsPermissions(context)) return false
        val cleanPhone = phone.replace("\\D".toRegex(), "")
        if (cleanPhone.isEmpty()) return false
        
        val mimeType = if (isBusiness) {
            "vnd.android.cursor.item/vnd.com.whatsapp.w4b.profile"
        } else {
            "vnd.android.cursor.item/vnd.com.whatsapp.profile"
        }
        
        val resolver = context.contentResolver
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(mimeType)
        
        try {
            val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numCol != -1) {
                    while (it.moveToNext()) {
                        val number = it.getString(numCol) ?: continue
                        val cleanNumber = number.replace("\\D".toRegex(), "")
                        if (cleanNumber.isNotEmpty() && (cleanNumber.endsWith(cleanPhone) || cleanPhone.endsWith(cleanNumber))) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppCheck", "Error checking whatsapp registration: ${e.message}")
        }
        return false
    }

    suspend fun checkIfWhatsAppRegistered(context: Context, phone: String, isBusiness: Boolean): Boolean {
        val cleanPhone = phone.replace("\\D".toRegex(), "")
        if (cleanPhone.isEmpty()) return false
        
        return withContext(Dispatchers.IO) {
            val synced = isWhatsAppRegisteredOnDevice(context, cleanPhone, isBusiness)
            if (synced) {
                true
            } else {
                // Intelligent mobile format validation for Brazil and general international mobile numbers
                val length = cleanPhone.length
                if (length >= 10) {
                    if (length == 11) {
                        cleanPhone[2] == '9'
                    } else if (length == 13) {
                        cleanPhone[4] == '9'
                    } else {
                        true
                    }
                } else {
                    false
                }
            }
        }
    }

    suspend fun checkIfInstagramUserExists(username: String): Boolean {
        val cleanUsername = username.replace("@", "").trim()
        if (cleanUsername.isEmpty()) return false
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.instagram.com/$cleanUsername/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                
                val responseCode = connection.responseCode
                responseCode != HttpURLConnection.HTTP_NOT_FOUND
            } catch (e: Exception) {
                true // fallback to true on network error so we don't block users if there's internet trouble or redirection issues
            }
        }
    }

    fun openInstagramProfile(context: Context, username: String) {
        val cleanUsername = username.replace("@", "").trim()
        if (cleanUsername.isEmpty()) return
        val uri = Uri.parse("http://instagram.com/_u/$cleanUsername")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$cleanUsername")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Instagram não pôde ser aberto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun formatNumberToWhatsApp(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length in 10..11) {
            "55$digits" // Automatically prepending Brazil standard country code
        } else {
            digits
        }
    }

    fun openWhatsAppChat(context: Context, phone: String, message: String, useBusiness: Boolean) {
        val formattedPhone = formatNumberToWhatsApp(phone)
        if (formattedPhone.isEmpty()) {
            Toast.makeText(context, "Telefone inválido ou vazio para envio.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage(if (useBusiness) "com.whatsapp.w4b" else "com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp/WhatsApp Business não localizado no aparelho.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
