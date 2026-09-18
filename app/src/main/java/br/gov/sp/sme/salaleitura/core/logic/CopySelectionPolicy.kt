package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity

/** ISBN identifies an edition, not an individual physical volume. */
sealed interface IsbnCopyDecision {
    data object NotCataloged : IsbnCopyDecision
    data object NoneAvailable : IsbnCopyDecision
    data class Single(val copy: BookCopyEntity) : IsbnCopyDecision
    data class ChooseCopy(val available: List<BookCopyEntity>) : IsbnCopyDecision
}

object CopySelectionPolicy {
    fun resolve(copies: List<BookCopyEntity>): IsbnCopyDecision {
        if (copies.isEmpty()) return IsbnCopyDecision.NotCataloged
        val available = copies.filter { it.status == CopyStatus.AVAILABLE }.sortedBy { it.sequence }
        if (available.isEmpty()) return IsbnCopyDecision.NoneAvailable
        // Even a single available copy requires selection if the edition has multiple copies.
        return if (copies.size == 1) IsbnCopyDecision.Single(available.single())
        else IsbnCopyDecision.ChooseCopy(available)
    }
}
