package br.gov.sp.sme.salaleitura.data.remote

import org.json.JSONObject

/** Edition endpoint, rather than a work-level title: do not substitute a different edition's data. */
class OpenLibraryIsbnService : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val body = httpGet("https://openlibrary.org/isbn/$isbn13.json") ?: return null
        val item = JSONObject(body)
        val title = item.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: return null
        val authorNames = item.optJSONArray("authors")?.let { list ->
            (0 until list.length()).mapNotNull { index ->
                list.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
            }
        }.orEmpty()
        val publisher = item.optJSONArray("publishers")?.let { list ->
            (0 until list.length()).mapNotNull { index ->
                val name = list.optString(index).takeIf { it.isNotBlank() && it != "null" }
                    ?: list.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
                name
            }.firstOrNull()
        }
        val subjects = item.optJSONArray("subjects")?.let { list ->
            (0 until minOf(list.length(), 12)).mapNotNull { index ->
                list.optString(index).takeIf { it.isNotBlank() && it != "null" }
            }
        }.orEmpty()
        val year = Regex("\\d{4}").find(item.optString("publish_date"))?.value?.toIntOrNull()
        val coverId = item.optJSONArray("covers")?.optLong(0, 0)?.takeIf { it > 0 }
        return BookMetadata(
            isbn13 = isbn13, title = title,
            subtitle = item.optString("subtitle").takeIf { it.isNotBlank() && it != "null" },
            authors = authorNames, publisher = publisher, publicationYear = year,
            pageCount = item.optInt("number_of_pages", 0).takeIf { it > 0 },
            subjects = subjects,
            coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
            source = "OPEN_LIBRARY_ISBN"
        )
    }
}
