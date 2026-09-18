package br.gov.sp.sme.salaleitura.feature.scanner

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult

sealed interface ScanResult {
    data class Isbn(val isbn13: String) : ScanResult
    data class Copy(val code: String) : ScanResult
    data class Person(val code: String) : ScanResult // Legacy result retained for source compatibility; never emitted.
    data class Unknown(val raw: String) : ScanResult
}

object ScanRouter {
    fun route(raw: String): ScanResult {
        val value = raw.trim()
        // Do not identify people via camera. Old PERSON: labels are intentionally unusable.
        if (value.startsWith("PERSON:", ignoreCase = true) ||
            (value.startsWith("P-", ignoreCase = true) && value.length >= 5)) return ScanResult.Unknown(value)
        if (value.startsWith("SL:", ignoreCase = true)) return ScanResult.Copy(value.substringAfter(':').trim())
        if (value.matches(Regex("SL-\\d{6}-\\d{3}"))) return ScanResult.Copy(value)
        return when (val isbn = Isbn.normalize(value)) {
            is IsbnResult.Valid -> ScanResult.Isbn(isbn.isbn13)
            is IsbnResult.Invalid -> ScanResult.Unknown(value)
        }
    }
}
