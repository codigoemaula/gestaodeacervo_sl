package br.gov.sp.sme.salaleitura.data.remote

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import org.junit.Assert.*
import org.junit.Test

/** User-reported ISBNs verified against the publisher and an institutional repository. */
class VerifiedBibliographyTest {
    @Test fun publisherRecordAutofillsAliceGendronBook() {
        val isbn = "9786557123409"
        assertTrue(Isbn.normalize(isbn) is IsbnResult.Valid)
        val book = requireNotNull(VerifiedBibliography.find(isbn))
        assertEquals(isbn, book.isbn13)
        assertEquals("O pequeno livro do TDAH", book.title)
        assertEquals("Um manual para enfim entender como sua mente funciona", book.subtitle)
        assertEquals(listOf("Alice Gendron"), book.authors)
        assertEquals("BestSeller", book.publisher)
        assertEquals(2024, book.publicationYear)
        assertEquals(208, book.pageCount)
        assertEquals("CATALOGO_VERIFICADO", book.source)
    }

    @Test fun institutionalRecordAutofillsBrazilianFoodGuide() {
        val isbn = "9788533421769"
        assertTrue(Isbn.normalize(isbn) is IsbnResult.Valid)
        val book = requireNotNull(VerifiedBibliography.find(isbn))
        assertEquals(isbn, book.isbn13)
        assertEquals("Guia alimentar para a população brasileira", book.title)
        assertEquals(listOf("Ministério da Saúde"), book.authors)
        assertEquals("Ministério da Saúde", book.publisher)
        assertEquals(2014, book.publicationYear)
        assertEquals(152, book.pageCount)
        assertEquals("CATALOGO_VERIFICADO", book.source)
    }

    @Test fun unrelatedIsbnDoesNotGetInventedMetadata() {
        assertNull(VerifiedBibliography.find("9780306406157"))
    }
}
