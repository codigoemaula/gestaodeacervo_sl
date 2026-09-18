package br.gov.sp.sme.salaleitura.data.remote

import org.junit.Assert.*
import org.junit.Test

class CrossrefBookServiceTest {
    private val isbn = "9780306406157"

    @Test fun acceptsOnlyBookLevelRecordWithTheExactIsbn() {
        val body = """{"message":{"items":[
          {"type":"book-chapter","title":["Wrong chapter"],"ISBN":["9780306406157"]},
          {"type":"book","title":["Wrong edition"],"ISBN":["9781861972712"]},
          {"type":"book","title":["Correct book"],"ISBN":["9780306406157"],"publisher":"Academic Press","author":[{"given":"Ana","family":"Silva"}],"issued":{"date-parts":[[2024,1,1]]}}
        ]}}"""
        val book = requireNotNull(CrossrefBookService().parseResponse(body, isbn))
        assertEquals("Correct book", book.title)
        assertEquals("Academic Press", book.publisher)
        assertEquals(listOf("Ana Silva"), book.authors)
        assertEquals(2024, book.publicationYear)
    }

    @Test fun rejectsChapterEvenIfIsbnMatches() {
        val body = """{"message":{"items":[{"type":"book-chapter","title":["Not an edition"],"ISBN":["9780306406157"]}]}}"""
        assertNull(CrossrefBookService().parseResponse(body, isbn))
    }
}
