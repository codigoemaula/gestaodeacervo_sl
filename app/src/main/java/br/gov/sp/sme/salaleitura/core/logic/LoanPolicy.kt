package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.LocalDate

object LoanPolicy {
    fun dueDate(start: LocalDate, period: LoanPeriod): LocalDate = start.plusDays(period.days)

    fun canLoan(personActive: Boolean, copyStatus: CopyStatus): Boolean =
        personActive && copyStatus == CopyStatus.AVAILABLE

    /** Teachers may make a pedagogical exception, but must explicitly confirm it. */
    fun canCheckoutWithOverdue(overdueCount: Int, teacherConfirmedException: Boolean): Boolean =
        overdueCount <= 0 || teacherConfirmedException
}
