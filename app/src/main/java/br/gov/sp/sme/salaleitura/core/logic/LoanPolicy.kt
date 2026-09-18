package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.LocalDate

object LoanPolicy {
    fun dueDate(start: LocalDate, period: LoanPeriod): LocalDate = start.plusDays(period.days)

    fun canLoan(personActive: Boolean, copyStatus: CopyStatus): Boolean =
        personActive && copyStatus == CopyStatus.AVAILABLE
}
