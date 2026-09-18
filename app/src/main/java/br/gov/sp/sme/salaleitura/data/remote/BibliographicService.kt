package br.gov.sp.sme.salaleitura.data.remote

data class BookMetadata(
    val isbn13: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publicationYear: Int? = null,
    val language: String? = null,
    val pageCount: Int? = null,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val source: String
)

interface BibliographicService {
    suspend fun lookup(isbn13: String): BookMetadata?
}
