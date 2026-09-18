package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MetadataLookupEngineTest {
    private val book = BookMetadata(isbn13 = "9780306406157", title = "Livro encontrado", source = "FALLBACK")
    private fun source(response: BookMetadata? = null, error: Exception? = null) = object : BibliographicService {
        override suspend fun lookup(isbn13: String): BookMetadata? {
            error?.let { throw it }
            return response
        }
    }

    @Test fun reportsUnavailableWhenSourcesFailInsteadOfClaimingIsbnDoesNotExist() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to source(error = IOException("connection timeout")),
            "Biblioteca B" to source()
        ))
        val result = engine.lookup(book.isbn13)
        assertTrue(result is MetadataLookupResult.Unavailable)
        assertTrue((result as MetadataLookupResult.Unavailable).failedSources.contains("Biblioteca A"))
    }

    @Test fun reportsMissingOnlyWhenEveryProviderRespondedWithoutTheBook() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(), "Biblioteca B" to source()))
        assertTrue(engine.lookup(book.isbn13) is MetadataLookupResult.Missing)
    }

    @Test fun triesNextSourceWhenFirstDoesNotKnowTheIsbn() = runBlocking {
        val engine = MetadataLookupEngine(listOf("Biblioteca A" to source(), "Biblioteca B" to source(book)))
        assertEquals(book, (engine.lookup(book.isbn13) as MetadataLookupResult.Found).book)
    }

    @Test fun triesNextSourceWhenFirstFails() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to source(error = IOException("offline")), "Biblioteca B" to source(book)
        ))
        assertEquals(book, (engine.lookup(book.isbn13) as MetadataLookupResult.Found).book)
    }
}
