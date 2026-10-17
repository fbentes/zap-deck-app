package br.com.facbentes.zapdeck

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.facbentes.zapdeck.utils.OfflineCardScanner
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

    @Test
    fun testPhoneClassificationWithLabelAboveNumber() {
        val lines = listOf(
            "DROGARIAS MAX",
            "Sempre ao seu lado",
            "Telefone",
            "(22) 2771-3643",
            "WhatsApp",
            "(22) 99809-8903",
            "Entrega em domicílio"
        )

        val result = OfflineCardScanner.extractClassifiedPhonesForTest(lines)
        assertEquals("(22) 99809-8903", result.primaryWhatsApp)
        assertEquals("(22) 2771-3643", result.landlineCommon)
    }

    @Test
    fun testIsFieldLabelIdentification() {
        assertTrue(OfflineCardScanner.isFieldLabel("WhatsApp"))
        assertTrue(OfflineCardScanner.isFieldLabel("whats:"))
        assertTrue(OfflineCardScanner.isFieldLabel("Telefone:"))
        assertTrue(OfflineCardScanner.isFieldLabel("Tel"))
        assertTrue(OfflineCardScanner.isFieldLabel("Fixo"))
        assertTrue(OfflineCardScanner.isFieldLabel("Instagram"))
        assertTrue(OfflineCardScanner.isFieldLabel("Instagram:"))
        assertTrue(OfflineCardScanner.isFieldLabel("Endereço:"))
        assertTrue(OfflineCardScanner.isFieldLabel("E-mail"))

        // Conteúdo legítimo de serviços ou anotações manuscritas não deve ser marcado como label
        assertFalse(OfflineCardScanner.isFieldLabel("Entrega em domicílio"))
        assertFalse(OfflineCardScanner.isFieldLabel("Dra. Maria - retorno dia 18"))
        assertFalse(OfflineCardScanner.isFieldLabel("Atendimento 24 horas"))
    }

    @Test
    fun testDynastyFootballWhatsAppBracketAndOcrInstagram() {
        val lines = listOf(
            "DYNASTY FOOTBALL",
            "WWW.DYNASTYFUT.COM.BR",
            "WHATSAPP",
            "[11] 97955-4224",
            "INSTAGRAM",
            "eDYNASTY_FUT",
            "RUA 24 DE MAIO - N62 / LOJA 172 / CENTRO - SÃO PAULO"
        )
        val fullText = lines.joinToString("\n")

        // 1. WhatsApp com DDD em colchetes [11] 97955-4224
        val phoneResult = OfflineCardScanner.extractClassifiedPhonesForTest(lines)
        assertEquals("(11) 97955-4224", phoneResult.primaryWhatsApp)

        // 2. Instagram onde OCR leu '@' como 'e' (eDYNASTY_FUT)
        val instagram = OfflineCardScanner.extractInstagramForTest(lines, fullText)
        assertEquals("@DYNASTY_FUT", instagram)
    }

    @Test
    fun testCleanInstagramHandleVariations() {
        assertEquals("@DYNASTY_FUT", OfflineCardScanner.cleanInstagramHandle("eDYNASTY_FUT", "DYNASTY FOOTBALL"))
        assertEquals("@DYNASTY_FUT", OfflineCardScanner.cleanInstagramHandle("@eDYNASTY_FUT", "DYNASTY FOOTBALL"))
        assertEquals("@DYNASTY_FUT", OfflineCardScanner.cleanInstagramHandle("@DYNASTY_FUT", "DYNASTY FOOTBALL"))
        assertEquals("@DYNASTY_FUT", OfflineCardScanner.cleanInstagramHandle("©DYNASTY_FUT", "DYNASTY FOOTBALL"))
        assertEquals("@clinicaavivar", OfflineCardScanner.cleanInstagramHandle("@clinicaavivar"))
    }

    @Test
    fun testPhoneWithoutDddInferredFromCity() {
        val lines = listOf(
            "DYNASTY FOOTBALL",
            "WHATSAPP",
            "97955-4224",
            "CENTRO - SÃO PAULO"
        )
        val phoneResult = OfflineCardScanner.extractClassifiedPhonesForTest(lines)
        assertEquals("(11) 97955-4224", phoneResult.primaryWhatsApp)
    }

    // Linhas reais lidas pelo ML Kit no cartao "América Micro" (icone do Instagram lido como '@ ').
    private val americaMicroLines = listOf(
        "AMÉRICA MICR0",
        "RIO DAS OSTRAS",
        "CELULARES- NOTEBOOKS",
        "COMPUTADORES- TABLETS",
        "(22) 99732-0176",
        "América Rio das Ostras",
        "@ america.micro",
        "Estrada do Contorno 110 - loja 07"
    )

    @Test
    fun testInstagramComIconeLidoComoArrobaEEspaco() {
        val result = OfflineCardScanner.extractInstagramForTest(
            americaMicroLines, americaMicroLines.joinToString(" ")
        )
        assertEquals("@america.micro", result)
    }

    @Test
    fun testInstagramNaoConfundeEmailComPerfil() {
        val lines = listOf("contato@empresa.com.br")
        assertEquals("", OfflineCardScanner.extractInstagramForTest(lines, lines.joinToString(" ")))
    }

    @Test
    fun testCorrigeZeroNoLugarDeLetraO() {
        assertEquals("AMÉRICA MICRO", OfflineCardScanner.fixZeroLetterConfusion("AMÉRICA MICR0"))
        assertEquals("(22) 99732-0176", OfflineCardScanner.fixZeroLetterConfusion("(22) 99732-0176"))
        assertEquals("Estrada do Contorno 110 - loja 07", OfflineCardScanner.fixZeroLetterConfusion("Estrada do Contorno 110 - loja 07"))
    }

    @Test
    fun testFacebookPorLinkERotulo() {
        assertEquals("minhaloja", OfflineCardScanner.extractFacebookForTest(listOf("facebook.com/minhaloja"), "facebook.com/minhaloja", ""))
        assertEquals("Minha Loja", OfflineCardScanner.extractFacebookForTest(listOf("Facebook: Minha Loja"), "Facebook: Minha Loja", ""))
    }

    @Test
    fun testFacebookPelaLinhaAcimaDoInstagram() {
        val result = OfflineCardScanner.extractFacebookForTest(
            americaMicroLines, americaMicroLines.joinToString(" "), "@america.micro"
        )
        assertEquals("América Rio das Ostras", result)
    }

    @Test
    fun testObservacoesDoCartaoAmericaMicro() {
        val result = OfflineCardScanner.extractObservationsForTest(
            lines = americaMicroLines,
            name = "América Micro Rio das Ostras",
            phones = listOf("(22) 99732-0176"),
            instagram = "@america.micro",
            address = "Estrada do Contorno 110 - loja 07",
            facebook = "América Rio das Ostras"
        )
        assertTrue(result, result.contains("CELULARES", ignoreCase = true))
        assertTrue(result, result.contains("TABLETS", ignoreCase = true))
    }
}
