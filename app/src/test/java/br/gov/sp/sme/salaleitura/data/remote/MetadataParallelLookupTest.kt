package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataParallelLookupTest {
    @Test fun slowerProviderDoesNotPreventIndependentProviderFromStarting() = runBlocking {
        val secondStarted = CompletableDeferred<Unit>()
        val isbn = "9780306406157"
        val first = object : BibliographicService {
            override suspend fun lookup(isbn13: String): BookMetadata? {
                withTimeout(1500) { secondStarted.await() }
                return BookMetadata(isbn13, "Livro", source = "FIRST")
            }
        }
        val second = object : BibliographicService {
            override suspend fun lookup(isbn13: String): BookMetadata? {
                secondStarted.complete(Unit)
                return BookMetadata(isbn13, "Livro", authors = listOf("Autora"), source = "SECOND")
            }
        }
        val result = withTimeout(2000) { MetadataLookupEngine(listOf("A" to first, "B" to second)).lookup(isbn) }
        assertEquals(listOf("Autora"), (result as MetadataLookupResult.Found).book.authors)
    }
}
