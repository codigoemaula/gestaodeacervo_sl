package br.gov.sp.sme.salaleitura.core.logic

import org.junit.Assert.*
import org.junit.Test

class CopyAliasPolicyTest {
    @Test fun normalizesBarcodeWithoutChangingPermanentCopyCode() {
        assertEquals("2026-00874", CopyAliasPolicy.normalize(" 2026-00874 "))
        assertEquals("ABC-12", CopyAliasPolicy.normalize("abc-12"))
    }
    @Test fun rejectsBlankOrOversizedPatrimony() {
        assertThrows(IllegalArgumentException::class.java) { CopyAliasPolicy.normalize(" ") }
        assertThrows(IllegalArgumentException::class.java) { CopyAliasPolicy.normalize("x".repeat(65)) }
    }
}
