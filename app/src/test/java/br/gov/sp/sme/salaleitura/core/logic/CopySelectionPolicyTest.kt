package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopySelectionPolicyTest {
    private fun copy(sequence: Int, status: CopyStatus = CopyStatus.AVAILABLE) = BookCopyEntity(
        internalCode = "SL-000001-%03d".format(sequence), editionId = 1, sequence = sequence,
        status = status, registeredAt = 0
    )

    @Test fun `single physical copy may be identified by its ISBN`() {
        val result = CopySelectionPolicy.resolve(listOf(copy(1)))
        assertEquals("SL-000001-001", (result as IsbnCopyDecision.Single).copy.internalCode)
    }

    @Test fun `ISBN with multiple physical copies requires explicit selection even if one is available`() {
        val result = CopySelectionPolicy.resolve(listOf(copy(1, CopyStatus.LOANED), copy(2)))
        assertTrue(result is IsbnCopyDecision.ChooseCopy)
        assertEquals(listOf("SL-000001-002"), (result as IsbnCopyDecision.ChooseCopy).available.map { it.internalCode })
    }

    @Test fun `all unavailable copies never yield a loanable copy`() {
        assertTrue(CopySelectionPolicy.resolve(listOf(copy(1, CopyStatus.LOANED))) is IsbnCopyDecision.NoneAvailable)
    }

    @Test fun `missing ISBN in catalog does not create an imaginary copy`() {
        assertTrue(CopySelectionPolicy.resolve(emptyList()) is IsbnCopyDecision.NotCataloged)
    }
}
