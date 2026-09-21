package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

/** Missing means every available provider responded without an exact ISBN match. */
sealed interface MetadataLookupResult {
    data class Found(val book: BookMetadata) : MetadataLookupResult
    data class Missing(val searchedSources: List<String>) : MetadataLookupResult
    data class Unavailable(val failedSources: List<String>, val searchedSources: List<String>) : MetadataLookupResult
}

/** Query independent catalogues together, but reconcile their answers in configured priority order. */
class MetadataLookupEngine(private val sources: List<Pair<String, BibliographicService>>) {
    private sealed interface Attempt {
        data class Response(val name: String, val book: BookMetadata?) : Attempt
        data class Failure(val name: String) : Attempt
    }

    suspend fun lookup(isbn13: String): MetadataLookupResult = coroutineScope {
        require(sources.isNotEmpty()) { "Informe ao menos uma fonte bibliográfica" }
        // A slow or throttled provider must not delay the start of the remaining providers.
        val attempts = sources.map { (name, service) ->
            async {
                try {
                    Attempt.Response(name, service.lookup(isbn13))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Attempt.Failure(name)
                }
            }
        }.awaitAll()
        val failed = mutableListOf<String>()
        val searched = mutableListOf<String>()
        var accumulated: BookMetadata? = null
        for (attempt in attempts) {
            when (attempt) {
                is Attempt.Failure -> failed += attempt.name
                is Attempt.Response -> {
                    searched += attempt.name
                    val result = attempt.book ?: continue
                    if (result.isbn13 != isbn13 || result.title.isBlank()) {
                        failed += "${attempt.name} (identificação inconsistente)"
                        continue
                    }
                    accumulated = if (accumulated == null) result else enrich(accumulated, result)
                }
            }
        }
        accumulated?.let { return@coroutineScope MetadataLookupResult.Found(it) }
        if (failed.isNotEmpty()) MetadataLookupResult.Unavailable(failed, searched)
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
