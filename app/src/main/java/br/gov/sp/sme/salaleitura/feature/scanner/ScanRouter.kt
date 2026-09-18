package br.gov.sp.sme.salaleitura.feature.scanner

import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult

sealed interface ScanResult {
    data class Isbn(val isbn13: String) : ScanResult
    data class Copy(val code: String) : ScanResult
    data class Person(val code: String) : ScanResult
    data class Unknown(val raw: String) : ScanResult
}

object ScanRouter {
    fun route(raw: String): ScanResult {
        val value = raw.trim()
        if (value.startsWith("SL:", ignoreCase = true)) return ScanResult.Copy(value.substringAfter(':').trim())
        if (value.startsWith("PERSON:", ignoreCase = true)) return ScanResult.Person(value.substringAfter(':').trim())
        if (value.matches(Regex("SL-\\d{6}-\\d{3}"))) return ScanResult.Copy(value)
        if (value.startsWith("P-") && value.length >= 5) return ScanResult.Person(value)
        return when (val isbn = Isbn.normalize(value)) {
            is IsbnResult.Valid -> ScanResult.Isbn(isbn.isbn13)
            is IsbnResult.Invalid -> ScanResult.Unknown(value)
        }
    }
}
