package br.gov.sp.sme.salaleitura.feature.catalog

import org.junit.Assert.*
import org.junit.Test

class BookFormIsbnEditTest {
    @Test fun newIsbnInvalidatesPreviousEditionAndAllPreviouslyFetchedMetadata() {
        val old = BookFormState(
            isbn = "9780306406157", title = "Livro anterior", authors = "Autora anterior",
            subtitle = "Subtítulo anterior", publisher = "Editora anterior",
            publicationYear = "2024", pageCount = "208", subjects = "Educação",
            existingEditionId = 12, metadataSource = "GOOGLE_BOOKS", saved = true,
            location = "Estante A", quantity = "3", error = "Erro anterior"
        )
        val updated = old.withIsbnInput("9781861972712")
        assertEquals("9781861972712", updated.isbn)
        assertEquals("", updated.title)
        assertEquals("", updated.authors)
        assertEquals("", updated.subtitle)
        assertEquals("", updated.publisher)
        assertEquals("", updated.publicationYear)
        assertEquals("", updated.pageCount)
        assertEquals("", updated.subjects)
        assertNull(updated.metadataSource)
        assertNull(updated.existingEditionId)
        assertNull(updated.error)
        assertFalse(updated.saved)
        assertEquals("Estante A", updated.location)
        assertEquals("3", updated.quantity)
    }

    @Test fun unrelatedFormFieldsAreNotErasedWhenTheIsbnDidNotChange() {
        val previous = BookFormState(isbn = "9780306406157", title = "Título editado pelo professor", location = "Sala", quantity = "2")
        assertEquals(previous, previous.withIsbnInput(previous.isbn))
    }
}
