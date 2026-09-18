package br.gov.sp.sme.salaleitura.feature.scanner

import br.gov.sp.sme.salaleitura.core.model.CopyStatus

enum class UniversalScanAction { CHECKOUT_COPY, RETURN_COPY, CHECKOUT_PERSON, REGISTER_ISBN, CATALOG, MANUAL_SEARCH }

/** Scanning is discovery only: no circulation state changes before explicit confirmation. */
object UniversalScanPolicy {
    fun copyActions(status: CopyStatus): List<UniversalScanAction> = when (status) {
        CopyStatus.AVAILABLE -> listOf(UniversalScanAction.CHECKOUT_COPY, UniversalScanAction.CATALOG)
        CopyStatus.LOANED -> listOf(UniversalScanAction.RETURN_COPY, UniversalScanAction.CATALOG)
        else -> listOf(UniversalScanAction.CATALOG)
    }
    fun unknownActions(): List<UniversalScanAction> = listOf(UniversalScanAction.MANUAL_SEARCH)
}
