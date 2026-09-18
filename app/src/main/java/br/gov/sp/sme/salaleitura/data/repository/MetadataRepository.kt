package br.gov.sp.sme.salaleitura.data.repository

import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.IsbnRangeDataEntity
import br.gov.sp.sme.salaleitura.data.local.entity.MetadataCacheEntity
import br.gov.sp.sme.salaleitura.data.local.entity.SyncStatusEntity
import br.gov.sp.sme.salaleitura.data.remote.*
import org.json.JSONArray
import org.json.JSONObject

class MetadataRepository(
    private val db: AppDatabase,
    private val primary: BibliographicService = OpenLibraryService(),
    private val fallback: BibliographicService = GoogleBooksService(),
    private val ranges: IsbnRangeService = IsbnRangeService()
) {
    private val system = db.systemDao()
    private val resolver = MetadataLookupEngine(listOf(
        "Open Library (catálogo)" to primary,
        "Open Library (edição ISBN)" to OpenLibraryIsbnService(),
        "Google Books" to fallback,
        "Open Library (busca ISBN)" to OpenLibrarySearchService()
    ))

    suspend fun lookup(isbn13: String, now: Long = System.currentTimeMillis()): BookMetadata? =
        when (val result = lookupDetailed(isbn13, now)) {
            is MetadataLookupResult.Found -> result.book
            else -> null
        }

    suspend fun lookupDetailed(isbn13: String, now: Long = System.currentTimeMillis()): MetadataLookupResult {
        system.metadata(isbn13)?.let { cache ->
            runCatching { decode(cache.payloadJson) }.getOrNull()?.let { return MetadataLookupResult.Found(it) }
        }
        val result = resolver.lookup(isbn13)
        if (result is MetadataLookupResult.Found) {
            system.putMetadata(MetadataCacheEntity(isbn13, result.book.source, encode(result.book), now))
        }
        return result
    }

    suspend fun refreshIsbnRanges(now: Long = System.currentTimeMillis()): Boolean {
        system.putSyncStatus(SyncStatusEntity("ISBN_RANGES", lastAttemptAt = now, status = "RUNNING"))
        val result = runCatching { ranges.download() }.getOrNull()
        if (result == null) {
            system.putSyncStatus(SyncStatusEntity("ISBN_RANGES", lastAttemptAt = now, status = "FAILED", message = "Falha de rede ou formato"))
            return false
        }
        system.putIsbnRanges(IsbnRangeDataEntity(version = result.serial, payload = result.xml, updatedAt = now))
        system.putSyncStatus(SyncStatusEntity("ISBN_RANGES", lastAttemptAt = now, lastSuccessAt = now, status = "OK", message = result.messageDate))
        return true
    }

    private fun encode(value: BookMetadata): String = JSONObject().apply {
        put("isbn13", value.isbn13); put("title", value.title); putOpt("subtitle", value.subtitle)
        put("authors", JSONArray(value.authors)); putOpt("publisher", value.publisher); putOpt("publicationYear", value.publicationYear)
        putOpt("language", value.language); putOpt("pageCount", value.pageCount); put("subjects", JSONArray(value.subjects))
        putOpt("coverUrl", value.coverUrl); put("source", value.source)
    }.toString()

    private fun decode(json: String): BookMetadata = JSONObject(json).let { o ->
        BookMetadata(
            isbn13 = o.getString("isbn13"), title = o.getString("title"), subtitle = o.optString("subtitle").takeIf(String::isNotBlank),
            authors = o.optJSONArray("authors")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
            publisher = o.optString("publisher").takeIf(String::isNotBlank), publicationYear = o.optInt("publicationYear", 0).takeIf { it > 0 },
            language = o.optString("language").takeIf(String::isNotBlank), pageCount = o.optInt("pageCount", 0).takeIf { it > 0 },
            subjects = o.optJSONArray("subjects")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
            coverUrl = o.optString("coverUrl").takeIf(String::isNotBlank), source = o.getString("source")
        )
    }
}
