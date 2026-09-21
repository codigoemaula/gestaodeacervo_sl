package br.gov.sp.sme.salaleitura.data.remote

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import org.json.JSONObject

/** Brazilian ISBN aggregation (CBL, Mercado Editorial and other public providers).
 * No fixed ISBN records, private API keys, scraping or per-book exceptions.
 */
class BrasilApiIsbnService(
    private val fetch: suspend (String) -> String? = { url -> httpGet(url) }
) : BibliographicService {
    override suspend fun lookup(isbn13: String): BookMetadata? {
        val canonical = (Isbn.normalize(isbn13) as? IsbnResult.Valid)?.isbn13 ?: return null
        val body = fetch("https://brasilapi.com.br/api/isbn/v1/$canonical") ?: return null
        return parseResponse(body, canonical)
    }

    internal fun parseResponse(body: String, isbn13: String): BookMetadata? {
        val data = JSONObject(body)
        val returned = (Isbn.normalize(data.optString("isbn")) as? IsbnResult.Valid)?.isbn13
        if (returned != isbn13) return null

        fun text(key: String): String? = data.optString(key).trim()
            .takeIf { it.isNotBlank() && it != "null" }
        val title = text("title") ?: return null
        val authors = data.optJSONArray("authors")?.let { entries ->
            (0 until entries.length()).mapNotNull { i ->
                entries.optString(i).trim().takeIf { it.isNotBlank() && it != "null" }
            }
        }.orEmpty()
        val subjects = data.optJSONArray("subjects")?.let { entries ->
            (0 until minOf(entries.length(), 12)).mapNotNull { i ->
                entries.optString(i).trim().takeIf { it.isNotBlank() && it != "null" }
            }
        }.orEmpty()
        val provider = text("provider")?.uppercase()?.replace('-', '_')
            ?.takeIf { it in setOf("CBL", "MERCADO_EDITORIAL", "OPEN_LIBRARY", "GOOGLE_BOOKS") }
            ?: "AGREGADOR"
        val cover = text("cover_url")?.replace("http://", "https://")
            ?.takeIf { it.startsWith("https://") }
        return BookMetadata(
            isbn13 = isbn13,
            title = title,
            subtitle = text("subtitle"),
            authors = authors,
            publisher = text("publisher"),
            publicationYear = data.optInt("year", 0).takeIf { it in 1450..2200 },
            pageCount = data.optInt("page_count", 0).takeIf { it > 0 },
            subjects = subjects,
            coverUrl = cover,
            source = "BRASIL_API_$provider"
        )
    }
}
