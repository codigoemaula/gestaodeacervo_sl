package br.gov.sp.sme.salaleitura.data.remote

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import org.json.JSONObject
import java.net.URLEncoder

/** Crossref contributes book-level publisher deposits; chapters are intentionally excluded. */
class CrossrefBookService : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val query = URLEncoder.encode(isbn13, "UTF-8")
        val url = "https://api.crossref.org/works?query.bibliographic=$query&rows=20&select=ISBN,title,subtitle,author,publisher,published-print,issued,language,type,subject"
        val body = httpGet(url) ?: return null
        return parseResponse(body, isbn13)
    }

    internal fun parseResponse(body: String, isbn13: String): BookMetadata? {
        val items = JSONObject(body).optJSONObject("message")?.optJSONArray("items") ?: return null
        for (i in 0 until items.length()) {
            val entry = items.optJSONObject(i) ?: continue
            if (entry.optString("type") !in setOf("book", "monograph", "edited-book", "reference-book")) continue
            val isbns = entry.optJSONArray("ISBN") ?: continue
            val exact = (0 until isbns.length()).any { j ->
                (Isbn.normalize(isbns.optString(j)) as? IsbnResult.Valid)?.isbn13 == isbn13
            }
            if (!exact) continue
            val title = entry.optJSONArray("title")?.optString(0)?.trim().orEmpty().takeIf { it.isNotBlank() } ?: continue
            val authors = entry.optJSONArray("author")?.let { a ->
                (0 until a.length()).mapNotNull { j ->
                    val person = a.optJSONObject(j) ?: return@mapNotNull null
                    listOf(person.optString("given"), person.optString("family"))
                        .filter { it.isNotBlank() && it != "null" }.joinToString(" ").takeIf(String::isNotBlank)
                        ?: person.optString("name").takeIf { it.isNotBlank() && it != "null" }
                }
            }.orEmpty()
            val year = listOf("published-print", "issued").firstNotNullOfOrNull { key ->
                entry.optJSONObject(key)?.optJSONArray("date-parts")?.optJSONArray(0)?.optInt(0, 0)?.takeIf { it in 1450..2200 }
            }
            return BookMetadata(
                isbn13 = isbn13, title = title,
                subtitle = entry.optJSONArray("subtitle")?.optString(0)?.takeIf { it.isNotBlank() && it != "null" },
                authors = authors, publisher = entry.optString("publisher").takeIf { it.isNotBlank() && it != "null" },
                publicationYear = year,
                language = entry.optString("language").takeIf { it.isNotBlank() && it != "null" },
                subjects = entry.optJSONArray("subject")?.let { a ->
                    (0 until minOf(a.length(), 12)).mapNotNull { j -> a.optString(j).takeIf { it.isNotBlank() && it != "null" } }
                }.orEmpty(),
                source = "CROSSREF"
            )
        }
        return null
    }
}
