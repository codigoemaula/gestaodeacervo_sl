package br.gov.sp.sme.salaleitura.core

import br.gov.sp.sme.salaleitura.core.logic.*
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CoreDomainTest {
    @Test fun loanPeriodsAreExactlySevenOrFourteenDays() {
        val start = LocalDate.of(2026, 9, 17)
        assertEquals(LocalDate.of(2026, 9, 24), LoanPolicy.dueDate(start, LoanPeriod.SEVEN))
        assertEquals(LocalDate.of(2026, 10, 1), LoanPolicy.dueDate(start, LoanPeriod.FOURTEEN))
        assertEquals(LoanPeriod.SEVEN, LoanPeriodSelector.fromDefaultDays(7))
        assertEquals(LoanPeriod.FOURTEEN, LoanPeriodSelector.fromDefaultDays(14))
        assertThrows(IllegalArgumentException::class.java) { LoanPeriodSelector.fromDefaultDays(10) }
    }

    @Test fun inactivePersonOrUnavailableCopyCannotBorrow() {
        assertFalse(LoanPolicy.canLoan(false, CopyStatus.AVAILABLE))
        assertFalse(LoanPolicy.canLoan(true, CopyStatus.LOANED))
        assertTrue(LoanPolicy.canLoan(true, CopyStatus.AVAILABLE))
    }

    @Test fun copyCodesAreStableAndSequential() {
        assertEquals("SL-000123-001", CopyCodeGenerator.generate("SL", 123, 1))
        assertEquals("SL-000123-012", CopyCodeGenerator.generate("SL", 123, 12))
    }

    @Test fun isbnTenAndThirteenNormalizeToSameEdition() {
        val ten = Isbn.normalize("0-306-40615-2") as IsbnResult.Valid
        val thirteen = Isbn.normalize("978-0-306-40615-7") as IsbnResult.Valid
        assertEquals("9780306406157", ten.isbn13)
        assertEquals(ten.isbn13, thirteen.isbn13)
        assertTrue(Isbn.normalize("9780306406158") is IsbnResult.Invalid)
    }
}
