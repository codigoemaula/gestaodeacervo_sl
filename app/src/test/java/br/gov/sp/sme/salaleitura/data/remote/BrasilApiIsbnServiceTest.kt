package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BrasilApiIsbnServiceTest {
    private val isbn = "9780306406157"

    @Test fun parsesProviderBookOnlyWhenReturnedIsbnIdentifiesRequestedEdition() {
        val json = """{
            "isbn":"9780306406157", "provider":"mercado-editorial", "title":"Livro da escola",
            "subtitle":"Subtítulo", "authors":["Primeira Autora","Segundo Autor"],
            "publisher":"Editora brasileira", "year":2024, "page_count":208,
            "subjects":["Educação"], "cover_url":"https://example.org/cover.jpg"
        }"""
        val result = requireNotNull(BrasilApiIsbnService().parseResponse(json, isbn))
        assertEquals("Livro da escola", result.title)
        assertEquals(listOf("Primeira Autora", "Segundo Autor"), result.authors)
        assertEquals("Editora brasileira", result.publisher)
        assertEquals(2024, result.publicationYear)
        assertEquals(208, result.pageCount)
        assertEquals("BRASIL_API_MERCADO_EDITORIAL", result.source)
        assertEquals("https://example.org/cover.jpg", result.coverUrl)
    }

    @Test fun rejectsDifferentEditionEvenWhenTheTitleExists() {
        val result = BrasilApiIsbnService().parseResponse("""{"isbn":"9781861972712","title":"Livro da escola"}""", isbn)
        assertNull(result)
    }

    @Test fun rejectsMissingOrMalformedProviderIsbnWithoutInventingAnEdition() {
        assertNull(BrasilApiIsbnService().parseResponse("""{"title":"Livro da escola"}""", isbn))
        assertNull(BrasilApiIsbnService().parseResponse("""{"isbn":"not-an-isbn","title":"Livro da escola"}""", isbn))
    }

    @Test fun providerFailureIsNotTreatedAsBookMissing() = runBlocking {
        val service = BrasilApiIsbnService(fetch = { throw IOException("HTTP 429") })
        val result = MetadataLookupEngine(listOf("BrasilAPI" to service)).lookup(isbn)
        assertTrue(result is MetadataLookupResult.Unavailable)
    }

    @Test fun queriesBrazilianEditionUsingThePublicApiWithoutCredentials() = runBlocking {
        val visited = mutableListOf<String>()
        val service = BrasilApiIsbnService(fetch = { url ->
            visited += url
            """{"isbn":"9780306406157","title":"Livro encontrado","provider":"cbl"}"""
        })
        assertEquals("Livro encontrado", service.lookup(isbn)?.title)
        assertEquals(listOf("https://brasilapi.com.br/api/isbn/v1/$isbn"), visited)
    }
}
