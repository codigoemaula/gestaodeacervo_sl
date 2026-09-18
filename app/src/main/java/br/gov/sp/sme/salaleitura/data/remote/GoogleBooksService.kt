package br.gov.sp.sme.salaleitura.data.remote

import org.json.JSONObject
import java.net.URLEncoder

class GoogleBooksService : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val url = "https://www.googleapis.com/books/v1/volumes?q=${URLEncoder.encode("isbn:$isbn13", "UTF-8")}&maxResults=1"
        val body = httpGet(url) ?: return null
        val root = JSONObject(body)
        val info = root.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("volumeInfo") ?: return null
        val authors = info.optJSONArray("authors")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } }.orEmpty()
        val categories = info.optJSONArray("categories")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } }.orEmpty()
        val year = Regex("(\\d{4})").find(info.optString("publishedDate"))?.groupValues?.getOrNull(1)?.toIntOrNull()
        return BookMetadata(
            isbn13 = isbn13,
            title = info.optString("title").takeIf(String::isNotBlank) ?: return null,
            subtitle = info.optString("subtitle").takeIf(String::isNotBlank),
            authors = authors,
            publisher = info.optString("publisher").takeIf(String::isNotBlank),
            publicationYear = year,
            language = info.optString("language").takeIf(String::isNotBlank),
            pageCount = info.optInt("pageCount", 0).takeIf { it > 0 },
            subjects = categories,
            coverUrl = info.optJSONObject("imageLinks")?.optString("thumbnail")?.replace("http://", "https://")?.takeIf(String::isNotBlank),
            source = "GOOGLE_BOOKS"
        )
    }
}
