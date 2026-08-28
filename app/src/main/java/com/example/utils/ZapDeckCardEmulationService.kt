package com.example.utils

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Host-based Card Emulation (HCE) service to transmit ZapDeck contacts
 * when another device with ZapDeck approaches via NFC.
 */
class ZapDeckCardEmulationService : HostApduService() {

    companion object {
        private const val TAG = "ZapDeckHceService"
        
        // Custom ZapDeck Application Identifier (AID)
        // Header: SELECT AID "F0010203040506"
        private val SELECT_APDU_HEADER = byteArrayOf(
            0x00.toByte(), // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04.toByte(), // P1 (Select by name)
            0x00.toByte(), // P2
            0x07.toByte(), // Lc (Length of AID)
            0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte() // AID
        )

        // Command to request active card data payload: 0x00 0xB0 0x00 0x00
        private val GET_DATA_HEADER = byteArrayOf(
            0x00.toByte(),
            0xB0.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )

        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())

        // Active contact payload in JSON currently set for broadcasting
        var activeTransferPayload: String? = null
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) return STATUS_FAILED

        Log.d(TAG, "processCommandApdu received ${commandApdu.size} bytes: ${commandApdu.joinToString("") { "%02X".format(it) }}")

        // 1. Check if it's the SELECT APDU command (CLA=00, INS=A4, P1=04, P2=00)
        if (isSelectApdu(commandApdu)) {
            Log.d(TAG, "ZapDeck AID selected via NFC!")
            return STATUS_SUCCESS
        }

        // 2. Check if it's GET DATA command (CLA=00, INS=B0) or READ BINARY
        if (isGetDataApdu(commandApdu)) {
            val payload = activeTransferPayload
            if (!payload.isNullOrBlank()) {
                val payloadBytes = payload.toByteArray(StandardCharsets.UTF_8)
                val response = ByteArray(payloadBytes.size + 2)
                System.arraycopy(payloadBytes, 0, response, 0, payloadBytes.size)
                response[response.size - 2] = 0x90.toByte()
                response[response.size - 1] = 0x00.toByte()
                Log.d(TAG, "Delivering ${payloadBytes.size} bytes of contact payload over NFC")
                return response
            } else {
                Log.w(TAG, "GET_DATA received but activeTransferPayload is empty!")
            }
        }

        return STATUS_SUCCESS
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE Service Deactivated: reason=$reason")
    }

    private fun isSelectApdu(apdu: ByteArray): Boolean {
        // Minimum ISO 7816-4 SELECT command: CLA=0x00, INS=0xA4
        if (apdu.size < 4) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte()) return false
        
        // Check if AID contains our AID byte sequence
        val zapdeckAid = byteArrayOf(0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte())
        
        // Search if zapdeckAid is present inside apdu
        for (i in 0..(apdu.size - zapdeckAid.size)) {
            var match = true
            for (j in zapdeckAid.indices) {
                if (apdu[i + j] != zapdeckAid[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }

    private fun isGetDataApdu(apdu: ByteArray): Boolean {
        if (apdu.size < 2) return false
        // Match CLA=00 and INS=B0 (READ BINARY / GET DATA) or INS=CA (GET DATA)
        return (apdu[0] == 0x00.toByte() && (apdu[1] == 0xB0.toByte() || apdu[1] == 0xCA.toByte()))
    }
}
