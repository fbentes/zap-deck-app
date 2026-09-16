package br.com.facbentes.zapdeck.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkShareHelper {

    /**
     * Extracts the installed APK file of ZapDeck and triggers the native Android
     * share sheet so the user can send it via WhatsApp, Quick Share, Bluetooth, Telegram, etc.
     */
    fun shareInstalledApk(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val sourceApk = File(appInfo.sourceDir)

            if (!sourceApk.exists()) {
                Toast.makeText(context, "Arquivo APK de instalação não encontrado no dispositivo.", Toast.LENGTH_LONG).show()
                return
            }

            // Create target cache directory for shared APKs
            val apkDir = File(context.cacheDir, "apks")
            if (!apkDir.exists()) {
                apkDir.mkdirs()
            }

            val targetApk = File(apkDir, "ZapDeck.apk")

            // Copy the APK to cache directory
            FileInputStream(sourceApk).use { input ->
                FileOutputStream(targetApk).use { output ->
                    input.copyTo(output)
                }
            }

            // Create Content URI using FileProvider
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "Aplicativo ZapDeck - Gestão e Troca de Cartões")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Instale o ZapDeck para escanear cartões de visita, salvar na agenda e trocar contatos instantaneamente por QR Code ou NFC!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartilhar ZapDeck (Enviar APK)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao preparar APK para envio: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
