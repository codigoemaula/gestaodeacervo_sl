package br.gov.sp.sme.salaleitura.feature.catalog

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult

/** A route argument is retained by Navigation even when the scanner screen is popped. */
object BookScanRoute {
    fun forIsbn(value: String): String {
        val normalized = Isbn.normalize(value)
        require(normalized is IsbnResult.Valid) { "ISBN inválido" }
        return "book/new?isbn=${normalized.isbn13}"
    }
}
