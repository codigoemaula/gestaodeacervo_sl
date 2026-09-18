package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.CancellationException

/** Missing means all configured services answered and none knew the exact ISBN. */
sealed interface MetadataLookupResult {
    data class Found(val book: BookMetadata) : MetadataLookupResult
    data class Missing(val searchedSources: List<String>) : MetadataLookupResult
    data class Unavailable(val failedSources: List<String>, val searchedSources: List<String>) : MetadataLookupResult
}

/** No provider failure can stop attempts against the remaining configured sources. */
class MetadataLookupEngine(private val sources: List<Pair<String, BibliographicService>>) {
    suspend fun lookup(isbn13: String): MetadataLookupResult {
        require(sources.isNotEmpty()) { "Informe ao menos uma fonte bibliográfica" }
        val failed = mutableListOf<String>()
        val searched = mutableListOf<String>()
        for ((name, source) in sources) {
            try {
                val result = source.lookup(isbn13)
                searched += name
                if (result != null) return MetadataLookupResult.Found(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += name
            }
        }
        return if (failed.isNotEmpty()) MetadataLookupResult.Unavailable(failed, searched)
        else MetadataLookupResult.Missing(searched)
    }
}
