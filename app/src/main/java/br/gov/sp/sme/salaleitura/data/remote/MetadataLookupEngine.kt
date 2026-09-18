package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.CancellationException
import java.util.Locale

/** Missing means every available provider responded without an exact ISBN match. */
sealed interface MetadataLookupResult {
    data class Found(val book: BookMetadata) : MetadataLookupResult
    data class Missing(val searchedSources: List<String>) : MetadataLookupResult
    data class Unavailable(val failedSources: List<String>, val searchedSources: List<String>) : MetadataLookupResult
}

/** Every provider is attempted; results for different ISBNs or conflicting titles are never merged. */
class MetadataLookupEngine(private val sources: List<Pair<String, BibliographicService>>) {
    suspend fun lookup(isbn13: String): MetadataLookupResult {
        require(sources.isNotEmpty()) { "Informe ao menos uma fonte bibliográfica" }
        val failed = mutableListOf<String>()
        val searched = mutableListOf<String>()
        var accumulated: BookMetadata? = null
        for ((name, service) in sources) {
            try {
                val result = service.lookup(isbn13)
                searched += name
                if (result == null) continue
                if (result.isbn13 != isbn13 || result.title.isBlank()) {
                    failed += "$name (identificação inconsistente)"
                    continue
                }
                accumulated = if (accumulated == null) result else enrich(accumulated, result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += name
            }
        }
        accumulated?.let { return MetadataLookupResult.Found(it) }
        return if (failed.isNotEmpty()) MetadataLookupResult.Unavailable(failed, searched)
        else MetadataLookupResult.Missing(searched)
    }

    private fun enrich(first: BookMetadata, next: BookMetadata): BookMetadata {
        // A work-level search result can mix editions. Only fill gaps when the titles agree.
        fun titleKey(s: String) = s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        if (titleKey(first.title) != titleKey(next.title)) return first
        val merged = first.copy(
            subtitle = first.subtitle.takeUnless { it.isNullOrBlank() } ?: next.subtitle,
            authors = first.authors.takeIf { it.isNotEmpty() } ?: next.authors,
            publisher = first.publisher.takeUnless { it.isNullOrBlank() } ?: next.publisher,
            publicationYear = first.publicationYear ?: next.publicationYear,
            language = first.language.takeUnless { it.isNullOrBlank() } ?: next.language,
            pageCount = first.pageCount ?: next.pageCount,
            subjects = first.subjects.takeIf { it.isNotEmpty() } ?: next.subjects,
            coverUrl = first.coverUrl.takeUnless { it.isNullOrBlank() } ?: next.coverUrl
        )
        return if (merged == first) first else merged.copy(
            source = if (first.source == next.source) first.source else "${first.source} + ${next.source}"
        )
    }
}
