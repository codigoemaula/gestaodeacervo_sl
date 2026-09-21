package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoanPolicyTest {
    @Test fun sevenDaysProducesCorrectDueDate() {
        assertEquals(LocalDate.of(2026, 9, 24), LoanPolicy.dueDate(LocalDate.of(2026, 9, 17), LoanPeriod.SEVEN))
    }

    @Test fun fourteenDaysProducesCorrectDueDate() {
        assertEquals(LocalDate.of(2026, 10, 1), LoanPolicy.dueDate(LocalDate.of(2026, 9, 17), LoanPeriod.FOURTEEN))
    }

    @Test fun inactivePersonCannotBorrow() {
        assertFalse(LoanPolicy.canLoan(false, CopyStatus.AVAILABLE))
    }

    @Test fun unavailableCopyCannotBeBorrowed() {
        assertFalse(LoanPolicy.canLoan(true, CopyStatus.LOANED))
        assertTrue(LoanPolicy.canLoan(true, CopyStatus.AVAILABLE))
    }
}
