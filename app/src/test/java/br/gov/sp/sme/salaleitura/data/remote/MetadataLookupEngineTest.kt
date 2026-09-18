package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MetadataLookupEngineTest {
    private val isbn = "9780306406157"
    private val book = BookMetadata(isbn13 = isbn, title = "Livro encontrado", source = "FALLBACK")
    private fun source(response: BookMetadata? = null, error: Exception? = null) = object : BibliographicService {
        override suspend fun lookup(isbn13: String): BookMetadata? {
            error?.let { throw it }
            return response
        }
    }

    @Test fun reportsUnavailableWhenSourcesFailInsteadOfClaimingIsbnDoesNotExist() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(error = IOException("connection timeout")), "Biblioteca B" to source()))
        val result = engine.lookup(isbn)
        assertTrue(result is MetadataLookupResult.Unavailable)
        assertTrue((result as MetadataLookupResult.Unavailable).failedSources.contains("Biblioteca A"))
    }

    @Test fun reportsMissingOnlyWhenEveryProviderRespondedWithoutTheBook() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(), "Biblioteca B" to source()))
        assertTrue(engine.lookup(isbn) is MetadataLookupResult.Missing)
    }

    @Test fun triesNextSourceWhenFirstDoesNotKnowTheIsbn() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(), "Biblioteca B" to source(book)))
        assertEquals(book, (engine.lookup(isbn) as MetadataLookupResult.Found).book)
    }

    @Test fun triesNextSourceWhenFirstFails() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(error = IOException("offline")), "Biblioteca B" to source(book)))
        assertEquals(book, (engine.lookup(isbn) as MetadataLookupResult.Found).book)
    }

    @Test fun fillsMissingMetadataFromAnotherVerifiedSourceForSameEdition() = runBlocking {
        val titleOnly = BookMetadata(isbn13 = isbn, title = "Um livro", source = "PRIMEIRA")
        val fuller = BookMetadata(isbn13 = isbn, title = "Um livro", authors = listOf("Autora"), publisher = "Editora", publicationYear = 2024, source = "SEGUNDA")
        val result = MetadataLookupEngine(listOf("A" to source(titleOnly), "B" to source(fuller))).lookup(isbn)
        val found = (result as MetadataLookupResult.Found).book
        assertEquals("Um livro", found.title)
        assertEquals(listOf("Autora"), found.authors)
        assertEquals("Editora", found.publisher)
        assertEquals(2024, found.publicationYear)
        assertTrue(found.source.contains("PRIMEIRA"))
        assertTrue(found.source.contains("SEGUNDA"))
    }

    @Test fun rejectsProviderResultForWrongIsbnAndKeepsSearching() = runBlocking {
        val wrong = BookMetadata(isbn13 = "9781861972712", title = "Livro errado", source = "ERRADA")
        val result = MetadataLookupEngine(listOf("A" to source(wrong), "B" to source(book))).lookup(isbn)
        assertEquals(book, (result as MetadataLookupResult.Found).book)
    }

    @Test fun doesNotCombineMetadataFromConflictingTitles() = runBlocking {
        val editionA = BookMetadata(isbn13 = isbn, title = "Livro A", source = "PRIMEIRA")
        val editionB = BookMetadata(isbn13 = isbn, title = "Livro B", authors = listOf("Autor errado"), source = "SEGUNDA")
        val result = MetadataLookupEngine(listOf("A" to source(editionA), "B" to source(editionB))).lookup(isbn)
        assertEquals(editionA, (result as MetadataLookupResult.Found).book)
    }
}
