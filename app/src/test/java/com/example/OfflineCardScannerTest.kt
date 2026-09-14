package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.utils.OfflineCardScanner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineCardScannerTest {

    @Test
    fun testPhoneClassificationAvivar() {
        val lines = listOf(
            "AVIVAR",
            "Clínica de Saúde",
            "Telefone: (22) 3087-6777",
            "WhatsApp: (22) 99781-0486",
            "Dra. Maria - retorno dia 18"
        )

        val result = OfflineCardScanner.extractClassifiedPhonesForTest(lines)
        assertEquals("(22) 99781-0486", result.primaryWhatsApp)
        assertEquals("(22) 3087-6777", result.landlineCommon)
    }

    @Test
    fun testPhoneClassificationDrogariasMax() {
        val lines = listOf(
            "DROGARIAS MAX",
            "Sempre ao seu lado",
            "Telefone: (22) 2771-3643",
            "WhatsApp: (22) 99809-8903",
            "WhatsApp 2: (22) 99960-0653",
            "Entrega em domicílio"
        )

        val result = OfflineCardScanner.extractClassifiedPhonesForTest(lines)
        assertEquals("(22) 99809-8903", result.primaryWhatsApp)
        assertEquals("(22) 99960-0653", result.secondaryWhatsApp)
        assertEquals("(22) 2771-3643", result.landlineCommon)
    }
}
