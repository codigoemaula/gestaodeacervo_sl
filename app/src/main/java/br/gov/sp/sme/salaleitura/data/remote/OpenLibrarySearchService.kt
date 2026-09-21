package br.gov.sp.sme.salaleitura.data.remote

import org.json.JSONObject

/** The Search API groups editions into works; only accept an explicit exact ISBN match. */
class OpenLibrarySearchService : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val url = "https://openlibrary.org/search.json?isbn=$isbn13&fields=isbn,title,author_name,cover_i&limit=5"
        val body = httpGet(url) ?: return null
        val docs = JSONObject(body).optJSONArray("docs") ?: return null
        for (index in 0 until docs.length()) {
            val item = docs.optJSONObject(index) ?: continue
            val isbnList = item.optJSONArray("isbn") ?: continue
            if ((0 until isbnList.length()).none { isbnList.optString(it) == isbn13 }) continue
            val title = item.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val authors = item.optJSONArray("author_name")?.let { list ->
                (0 until list.length()).mapNotNull { list.optString(it).takeIf { name -> name.isNotBlank() } }
            }.orEmpty()
            val cover = item.optLong("cover_i", 0).takeIf { it > 0 }
            return BookMetadata(
                isbn13 = isbn13, title = title, authors = authors,
                coverUrl = cover?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
                // Work-level publisher/year can belong to a different edition, so leave them blank.
                source = "OPEN_LIBRARY_SEARCH"
            )
        }
        return null
    }
}
