package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MetadataLookupEngineTest {
    private val book = BookMetadata(isbn13 = "9780306406157", title = "Livro encontrado", source = "FALLBACK")

    @Test fun reportsUnavailableWhenSourcesFailInsteadOfClaimingIsbnDoesNotExist() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to BibliographicService { throw IOException("connection timeout") },
            "Biblioteca B" to BibliographicService { null }
        ))
        val result = engine.lookup(book.isbn13)
        assertTrue(result is MetadataLookupResult.Unavailable)
        assertTrue((result as MetadataLookupResult.Unavailable).failedSources.contains("Biblioteca A"))
    }

    @Test fun reportsMissingOnlyWhenEveryProviderRespondedWithoutTheBook() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to BibliographicService { null },
            "Biblioteca B" to BibliographicService { null }
        ))
        assertTrue(engine.lookup(book.isbn13) is MetadataLookupResult.Missing)
    }

    @Test fun triesNextSourceWhenFirstDoesNotKnowTheIsbn() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to BibliographicService { null },
            "Biblioteca B" to BibliographicService { book }
        ))
        assertEquals(book, (engine.lookup(book.isbn13) as MetadataLookupResult.Found).book)
    }

    @Test fun triesNextSourceWhenFirstFails() = runBlocking {
        val engine = MetadataLookupEngine(listOf(
            "Biblioteca A" to BibliographicService { throw IOException("offline") },
            "Biblioteca B" to BibliographicService { book }
        ))
        assertEquals(book, (engine.lookup(book.isbn13) as MetadataLookupResult.Found).book)
    }
}
