package com.example.utils

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.ContactEntity
import java.nio.charset.StandardCharsets

object NfcReaderHelper {

    private const val TAG = "NfcReaderHelper"

    // ZapDeck AID APDU command to SELECT
    // Header: 00 A4 04 00 07 F0 01 02 03 04 05 06 00
    private val SELECT_ZAPDECK_AID_APDU = byteArrayOf(
        0x00.toByte(), // CLA
        0xA4.toByte(), // INS
        0x04.toByte(), // P1 (Select by name)
        0x00.toByte(), // P2
        0x07.toByte(), // Lc (AID length)
        0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), // AID
        0x00.toByte()  // Le
    )

    // GET DATA APDU (00 B0 00 00 00)
    private val GET_DATA_APDU = byteArrayOf(
        0x00.toByte(),
        0xB0.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * Enables Reader Mode in the Activity to automatically read another phone's HCE card emulation or NDEF tag
     */
    fun enableReaderMode(activity: Activity, onContactRead: (ContactEntity) -> Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        
        // Listen for standard NFC tags (ISO-DEP / Type A / Type B / Type F / Type V)
        val flags = NfcAdapter.FLAG_READER_NFC_A or 
                    NfcAdapter.FLAG_READER_NFC_B or 
                    NfcAdapter.FLAG_READER_NFC_F or 
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = android.os.Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        nfcAdapter.enableReaderMode(activity, { tag: Tag ->
            Log.d(TAG, "NFC Tag detected: ${tag.id?.joinToString("") { "%02X".format(it) }} with techs: ${tag.techList.joinToString()}")
            
            // Method 1: Try ISO-DEP (Host Card Emulation communication)
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                try {
                    isoDep.connect()
                    isoDep.timeout = 5000

                    Log.d(TAG, "Sending SELECT APDU to tag...")
                    val selectResponse = isoDep.transceive(SELECT_ZAPDECK_AID_APDU)
                    Log.d(TAG, "SELECT response: ${selectResponse?.joinToString("") { "%02X".format(it) }}")

                    if (isSuccessResponse(selectResponse)) {
                        Log.d(TAG, "ZapDeck AID successfully selected on remote device!")

                        val dataResponse = isoDep.transceive(GET_DATA_APDU)
                        if (dataResponse != null && dataResponse.size > 2) {
                            val payloadLen = dataResponse.size - 2
                            val jsonBytes = ByteArray(payloadLen)
                            System.arraycopy(dataResponse, 0, jsonBytes, 0, payloadLen)
                            val jsonStr = String(jsonBytes, StandardCharsets.UTF_8)
                            Log.d(TAG, "Received payload string: $jsonStr")

                            val contact = CardBeamTransferHelper.jsonToContact(jsonStr)
                                ?: CardBeamTransferHelper.vCardToContact(jsonStr)

                            if (contact != null) {
                                playSuccessFeedback(activity)
                                activity.runOnUiThread {
                                    onContactRead(contact)
                                }
                                return@enableReaderMode
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ISO-DEP transceive: ${e.message}")
                } finally {
                    try {
                        isoDep.close()
                    } catch (ignored: Exception) {}
                }
            }

            // Method 2: Try standard NDEF reading (if tag was formatted or written as NDEF)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage
                    if (ndefMessage != null) {
                        for (record in ndefMessage.records) {
                            val payload = String(record.payload, StandardCharsets.UTF_8)
                            val contact = CardBeamTransferHelper.jsonToContact(payload)
                                ?: CardBeamTransferHelper.vCardToContact(payload)
                            if (contact != null) {
                                playSuccessFeedback(activity)
                                activity.runOnUiThread {
                                    onContactRead(contact)
                                }
                                return@enableReaderMode
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in NDEF read: ${e.message}")
                } finally {
                    try {
                        ndef.close()
                    } catch (ignored: Exception) {}
                }
            }
        }, flags, options)
    }

    /**
     * Disable Reader Mode when leaving the screen or pausing
     */
    fun disableReaderMode(activity: Activity) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            nfcAdapter.disableReaderMode(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling reader mode: ${e.message}")
        }
    }
    
    private fun isSuccessResponse(response: ByteArray?): Boolean {
        if (response == null || response.size < 2) return false
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return sw1 == 0x90.toByte() && sw2 == 0x00.toByte()
    }

    private fun playSuccessFeedback(context: Context) {
        try {
            // Haptic feedback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }

            // Audio tone feedback
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e(TAG, "Feedback error: ${e.message}")
        }
    }
}
