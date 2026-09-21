package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GoogleBooksServiceTest {
    private val isbn13 = "9780306406157"
    private val body = """{
      "items": [
        {"volumeInfo":{"title":"Wrong title","industryIdentifiers":[{"type":"ISBN_13","identifier":"9781861972712"}]}},
        {"volumeInfo":{"title":"Correct title","authors":["A. Researcher"],"publisher":"Publisher","industryIdentifiers":[{"type":"ISBN_10","identifier":"0306406152"}]}}
      ]
    }"""

    @Test fun exactIsbnIsRequiredAndCorrectResultCanAppearAfterFirstHit() {
        val book = requireNotNull(GoogleBooksService().parseResponse(body, isbn13))
        assertEquals("Correct title", book.title)
        assertEquals(listOf("A. Researcher"), book.authors)
        assertEquals("Publisher", book.publisher)
    }

    @Test fun unrelatedSearchResultIsNeverAcceptedAsScannedEdition() {
        val wrongOnly = """{"items":[{"volumeInfo":{"title":"Wrong title","industryIdentifiers":[{"type":"ISBN_13","identifier":"9781861972712"}]}}]}"""
        assertNull(GoogleBooksService().parseResponse(wrongOnly, isbn13))
    }

    @Test fun providerRetriesWithIsbn10WhenIsbn13SearchHasNoResult() = runBlocking {
        val urls = mutableListOf<String>()
        val service = GoogleBooksService(fetch = { url ->
            urls += url
            if (url.contains("0306406152")) body else """{"totalItems":0}"""
        })
        assertEquals("Correct title", service.lookup(isbn13)?.title)
        assertEquals(2, urls.size)
        assertTrue(urls.first().contains(isbn13))
        assertTrue(urls.last().contains("0306406152"))
    }
}
