package br.gov.sp.sme.salaleitura.core

import br.gov.sp.sme.salaleitura.feature.legal.LegalInfoContent
import org.junit.Assert.*
import org.junit.Test

class LegalInfoContentTest {
    @Test fun `policy explains local storage and third-party ISBN queries and states LGPD still applies`() {
        val text = LegalInfoContent.privacySections.joinToString(" ") { it.second }
        assertTrue(text.contains("LGPD"))
        assertTrue(text.contains("ISBN"))
        assertTrue(text.contains("dispositivo"))
        assertTrue(text.contains("endereço IP"))
        assertTrue(text.contains("Exportações"))
        assertTrue(text.contains("foto"))
        assertTrue(text.contains("uso offline não dispensa sua observância", ignoreCase = true))
        assertFalse(text.contains("LGPD não se aplica", ignoreCase = true))
        assertTrue(text.contains("backup inclui banco de dados e fotos opcionais", ignoreCase = true))
    }

    @Test fun `about credits independent project without institutional endorsement`() {
        assertEquals("Código em Aula", LegalInfoContent.creator)
        assertEquals("https://www.instagram.com/codigoemaula/", LegalInfoContent.instagramUrl)
        assertEquals("@codigoemaula", LegalInfoContent.instagramHandle)
        assertFalse(LegalInfoContent.about.contains("SME", ignoreCase = true))
        assertFalse(LegalInfoContent.about.contains("RME", ignoreCase = true))
    }
}
