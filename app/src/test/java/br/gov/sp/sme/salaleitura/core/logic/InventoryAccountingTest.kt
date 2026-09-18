package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import org.junit.Assert.*
import org.junit.Test

class InventoryAccountingTest {
    @Test fun duplicateReadsCannotInflateCountAndLoanedAreNotMissing() {
        val copies = listOf(
            InventoryCopy(1, CopyStatus.AVAILABLE),
            InventoryCopy(2, CopyStatus.LOANED),
            InventoryCopy(3, CopyStatus.AVAILABLE)
        )
        val summary = InventorySummaryCalculator.calculate(copies, setOf(1, 1, 1))
        assertEquals(3, summary.registered)
        assertEquals(1, summary.found)
        assertEquals(1, summary.loaned)
        assertEquals(1, summary.missing)
    }
}
