package br.gov.sp.sme.salaleitura.feature.scanner

import br.gov.sp.sme.salaleitura.data.remote.BookMetadata

/** Scanner summaries never present an ISBN as though it were bibliographic metadata. */
object BookScanSummary {
    fun from(book: BookMetadata?): Pair<String, String> = if (book == null) {
        "ISBN identificado" to "Os dados bibliográficos não foram encontrados na consulta. Confira a conexão ou complete o cadastro manualmente."
    } else {
        book.title to listOfNotNull(
            book.authors.joinToString(", ").takeIf(String::isNotBlank),
            book.publisher,
            book.publicationYear?.toString()
        ).joinToString(" · ").ifBlank { "Dados bibliográficos encontrados. Confira e complete o cadastro." }
    }
}
