package br.gov.sp.sme.salaleitura.data.remote

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.net.URLEncoder

/** Exact-edition lookup: never use the first search hit without validating its identifiers. */
class GoogleBooksService(
    private val fetch: suspend (String) -> String? = { url -> httpGet(url) }
) : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val valid = Isbn.normalize(isbn13) as? IsbnResult.Valid ?: return null
        val variants = listOfNotNull(valid.isbn13, valid.isbn10).distinct()
        var lastFailure: Exception? = null
        for (variant in variants) {
            val url = "https://www.googleapis.com/books/v1/volumes?q=${URLEncoder.encode("isbn:$variant", "UTF-8")}&maxResults=20"
            try {
                val body = fetch(url) ?: continue
                parseResponse(body, valid.isbn13)?.let { return it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
            }
        }
        lastFailure?.let { throw it }
        return null
    }

    internal fun parseResponse(body: String, isbn13: String): BookMetadata? {
        val root = JSONObject(body)
        val items = root.optJSONArray("items") ?: return null
        for (index in 0 until items.length()) {
            val info = items.optJSONObject(index)?.optJSONObject("volumeInfo") ?: continue
            val identifiers = info.optJSONArray("industryIdentifiers") ?: continue
            val matches = (0 until identifiers.length()).any { identifierIndex ->
                val entry = identifiers.optJSONObject(identifierIndex) ?: return@any false
                if (entry.optString("type") !in setOf("ISBN_10", "ISBN_13")) return@any false
                val normalized = Isbn.normalize(entry.optString("identifier")) as? IsbnResult.Valid
                normalized?.isbn13 == isbn13
            }
            if (!matches) continue
            fun text(key: String): String? = info.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
            val title = text("title") ?: continue
            val authors = info.optJSONArray("authors")?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf { name -> name.isNotBlank() && name != "null" } }
            }.orEmpty()
            val categories = info.optJSONArray("categories")?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf { name -> name.isNotBlank() && name != "null" } }
            }.orEmpty()
            val year = Regex("\\d{4}").find(info.optString("publishedDate"))?.value?.toIntOrNull()
            val cover = info.optJSONObject("imageLinks")?.optString("thumbnail")
                ?.replace("http://", "https://")?.takeIf { it.isNotBlank() && it != "null" }
            return BookMetadata(
                isbn13 = isbn13, title = title, subtitle = text("subtitle"), authors = authors,
                publisher = text("publisher"), publicationYear = year, language = text("language"),
                pageCount = info.optInt("pageCount", 0).takeIf { it > 0 },
                subjects = categories, coverUrl = cover, source = "GOOGLE_BOOKS"
            )
        }
        return null
    }
}
