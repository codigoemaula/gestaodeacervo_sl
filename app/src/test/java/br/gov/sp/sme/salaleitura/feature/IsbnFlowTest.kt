package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.feature.catalog.BookScanRoute
import org.junit.Assert.*
import org.junit.Test

class IsbnFlowTest {
    @Test fun scannedIsbnHasDedicatedDeepLink() {
        assertEquals("book/new?isbn=9780306406157", BookScanRoute.forIsbn("9780306406157"))
    }

    @Test fun invalidCodeCannotTriggerBibliographicLookup() {
        assertThrows(IllegalArgumentException::class.java) { BookScanRoute.forIsbn("1234567890123") }
    }
}
