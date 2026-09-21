package br.gov.sp.sme.salaleitura.data.remote

import org.json.JSONObject
import java.net.URLEncoder

class OpenLibraryService : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val key = "ISBN:$isbn13"
        val url = "https://openlibrary.org/api/books?bibkeys=${URLEncoder.encode(key, "UTF-8")}&jscmd=data&format=json"
        val body = httpGet(url) ?: return null
        val item = JSONObject(body).optJSONObject(key) ?: return null
        val authors = item.optJSONArray("authors")?.let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }
        }.orEmpty()
        val publishers = item.optJSONArray("publishers")
        val subjects = item.optJSONArray("subjects")?.let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }.take(12)
        }.orEmpty()
        val publishDate = item.optString("publish_date")
        val year = Regex("(\\d{4})").find(publishDate)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val covers = item.optJSONObject("cover")
        return BookMetadata(
            isbn13 = isbn13,
            title = item.optString("title").takeIf(String::isNotBlank) ?: return null,
            subtitle = item.optString("subtitle").takeIf(String::isNotBlank),
            authors = authors,
            publisher = publishers?.optJSONObject(0)?.optString("name")?.takeIf(String::isNotBlank),
            publicationYear = year,
            pageCount = item.optInt("number_of_pages", 0).takeIf { it > 0 },
            subjects = subjects,
            coverUrl = covers?.optString("medium")?.takeIf(String::isNotBlank),
            source = "OPEN_LIBRARY"
        )
    }
}
