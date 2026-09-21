package br.gov.sp.sme.salaleitura.core.logic

import org.junit.Assert.*
import org.junit.Test

class IsbnEquivalentTest {
    @Test fun isbn13With978PrefixPreservesConvertibleIsbn10ForCatalogFallback() {
        val result = Isbn.normalize("9780306406157") as IsbnResult.Valid
        assertEquals("9780306406157", result.isbn13)
        assertEquals("0306406152", result.isbn10)
    }

    @Test fun userReportedBrazilianIsbnsAreValidAndAlsoConvertible() {
        assertEquals("6557123408", (Isbn.normalize("9786557123409") as IsbnResult.Valid).isbn10)
        assertEquals("8533421761", (Isbn.normalize("9788533421769") as IsbnResult.Valid).isbn10)
    }
}
