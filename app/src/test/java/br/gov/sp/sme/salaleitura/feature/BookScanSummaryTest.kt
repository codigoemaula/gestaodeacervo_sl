package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.feature.scanner.BookScanSummary
import br.gov.sp.sme.salaleitura.data.remote.BookMetadata
import org.junit.Assert.*
import org.junit.Test

class BookScanSummaryTest {
    @Test fun successfulLookupDisplaysActualTitleAndAuthors() {
        val book = BookMetadata(isbn13 = "9780306406157", title = "Livro de exemplo", authors = listOf("Autora Exemplo"), source = "OPEN_LIBRARY")
        val result = BookScanSummary.from(book)
        assertEquals("Livro de exemplo", result.first)
        assertTrue(result.second.contains("Autora Exemplo"))
    }
    @Test fun missingMetadataDoesNotFabricateBookDetails() {
        val result = BookScanSummary.from(null)
        assertEquals("ISBN identificado", result.first)
        assertTrue(result.second.contains("não foram encontrados"))
    }
}
