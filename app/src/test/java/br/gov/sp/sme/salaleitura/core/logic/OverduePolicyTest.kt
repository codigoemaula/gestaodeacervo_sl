package br.gov.sp.sme.salaleitura.core.logic

import org.junit.Assert.*
import org.junit.Test

class OverduePolicyTest {
    @Test fun noOverdueAllowsNormalCheckout() {
        assertTrue(LoanPolicy.canCheckoutWithOverdue(0, false))
    }
    @Test fun overdueBlocksUntilTeacherConfirmsException() {
        assertFalse(LoanPolicy.canCheckoutWithOverdue(2, false))
        assertTrue(LoanPolicy.canCheckoutWithOverdue(2, true))
    }
}
